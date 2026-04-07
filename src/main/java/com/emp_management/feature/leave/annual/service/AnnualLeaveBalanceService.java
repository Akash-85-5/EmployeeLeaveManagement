package com.emp_management.feature.leave.annual.service;

import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.feature.leave.annual.repository.AnnualLeaveMonthlyBalanceRepository;
import com.emp_management.feature.leave.annual.repository.LeaveTypeRepository;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.emp_management.shared.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Manages the cumulative ANNUAL_LEAVE monthly balance.
 *
 * RULES:
 *  - Each month, allocatedDays/12 days are added cumulatively.
 *  - Employee can use up to their cumulative available days.
 *  - Jan of year: available = (allocatedDays/12) + carry_forward_remaining (from previous year)
 *  - Feb: available = Jan_remaining + (allocatedDays/12)
 *  - ... and so on.
 *
 * ✅ REMOVED: processYearEndCarryForward() — moved to CarryForwardBalanceService.
 *    Reason: Year-end carry forward logic belongs in the carry-forward module,
 *            not here. Cap is now DB-driven via LeaveType "CARRY_FORWARD".allocatedDays.
 *
 * ✅ REMOVED: MAX_CARRY_FORWARD = 10.0 constant.
 *    Reason: Was hardcoded. Now admin configures it in the leave_type table.
 *
 * NOTE: getPreviousRemaining() for January still reads CarryForwardBalance
 *       to seed the first month — this is UNCHANGED and still works correctly.
 */
@Service
public class AnnualLeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(AnnualLeaveBalanceService.class);

    // ✅ REMOVED: private static final double MAX_CARRY_FORWARD = 10.0;
    //    Now lives in LeaveType "CARRY_FORWARD".allocatedDays — admin configures it.

    private final AnnualLeaveMonthlyBalanceRepository monthlyBalanceRepo;
    private final CarryForwardBalanceRepository       carryForwardRepo;
    private final EmployeeRepository                  employeeRepository;
    private final LeaveTypeRepository                 leaveTypeRepository;

    public AnnualLeaveBalanceService(AnnualLeaveMonthlyBalanceRepository monthlyBalanceRepo,
                                     CarryForwardBalanceRepository carryForwardRepo,
                                     EmployeeRepository employeeRepository,
                                     LeaveTypeRepository leaveTypeRepository) {
        this.monthlyBalanceRepo  = monthlyBalanceRepo;
        this.carryForwardRepo    = carryForwardRepo;
        this.employeeRepository  = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // ─── resolve from DB ─────────────────────────────────────────

    private LeaveType getAnnualLeaveType() {
        return leaveTypeRepository.findByLeaveType("ANNUAL_LEAVE")
                .orElseThrow(() -> new RuntimeException(
                        "LeaveType 'ANNUAL_LEAVE' not found in DB. Please seed the leave_type table."));
    }

    private double getMonthlyAccrualRate(LeaveType leaveType) {
        return leaveType.getAllocatedDays() / 12.0;
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API  (all unchanged)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public double getAvailableForMonth(String employeeId, int year, int month) {
        ensureMonthsInitialized(employeeId, year, month);
        return getOrThrow(employeeId, year, month).getRemainingDays();
    }

    @Transactional
    public void deductLeave(String employeeId, int year, int month, double days) {
        ensureMonthsInitialized(employeeId, year, month);
        AnnualLeaveMonthlyBalance record = getOrThrow(employeeId, year, month);

        double newUsed      = record.getUsedDays() + days;
        double newRemaining = record.getAvailableDays() - newUsed;

        if (newRemaining < 0) {
            throw new BadRequestException(
                    "Insufficient ANNUAL_LEAVE balance. Available: "
                            + record.getRemainingDays() + ", Requested: " + days);
        }

        record.setUsedDays(newUsed);
        record.setRemainingDays(newRemaining);
        monthlyBalanceRepo.save(record);

        log.info("[ANNUAL_LEAVE] Deducted {} days for employee {} month {}/{}. Remaining: {}",
                days, employeeId, month, year, newRemaining);
    }

    @Transactional
    public void restoreLeave(String employeeId, int year, int month, double days) {
        monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .ifPresent(record -> {
                    double newUsed      = Math.max(record.getUsedDays() - days, 0.0);
                    double newRemaining = record.getAvailableDays() - newUsed;
                    record.setUsedDays(newUsed);
                    record.setRemainingDays(newRemaining);
                    monthlyBalanceRepo.save(record);
                    log.info("[ANNUAL_LEAVE] Restored {} days for employee {} month {}/{}. Remaining: {}",
                            days, employeeId, month, year, newRemaining);
                });
    }

    @Transactional
    public List<AnnualLeaveMonthlyBalance> getYearSummary(String employeeId, int year) {
        int currentMonth = (year == LocalDate.now().getYear())
                ? LocalDate.now().getMonthValue() : 12;
        ensureMonthsInitialized(employeeId, year, currentMonth);
        return monthlyBalanceRepo.findByEmployeeIdAndYearOrderByMonthAsc(employeeId, year);
    }

    // ✅ REMOVED: processYearEndCarryForward() method.
    //    It has been moved to CarryForwardBalanceService.processYearEndCarryForward().
    //    The scheduler (CarryForwardScheduler) now calls it from there.

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL  (all unchanged)
    // ═══════════════════════════════════════════════════════════════

    private void ensureMonthsInitialized(String employeeId, int year, int targetMonth) {
        LeaveType leaveType   = getAnnualLeaveType();
        double    monthlyRate = getMonthlyAccrualRate(leaveType);

        for (int m = 1; m <= targetMonth; m++) {
            if (monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, m).isEmpty()) {
                createMonthRecord(employeeId, year, m, monthlyRate);
            }
        }
    }

    private void createMonthRecord(String employeeId, int year, int month, double monthlyRate) {
        // January: seed from carry-forward balance (unchanged — still reads CarryForwardBalance)
        double previousRemaining = getPreviousRemaining(employeeId, year, month);
        double available = previousRemaining + monthlyRate;

        AnnualLeaveMonthlyBalance record = new AnnualLeaveMonthlyBalance();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setAvailableDays(available);
        record.setUsedDays(0.0);
        record.setRemainingDays(available);
        monthlyBalanceRepo.save(record);

        log.info("[ANNUAL_LEAVE] Created {}/{} for employee {}. Rate: {}/month, Available: {}",
                month, year, employeeId, monthlyRate, available);
    }

    /**
     * For January (month=1): reads carry-forward balance for this year to seed the opening balance.
     * For all other months: reads the previous month's remaining days.
     *
     * This method is UNCHANGED — it still correctly reads CarryForwardBalance for January.
     * CarryForwardBalanceService.processYearEndCarryForward() writes the CarryForwardBalance
     * record before this is ever called for January, so the seeding still works.
     */
    private double getPreviousRemaining(String employeeId, int year, int month) {
        if (month == 1) {
            double cf = carryForwardRepo
                    .findByEmployee_EmpIdAndYear(employeeId, year)
                    .map(CarryForwardBalance::getRemaining)
                    .orElse(0.0);
            log.info("[ANNUAL_LEAVE] Jan init for employee {}: carry-forward = {}", employeeId, cf);
            return cf;
        }
        return monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month - 1)
                .map(AnnualLeaveMonthlyBalance::getRemainingDays)
                .orElse(0.0);
    }

    private AnnualLeaveMonthlyBalance getOrThrow(String employeeId, int year, int month) {
        return monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException(
                        "Annual leave balance not found for employee "
                                + employeeId + " " + year + "/" + month));
    }
}