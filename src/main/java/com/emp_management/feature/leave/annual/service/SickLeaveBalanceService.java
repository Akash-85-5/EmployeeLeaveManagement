package com.emp_management.feature.leave.annual.service;

import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.feature.leave.annual.entity.SickLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.repository.LeaveTypeRepository;
import com.emp_management.feature.leave.annual.repository.SickLeaveMonthlyBalanceRepository;
import com.emp_management.shared.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Manages cumulative SICK leave monthly balance.
 *
 * RULES:
 *  - Accrues (allocatedDays / 12) days per month.
 *  - Only the CURRENT month's record is lazily created on first access.
 *  - Prior months are NOT backfilled — new employees joining mid-year
 *    start with a single month's accrual, not all months up to today.
 *  - Jan of each year starts fresh at 0 + monthlyRate (NO carry-forward across years).
 *  - Feb onwards: available = previous month's remaining + monthlyRate.
 *  - Year-end: RESET. Sick balance does NOT carry forward to next year.
 */
@Service
public class SickLeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(SickLeaveBalanceService.class);

    private final SickLeaveMonthlyBalanceRepository monthlyBalanceRepo;
    private final LeaveTypeRepository               leaveTypeRepository;

    public SickLeaveBalanceService(SickLeaveMonthlyBalanceRepository monthlyBalanceRepo,
                                   LeaveTypeRepository leaveTypeRepository) {
        this.monthlyBalanceRepo = monthlyBalanceRepo;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the remaining (available) days for the given month.
     * Initializes the current month's record if it doesn't exist yet.
     */
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
        SickLeaveMonthlyBalance record = getOrThrow(employeeId, year, month);

        double newUsed      = record.getUsedDays() + days;
        double newRemaining = record.getAvailableDays() - newUsed;

        if (newRemaining < 0) {
            throw new BadRequestException(
                    "Insufficient SICK leave balance. Available: "
                            + record.getRemainingDays() + ", Requested: " + days);
        }

        record.setUsedDays(newUsed);
        record.setRemainingDays(newRemaining);
        monthlyBalanceRepo.save(record);

        log.info("[SICK] Deducted {} days for employee {} month {}/{}. Remaining: {}",
                days, employeeId, month, year, newRemaining);
    }

    /**
     * Restores leave days back to the given month's balance (on cancellation/rejection).
     */
    @Transactional
    public void restoreLeave(String employeeId, int year, int month, double days) {
        monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .ifPresent(record -> {
                    double newUsed      = Math.max(record.getUsedDays() - days, 0.0);
                    double newRemaining = record.getAvailableDays() - newUsed;
                    record.setUsedDays(newUsed);
                    record.setRemainingDays(newRemaining);
                    monthlyBalanceRepo.save(record);
                    log.info("[SICK] Restored {} days for employee {} month {}/{}. Remaining: {}",
                            days, employeeId, month, year, newRemaining);
                });
    }

    /**
     * Returns all monthly balance records for the given year.
     * Only initializes the current month — does not backfill past months.
     */
    @Transactional
    public List<SickLeaveMonthlyBalance> getYearSummary(String employeeId, int year) {
        int currentMonth = (year == LocalDate.now().getYear())
                ? LocalDate.now().getMonthValue() : 12;
        ensureCurrentMonthInitialized(employeeId, year, currentMonth);
        return monthlyBalanceRepo.findByEmployeeIdAndYearOrderByMonthAsc(employeeId, year);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
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

        double monthlyRate = getMonthlyAccrualRate();
        createMonthRecord(employeeId, year, month, monthlyRate);
    }

    private void createMonthRecord(String employeeId, int year, int month, double monthlyRate) {
        double previousRemaining = getPreviousRemaining(employeeId, year, month);
        double available         = previousRemaining + monthlyRate;

        SickLeaveMonthlyBalance record = new SickLeaveMonthlyBalance();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setAvailableDays(available);
        record.setUsedDays(0.0);
        record.setRemainingDays(available);
        monthlyBalanceRepo.save(record);

        log.info("[SICK] Created {}/{} for employee {}. Rate: {}/month, PrevRemaining: {}, Available: {}",
                month, year, employeeId, monthlyRate, previousRemaining, available);
    }

    /**
     * For January: always returns 0.0 — SICK leave does NOT carry forward across years.
     * For other months: reads the previous month's remaining days.
     * If the previous month record doesn't exist (new employee joining mid-year),
     * returns 0.0 — so they only get the current month's accrual, not a backfill.
     */
    private double getPreviousRemaining(String employeeId, int year, int month) {
        if (month == 1) {
            return 0.0; // SICK leave resets every year — no cross-year carry-forward
        }
        return monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month - 1)
                .map(SickLeaveMonthlyBalance::getRemainingDays)
                .orElse(0.0); // 0.0 = new employee joining mid-year, no prior record
    }

    private double getMonthlyAccrualRate() {
        LeaveType leaveType = leaveTypeRepository.findByLeaveType("SICK")
                .orElseThrow(() -> new BadRequestException(
                        "LeaveType 'SICK' not found in DB. Please seed the leave_type table."));

        if (!Boolean.TRUE.equals(leaveType.isAutoAllocate())) {
            throw new BadRequestException(
                    "SICK leave is not configured for auto-allocation.");
        }

        return leaveType.getAllocatedDays() / 12.0;
    }

    private SickLeaveMonthlyBalance getOrThrow(String employeeId, int year, int month) {
        return monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new BadRequestException(
                        "Sick leave balance not found for employee "
                                + employeeId + " " + year + "/" + month));
    }
}