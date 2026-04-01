package com.emp_management.feature.leave.annual.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.feature.leave.annual.repository.AnnualLeaveMonthlyBalanceRepository;
import com.emp_management.feature.leave.annual.repository.LeaveTypeRepository;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
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
 *  - Each month, 2 days (ANNUAL_LEAVE_PER_MONTH) are added cumulatively.
 *  - Employee can use up to their cumulative available days.
 *  - Jan of year: available = 2 + carry_forward_remaining (from previous year)
 *  - Feb: available = Jan_remaining + 2
 *  - Mar: available = Feb_remaining + 2  ... and so on.
 *  - Dec year-end: unused remaining → carry forward (max 10).
 *
 * This service ensures records are lazily initialized up to the current month
 * whenever a balance check or deduction is needed.
 */
@Service
public class AnnualLeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(AnnualLeaveBalanceService.class);

    private final AnnualLeaveMonthlyBalanceRepository monthlyBalanceRepo;
    private final CarryForwardBalanceRepository       carryForwardRepo;
    private final EmployeeRepository                  employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

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

    // MAX_CARRY_FORWARD: keep as a DB-driven value if you add a carryForwardLimit
    // column to LeaveType later. For now, a safe constant here is fine.
    private static final double MAX_CARRY_FORWARD = 10.0;

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
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

    @Transactional
    public void processYearEndCarryForward(String employeeId, int fromYear) {
        ensureMonthsInitialized(employeeId, fromYear, 12);

        double remaining = monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, fromYear, 12)
                .map(AnnualLeaveMonthlyBalance::getRemainingDays)
                .orElse(0.0);

        double carryForward = Math.min(remaining, MAX_CARRY_FORWARD);
        if (carryForward <= 0) {
            log.info("[CF] No carry forward for employee {} from year {}", employeeId, fromYear);
            return;
        }

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employeeId));

        int toYear = fromYear + 1;
        CarryForwardBalance cf = carryForwardRepo
                .findByEmployee_EmpIdAndYear(employeeId, toYear)
                .orElse(new CarryForwardBalance());

        cf.setEmployee(employee);
        cf.setYear(toYear);
        cf.setTotalCarriedForward(carryForward);
        cf.setTotalUsed(0.0);
        cf.setRemaining(carryForward);
        carryForwardRepo.save(cf);

        log.info("[CF] Carried forward {} days for employee {} to year {}", carryForward, employeeId, toYear);
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════════════════════════

    private void ensureMonthsInitialized(String employeeId, int year, int targetMonth) {
        // Resolve rate ONCE — avoids repeated DB hits in the loop
        LeaveType leaveType  = getAnnualLeaveType();
        double    monthlyRate = getMonthlyAccrualRate(leaveType);

        for (int m = 1; m <= targetMonth; m++) {
            if (monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, m).isEmpty()) {
                createMonthRecord(employeeId, year, m, monthlyRate);
            }
        }
    }

    private void createMonthRecord(String employeeId, int year, int month, double monthlyRate) {
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

    private double getPreviousRemaining(String employeeId, int year, int month) {
        if (month == 1) {
            // Jan: seed from carry-forward balance of previous year
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