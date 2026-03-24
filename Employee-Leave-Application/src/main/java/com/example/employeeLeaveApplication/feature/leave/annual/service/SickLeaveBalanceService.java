package com.example.employeeLeaveApplication.feature.leave.annual.service;

import com.example.employeeLeaveApplication.shared.constants.PolicyConstants;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.SickLeaveMonthlyBalance;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.SickLeaveMonthlyBalanceRepository;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages cumulative SICK leave monthly balance.
 *
 * Rules:
 *  - Accrues 1.0 day per month (SICK_LEAVE_PER_MONTH).
 *  - Unused sick days roll forward month-to-month within the same year.
 *  - Jan of each year starts fresh: available = 1.0 (NO carry-forward from prev year).
 *  - Feb: available = Jan_remaining + 1.0
 *  - ... rolls up to December.
 *  - Year-end: RESET. Sick balance does NOT carry forward to next year.
 *
 * Max cumulative available at any month = month_number * 1.0
 * (e.g., by March: max 3.0 days if none used).
 */
@Service
@RequiredArgsConstructor
public class SickLeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(SickLeaveBalanceService.class);

    private final SickLeaveMonthlyBalanceRepository monthlyBalanceRepo;

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the cumulative SICK leave days available for the given month.
     * Lazily initializes missing month records.
     */
    @Transactional
    public double getAvailableForMonth(Long employeeId, int year, int month) {
        ensureMonthsInitialized(employeeId, year, month);
        return getOrThrow(employeeId, year, month).getRemainingDays();
    }

    /**
     * Deducts approved SICK leave days from the monthly balance.
     * Called when a SICK leave is APPROVED.
     */
    @Transactional
    public void deductLeave(Long employeeId, int year, int month, double days) {
        ensureMonthsInitialized(employeeId, year, month);
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
     * Restores SICK leave days (on leave cancellation).
     */
    @Transactional
    public void restoreLeave(Long employeeId, int year, int month, double days) {
        Optional<SickLeaveMonthlyBalance> opt =
                monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month);

        if (opt.isEmpty()) return;

        SickLeaveMonthlyBalance record = opt.get();
        double newUsed      = Math.max(record.getUsedDays() - days, 0.0);
        double newRemaining = record.getAvailableDays() - newUsed;

        record.setUsedDays(newUsed);
        record.setRemainingDays(newRemaining);
        monthlyBalanceRepo.save(record);

        log.info("[SICK] Restored {} days for employee {} month {}/{}. Remaining: {}",
                days, employeeId, month, year, newRemaining);
    }

    /**
     * Returns the full year's monthly SICK balance list.
     * Initializes all months up to current month.
     */
    @Transactional
    public List<SickLeaveMonthlyBalance> getYearSummary(Long employeeId, int year) {
        int currentMonth = (year == java.time.LocalDate.now().getYear())
                ? java.time.LocalDate.now().getMonthValue() : 12;
        ensureMonthsInitialized(employeeId, year, currentMonth);
        return monthlyBalanceRepo.findByEmployeeIdAndYearOrderByMonthAsc(employeeId, year);
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL: Lazy initialization of monthly records
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ensures all month records from 1 up to targetMonth exist for the given year.
     *
     * Month 1 (January):
     *   available = SICK_LEAVE_PER_MONTH only (NO carry-forward from previous year)
     *
     * Month N (Feb–Dec):
     *   available = previous_month.remainingDays + SICK_LEAVE_PER_MONTH
     */
    private void ensureMonthsInitialized(Long employeeId, int year, int targetMonth) {
        for (int m = 1; m <= targetMonth; m++) {
            if (monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, m).isEmpty()) {
                createMonthRecord(employeeId, year, m);
            }
        }
    }

    private void createMonthRecord(Long employeeId, int year, int month) {
        double previousRemaining = getPreviousRemaining(employeeId, year, month);
        double available = previousRemaining + PolicyConstants.SICK_LEAVE_PER_MONTH;

        SickLeaveMonthlyBalance record = new SickLeaveMonthlyBalance();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setAvailableDays(available);
        record.setUsedDays(0.0);
        record.setRemainingDays(available);
        monthlyBalanceRepo.save(record);

        log.info("[SICK] Created month record {}/{} for employee {}. Available: {}",
                month, year, employeeId, available);
    }

    /**
     * For January: starts fresh — previous remaining = 0.0 (no carry-forward).
     * For Feb–Dec: previous remaining = last month's remainingDays.
     */
    private double getPreviousRemaining(Long employeeId, int year, int month) {
        if (month == 1) {
            // SICK does NOT carry forward — always start Jan at 0
            log.info("[SICK] Jan init for employee {}: starting fresh (no carry-forward)", employeeId);
            return 0.0;
        }

        SickLeaveMonthlyBalance prev =
                monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month - 1)
                        .orElse(null);
        return (prev != null) ? prev.getRemainingDays() : 0.0;
    }

    private SickLeaveMonthlyBalance getOrThrow(Long employeeId, int year, int month) {
        return monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException(
                        "Sick leave monthly balance record not found for employee "
                                + employeeId + " year " + year + " month " + month));
    }
}