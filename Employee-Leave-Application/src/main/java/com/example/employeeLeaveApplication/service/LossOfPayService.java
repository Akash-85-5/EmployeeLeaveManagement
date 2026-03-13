package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.PolicyConstants;
import com.example.employeeLeaveApplication.entity.LossOfPayRecord;
import com.example.employeeLeaveApplication.repository.LossOfPayRecordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LossOfPayService {

    private final LossOfPayRecordRepository lopRepo;

    private static final Logger log = LoggerFactory.getLogger(LossOfPayService.class);

    // ═══════════════════════════════════════════════════════════════
    // APPLY LOP — FIXED: INCREMENT instead of overwrite
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void applyLossOfPay(Long empId, Integer year, Integer month, Double excessDays) {

        validateMonth(month);

        log.info("[LOP] Applying loss of pay: employee={}, year={}, month={}, excessDays={}",
                empId, year, month, excessDays);

        double lossPercentage = excessDays * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY;

        LossOfPayRecord lop = getOrCreate(empId, year, month);

        lop.setExcessDays(lop.getExcessDays() + excessDays);
        lop.setLossPercentage(lop.getLossPercentage() + lossPercentage);
        lop.setViolationCount(lop.getViolationCount() + 1); // ✅ ADDED
        lop.setReason("Excess leave days beyond monthly limit");
        lop.setUpdatedAt(LocalDateTime.now());

        lopRepo.save(lop);

        log.info("[LOP] Applied {}% loss of pay for {} excess days. Total LOP this month: {}%",
                lossPercentage, excessDays, lop.getLossPercentage());
    }

    // ═══════════════════════════════════════════════════════════════
    // RESTORE LOP — called ONLY internally by leave cancellation flow
    // NOT exposed as API endpoint — no one can manually delete LOP
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void restoreLossOfPay(Long empId, Integer year, Integer month, Double excessDays) {

        log.info("[LOP-RESTORE] Restoring LOP: employee={}, year={}, month={}, excessDays={}",
                empId, year, month, excessDays);

        lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month).ifPresent(lop -> {

            double newExcess   = lop.getExcessDays()     - excessDays;
            double newPercent  = lop.getLossPercentage() - (excessDays * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY);

            if (newExcess <= 0.0) {
                // Fully reversed — safe to delete the record entirely
                lopRepo.delete(lop);
                log.info("[LOP-RESTORE] LOP record fully reversed and deleted for month {}", month);
            } else {
                // Partial reversal — keep record, just decrement
                lop.setExcessDays(newExcess);
                lop.setLossPercentage(newPercent);
                lop.setUpdatedAt(LocalDateTime.now());
                lopRepo.save(lop);
                log.info("[LOP-RESTORE] LOP partially restored. Remaining: {}% for month {}", newPercent, month);
            }
        });

        // No 'else' warn needed — if no record exists, nothing to restore
    }

    // ═══════════════════════════════════════════════════════════════
    // MONTHLY LOP — returns formatted percentage for a specific month
    // ═══════════════════════════════════════════════════════════════

    public String getMonthlyLossOfPay(Long empId, Integer year, Integer month) {

        validateMonth(month);

        Optional<LossOfPayRecord> lop =
                lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month);

        double percentage = lop.isPresent()
                ? lop.get().getLossPercentage()
                : 0.0;

        return String.format("%.2f%%", percentage);
    }

    // ═══════════════════════════════════════════════════════════════
    // YEARLY LOP — returns total LOP% across all months in a year
    // ═══════════════════════════════════════════════════════════════

    public String getYearlyLossOfPay(Long empId, Integer year) {

        Double total = lopRepo
                .getTotalLossPercentageByEmployeeIdAndYear(empId, year);

        double result = (total != null) ? total : 0.0;

        return String.format("%.2f%%", result);
    }

    // ═══════════════════════════════════════════════════════════════
    // FULL SUMMARY — month by month breakdown for a year
    // ═══════════════════════════════════════════════════════════════

    public Map<String, Object> getLopSummary(Long empId, Integer year) {

        List<LossOfPayRecord> records =
                lopRepo.findByEmployeeIdAndYear(empId, year);

        // Initialize all 12 months to "0.00%" using Java Month enum
        Map<String, String> monthlyBreakdown = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            String name = Month.of(m)
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            monthlyBreakdown.put(name, "0.00%");
        }

        // Fill actual LOP values with formatted percentage
        for (LossOfPayRecord record : records) {
            String name = Month.of(record.getMonth())
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            monthlyBreakdown.put(name,
                    String.format("%.2f%%", record.getLossPercentage()));
        }

        // Calculate yearly total
        double yearlyTotal = records.stream()
                .mapToDouble(LossOfPayRecord::getLossPercentage)
                .sum();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("employeeId",       empId);
        summary.put("year",             year);
        summary.put("monthlyBreakdown", monthlyBreakdown);
        summary.put("yearlyTotal",      String.format("%.2f%%", yearlyTotal));

        return summary;
    }

    // ═══════════════════════════════════════════════════════════════
    // EXISTING — Get total accumulated LOP% for the year (as Double)
    // Used internally by LeaveBalanceService
    // ═══════════════════════════════════════════════════════════════

    public Double getTotalLossOfPayPercentage(Long empId, Integer year) {
        Double total = lopRepo
                .getTotalLossPercentageByEmployeeIdAndYear(empId, year);
        return total != null ? total : 0.0;
    }

    // ═══════════════════════════════════════════════════════════════
    // EXISTING — Get all LOP records for an employee (all years)
    // ═══════════════════════════════════════════════════════════════

    public List<LossOfPayRecord> getAllForEmployee(Long empId) {
        return lopRepo.findByEmployeeIdOrderByYearDescMonthDesc(empId);
    }

    // ═══════════════════════════════════════════════════════════════
    // EXISTING — Get LOP records for an employee in a specific year
    // ═══════════════════════════════════════════════════════════════

    public List<LossOfPayRecord> getForEmployeeAndYear(Long empId, Integer year) {
        return lopRepo.findByEmployeeIdAndYear(empId, year);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE — Find existing LOP record or create new one
    // ═══════════════════════════════════════════════════════════════

    private LossOfPayRecord getOrCreate(Long empId, Integer year, Integer month) {

        return lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month)
                .orElseGet(() -> {
                    LossOfPayRecord lop = new LossOfPayRecord();
                    lop.setEmployeeId(empId);
                    lop.setYear(year);
                    lop.setMonth(month);
                    lop.setExcessDays(0.0);
                    lop.setLossPercentage(0.0);
                    lop.setViolationCount(0);
                    lop.setCreatedAt(LocalDateTime.now());
                    lop.setUpdatedAt(LocalDateTime.now());
                    return lop;
                });
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE — Validate month is between 1 and 12
    // ═══════════════════════════════════════════════════════════════

    private void validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
    }
}