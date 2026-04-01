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
public class SickLeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(SickLeaveBalanceService.class);

    private final SickLeaveMonthlyBalanceRepository monthlyBalanceRepo;
    private final LeaveTypeRepository               leaveTypeRepository;

    public SickLeaveBalanceService(SickLeaveMonthlyBalanceRepository monthlyBalanceRepo,
                                   LeaveTypeRepository leaveTypeRepository) {
        this.monthlyBalanceRepo  = monthlyBalanceRepo;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // ─── resolve monthly accrual rate from DB ────────────────────
    private double getMonthlyAccrualRate() {
        LeaveType leaveType = leaveTypeRepository.findByLeaveType("SICK")
                .orElseThrow(() -> new RuntimeException(
                        "LeaveType 'SICK' not found in DB. Please seed the leave_type table."));

        if (!Boolean.TRUE.equals(leaveType.isAutoAllocate())) {
            throw new RuntimeException("SICK leave is not configured for auto-allocation.");
        }

        // allocatedDays is yearly total → divide by 12 for monthly rate
        return leaveType.getAllocatedDays() / 12.0;
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API (signatures unchanged — callers need no change)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public double getAvailableForMonth(String employeeId, int year, int month) {
        ensureMonthsInitialized(employeeId, year, month);
        return getOrThrow(employeeId, year, month).getRemainingDays();
    }

    @Transactional
    public void deductLeave(String employeeId, int year, int month, double days) {
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

    @Transactional
    public List<SickLeaveMonthlyBalance> getYearSummary(String employeeId, int year) {
        int currentMonth = (year == LocalDate.now().getYear())
                ? LocalDate.now().getMonthValue() : 12;
        ensureMonthsInitialized(employeeId, year, currentMonth);
        return monthlyBalanceRepo.findByEmployeeIdAndYearOrderByMonthAsc(employeeId, year);
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════════════════════════

    private void ensureMonthsInitialized(String employeeId, int year, int targetMonth) {
        // Resolve rate ONCE per call — avoids repeated DB hits in the loop
        double monthlyRate = getMonthlyAccrualRate();
        for (int m = 1; m <= targetMonth; m++) {
            if (monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, m).isEmpty()) {
                createMonthRecord(employeeId, year, m, monthlyRate);
            }
        }
    }

    private void createMonthRecord(String employeeId, int year, int month, double monthlyRate) {
        // SICK does NOT carry forward across years — Jan always starts at 0
        double previousRemaining = (month == 1) ? 0.0 : getPreviousRemaining(employeeId, year, month);
        double available = previousRemaining + monthlyRate;

        SickLeaveMonthlyBalance record = new SickLeaveMonthlyBalance();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setAvailableDays(available);
        record.setUsedDays(0.0);
        record.setRemainingDays(available);
        monthlyBalanceRepo.save(record);

        log.info("[SICK] Created {}/{} for employee {}. Rate: {}/month, Available: {}",
                month, year, employeeId, monthlyRate, available);
    }

    private double getPreviousRemaining(String employeeId, int year, int month) {
        return monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month - 1)
                .map(SickLeaveMonthlyBalance::getRemainingDays)
                .orElse(0.0);
    }

    private SickLeaveMonthlyBalance getOrThrow(String employeeId, int year, int month) {
        return monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException(
                        "Sick leave balance not found for employee "
                                + employeeId + " " + year + "/" + month));
    }
}