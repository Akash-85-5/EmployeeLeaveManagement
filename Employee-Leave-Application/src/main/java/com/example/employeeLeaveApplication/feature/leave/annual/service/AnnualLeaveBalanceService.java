package com.example.employeeLeaveApplication.feature.leave.annual.service;

import com.example.employeeLeaveApplication.shared.constants.PolicyConstants;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.AnnualLeaveMonthlyBalanceRepository;
import com.example.employeeLeaveApplication.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.feature.leave.carryforward.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AnnualLeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(AnnualLeaveBalanceService.class);

    private final AnnualLeaveMonthlyBalanceRepository monthlyBalanceRepo;
    private final CarryForwardBalanceRepository carryForwardRepo;

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the available ANNUAL_LEAVE days for the employee at the given month.
     * Initializes missing month records lazily (rolling forward unused days).
     */
    @Transactional
    public double getAvailableForMonth(Long employeeId, int year, int month) {
        ensureMonthsInitialized(employeeId, year, month);
        AnnualLeaveMonthlyBalance record = getOrThrow(employeeId, year, month);
        return record.getRemainingDays();
    }

    /**
     * Deducts approved leave days from the monthly balance.
     * Called when a leave is APPROVED.
     */
    @Transactional
    public void deductLeave(Long employeeId, int year, int month, double days) {
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

    /**
     * Restores leave days back to the monthly balance (on cancellation).
     */
    @Transactional
    public void restoreLeave(Long employeeId, int year, int month, double days) {
        Optional<AnnualLeaveMonthlyBalance> opt =
                monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month);

        if (opt.isEmpty()) return;

        AnnualLeaveMonthlyBalance record = opt.get();
        double newUsed      = Math.max(record.getUsedDays() - days, 0.0);
        double newRemaining = record.getAvailableDays() - newUsed;

        record.setUsedDays(newUsed);
        record.setRemainingDays(newRemaining);
        monthlyBalanceRepo.save(record);

        log.info("[ANNUAL_LEAVE] Restored {} days for employee {} month {}/{}. Remaining: {}",
                days, employeeId, month, year, newRemaining);
    }

    /**
     * Returns the full year's monthly balance list.
     * Initializes all months up to current month.
     */
    @Transactional
    public List<AnnualLeaveMonthlyBalance> getYearSummary(Long employeeId, int year) {
        int currentMonth = (year == LocalDate.now().getYear())
                ? LocalDate.now().getMonthValue()
                : 12;
        ensureMonthsInitialized(employeeId, year, currentMonth);
        return monthlyBalanceRepo.findByEmployeeIdAndYearOrderByMonthAsc(employeeId, year);
    }

    /**
     * Called at year-end to calculate remaining ANNUAL_LEAVE and store carry-forward.
     * Only ANNUAL_LEAVE is eligible for carry-forward.
     */
    @Transactional
    public void processYearEndCarryForward(Long employeeId, int fromYear) {
        ensureMonthsInitialized(employeeId, fromYear, 12);

        AnnualLeaveMonthlyBalance decRecord =
                monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, fromYear, 12)
                        .orElse(null);

        double remaining = (decRecord != null) ? decRecord.getRemainingDays() : 0.0;
        double carryForward = Math.min(remaining, PolicyConstants.MAX_CARRY_FORWARD);

        if (carryForward <= 0) {
            log.info("[CF] No carry forward for employee {} from year {}", employeeId, fromYear);
            return;
        }

        int toYear = fromYear + 1;
        CarryForwardBalance cf = carryForwardRepo
                .findByEmployeeIdAndYear(employeeId, toYear)
                .orElse(new CarryForwardBalance());

        cf.setEmployeeId(employeeId);
        cf.setYear(toYear);
        cf.setTotalCarriedForward(carryForward);
        cf.setTotalUsed(0.0);
        cf.setRemaining(carryForward);
        carryForwardRepo.save(cf);

        log.info("[CF] Carried forward {} days for employee {} to year {}", carryForward, employeeId, toYear);
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL: Lazy initialization of monthly records
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ensures all month records from 1 up to targetMonth exist for the given year.
     *
     * Month 1 (January):
     *   available = carry_forward_remaining (from last year) + ANNUAL_LEAVE_PER_MONTH
     *
     * Month N (Feb–Dec):
     *   available = previous_month.remainingDays + ANNUAL_LEAVE_PER_MONTH
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
        double available = previousRemaining + PolicyConstants.ANNUAL_LEAVE_PER_MONTH;

        AnnualLeaveMonthlyBalance record = new AnnualLeaveMonthlyBalance();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setAvailableDays(available);
        record.setUsedDays(0.0);
        record.setRemainingDays(available);
        monthlyBalanceRepo.save(record);

        log.info("[ANNUAL_LEAVE] Created month record {}/{} for employee {}. Available: {}",
                month, year, employeeId, available);
    }

    /**
     * For January: previous remaining = carry-forward balance from last year.
     * For Feb–Dec: previous remaining = last month's remainingDays.
     */
    private double getPreviousRemaining(Long employeeId, int year, int month) {
        if (month == 1) {
            // Start of year: use carry-forward from previous year
            CarryForwardBalance cf = carryForwardRepo
                    .findByEmployeeIdAndYear(employeeId, year)
                    .orElse(null);
            double cfRemaining = (cf != null) ? cf.getRemaining() : 0.0;
            log.info("[ANNUAL_LEAVE] Jan init for employee {}: carry-forward = {}", employeeId, cfRemaining);
            return cfRemaining;
        }

        // Previous month in same year
        AnnualLeaveMonthlyBalance prev =
                monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month - 1)
                        .orElse(null);
        return (prev != null) ? prev.getRemainingDays() : 0.0;
    }

    private AnnualLeaveMonthlyBalance getOrThrow(Long employeeId, int year, int month) {
        return monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException(
                        "Monthly balance record not found for employee " + employeeId
                                + " year " + year + " month " + month));
    }
}