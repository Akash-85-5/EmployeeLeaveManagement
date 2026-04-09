package com.emp_management.feature.leave.annual.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.dto.AnnualLeaveBalanceResponse;
import com.emp_management.feature.leave.annual.mapper.AnnualLeaveBalanceMapper;
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
    private final LeaveTypeRepository                 leaveTypeRepository;

    public AnnualLeaveBalanceService(AnnualLeaveMonthlyBalanceRepository monthlyBalanceRepo,
                                     CarryForwardBalanceRepository carryForwardRepo,
                                     EmployeeRepository employeeRepository,
                                     LeaveTypeRepository leaveTypeRepository) {
        this.monthlyBalanceRepo = monthlyBalanceRepo;
        this.carryForwardRepo   = carryForwardRepo;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // ─── resolve from DB ─────────────────────────────────────────

    private LeaveType getAnnualLeaveType() {
        return leaveTypeRepository.findByLeaveType("ANNUAL")
                .orElseThrow(() -> new RuntimeException(
                        "LeaveType 'ANNUAL_LEAVE' not found in DB. Please seed the leave_type table."));
    }

    private double getMonthlyAccrualRate(LeaveType leaveType) {
        return leaveType.getAllocatedDays() / 12.0;
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public double getAvailableForMonth(String employeeId, int year, int month) {
        ensureCurrentMonthInitialized(employeeId, year, month);
        return getOrThrow(employeeId, year, month).getRemainingDays();
    }

    /**
     * Called during employee onboarding (personal details submission)
     * and on dashboard load to ensure the current month's record exists.
     */
    @Transactional
    public void initializeForCurrentMonth(String employeeId, int year, int month) {
        ensureCurrentMonthInitialized(employeeId, year, month);
    }

    /**
     * Deducts leave days from the given month's balance.
     */
    @Transactional
    public void deductLeave(String employeeId, int year, int month, double days) {
        ensureCurrentMonthInitialized(employeeId, year, month);
        AnnualLeaveMonthlyBalance record = getOrThrow(employeeId, year, month);

        double newUsed      = record.getUsedDays() + days;
        double newRemaining = record.getAvailableDays() - newUsed;

        if (newRemaining < 0) {
            throw new BadRequestException(
                    "Insufficient ANNUAL leave balance. Available: "
                            + record.getRemainingDays() + ", Requested: " + days);
        }

        record.setUsedDays(newUsed);
        record.setRemainingDays(newRemaining);
        monthlyBalanceRepo.save(record);

        log.info("[ANNUAL] Deducted {} days for employee {} month {}/{}. Remaining: {}",
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
                    log.info("[ANNUAL] Restored {} days for employee {} month {}/{}. Remaining: {}",
                            days, employeeId, month, year, newRemaining);
                });
    }

    @Transactional
    public List<AnnualLeaveMonthlyBalance> getYearSummary(String employeeId, int year) {
        int currentMonth = (year == LocalDate.now().getYear())
                ? LocalDate.now().getMonthValue() : 12;
        ensureCurrentMonthInitialized(employeeId, year, currentMonth);
        return monthlyBalanceRepo.findByEmployeeIdAndYearOrderByMonthAsc(employeeId, year);
    }

    // ✅ REMOVED: processYearEndCarryForward() method.
    //    It has been moved to CarryForwardBalanceService.processYearEndCarryForward().
    //    The scheduler (CarryForwardScheduler) now calls it from there.

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Creates the monthly record ONLY for the given month if it doesn't exist.
     * Does NOT loop through prior months — new employees joining mid-year
     * correctly get only the current month's single accrual, not a backfill.
     *
     * Cumulative carry-forward still works naturally:
     *  - When the next month's record is created, it reads the previous
     *    month's remainingDays (if that record exists) and adds the accrual.
     *  - If the previous month record doesn't exist (new employee), it defaults to 0.
     */
    private void ensureCurrentMonthInitialized(String employeeId, int year, int month) {
        if (monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .isPresent()) {
            return; // already initialized — nothing to do
        }

        LeaveType leaveType   = getAnnualLeaveType();
        double    monthlyRate = getMonthlyAccrualRate(leaveType);
        createMonthRecord(employeeId, year, month, monthlyRate);
    }

    private void createMonthRecord(String employeeId, int year, int month, double monthlyRate) {
        double previousRemaining = getPreviousRemaining(employeeId, year, month);
        double available         = previousRemaining + monthlyRate;

        AnnualLeaveMonthlyBalance record = new AnnualLeaveMonthlyBalance();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setAvailableDays(available);
        record.setUsedDays(0.0);
        record.setRemainingDays(available);
        monthlyBalanceRepo.save(record);

        log.info("[ANNUAL] Created {}/{} for employee {}. Rate: {}/month, PrevRemaining: {}, Available: {}",
                month, year, employeeId, monthlyRate, previousRemaining, available);
    }

    private double getPreviousRemaining(String employeeId, int year, int month) {
        if (month == 1) {
            // Jan: seed from carry-forward balance of previous year
            double cf = carryForwardRepo
                    .findByEmployee_EmpIdAndYear(employeeId, year)
                    .map(CarryForwardBalance::getRemaining)
                    .orElse(0.0);
            log.info("[ANNUAL] Jan init for employee {}: carry-forward = {}", employeeId, cf);
            return cf;
        }
        return monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month - 1)
                .map(AnnualLeaveMonthlyBalance::getRemainingDays)
                .orElse(0.0);
    }

    private AnnualLeaveMonthlyBalance getOrThrow(String employeeId, int year, int month) {
        return monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new BadRequestException(
                        "Annual leave balance not found for employee "
                                + employeeId + " " + year + "/" + month));
    }
    // ================= DTO METHODS (NEW - SAFE ADDITION) =================

    @Transactional
    public List<AnnualLeaveBalanceResponse> getYearSummaryDTO(String employeeId, int year) {
        return getYearSummary(employeeId, year)
                .stream()
                .map(AnnualLeaveBalanceMapper::toDTO)
                .toList();
    }

    @Transactional
    public AnnualLeaveBalanceResponse getSingleMonthDTO(String employeeId, int year, int month) {
        ensureCurrentMonthInitialized(employeeId, year, month);

        return monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .map(AnnualLeaveBalanceMapper::toDTO)
                .orElseThrow(() -> new BadRequestException(
                        "Annual leave balance not found for employee "
                                + employeeId + " " + year + "/" + month));
    }
}