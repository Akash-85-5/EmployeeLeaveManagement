package com.example.employeeLeaveApplication.service;

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

/**
 * LossOfPayService — MANUAL entry only.
 *
 * LOP is no longer auto-calculated by the system.
 * The CFO reviews attendance data from the attendance application
 * and manually sets the LOP percentage for each employee per month.
 *
 * HR or Manager can view LOP records but cannot modify them.
 * Only CFO (via ADMIN role) can record or update LOP.
 */
@Service
@RequiredArgsConstructor
public class LossOfPayService {

    private final LossOfPayRecordRepository lopRepo;
    private static final Logger log = LoggerFactory.getLogger(LossOfPayService.class);

    // ═══════════════════════════════════════════════════════════════
    // RECORD LOP — CFO manually sets loss percentage for a month
    // Called by: POST /api/lop/record  (ADMIN/CFO only)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Records or updates a manual LOP entry for an employee.
     *
     * @param empId          Employee ID
     * @param year           Year
     * @param month          Month (1–12)
     * @param lossPercentage The LOP percentage decided by CFO (e.g. 3.5 means 3.5%)
     * @param reason         Reason noted by CFO (e.g. "3 days absent without approval")
     */
    @Transactional
    public LossOfPayRecord recordLossOfPay(Long empId, Integer year, Integer month,
                                           Double lossPercentage, String reason) {
        validateMonth(month);

        if (lossPercentage == null || lossPercentage < 0) {
            throw new IllegalArgumentException("Loss percentage must be a non-negative value");
        }

        log.info("[LOP] CFO recording LOP: employee={}, year={}, month={}, lossPercentage={}%",
                empId, year, month, lossPercentage);

        LossOfPayRecord lop = getOrCreate(empId, year, month);
        lop.setLossPercentage(lossPercentage);
        lop.setReason(reason != null ? reason : "Manual entry by CFO");
        lop.setUpdatedAt(LocalDateTime.now());

        LossOfPayRecord saved = lopRepo.save(lop);

        log.info("[LOP] Recorded {}% LOP for employee {} month {}/{}", lossPercentage, empId, month, year);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE LOP — CFO removes an incorrect LOP entry
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void deleteLossOfPay(Long empId, Integer year, Integer month) {
        validateMonth(month);
        lopRepo.deleteByEmployeeIdAndYearAndMonth(empId, year, month);
        log.info("[LOP] Deleted LOP record for employee={}, year={}, month={}", empId, year, month);
    }

    // ═══════════════════════════════════════════════════════════════
    // READ — Monthly LOP for a specific month
    // ═══════════════════════════════════════════════════════════════

    public String getMonthlyLossOfPay(Long empId, Integer year, Integer month) {
        validateMonth(month);
        Optional<LossOfPayRecord> lop =
                lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month);
        double percentage = lop.map(LossOfPayRecord::getLossPercentage).orElse(0.0);
        return String.format("%.2f%%", percentage);
    }

    // ═══════════════════════════════════════════════════════════════
    // READ — Yearly total LOP across all months
    // ═══════════════════════════════════════════════════════════════

    public String getYearlyLossOfPay(Long empId, Integer year) {
        Double total = lopRepo.getTotalLossPercentageByEmployeeIdAndYear(empId, year);
        return String.format("%.2f%%", total != null ? total : 0.0);
    }

    // ═══════════════════════════════════════════════════════════════
    // READ — Full month-by-month summary for a year
    // ═══════════════════════════════════════════════════════════════

    public Map<String, Object> getLopSummary(Long empId, Integer year) {
        List<LossOfPayRecord> records = lopRepo.findByEmployeeIdAndYear(empId, year);

        Map<String, String> monthlyBreakdown = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyBreakdown.put(
                    Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    "0.00%");
        }

        for (LossOfPayRecord record : records) {
            monthlyBreakdown.put(
                    Month.of(record.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    String.format("%.2f%%", record.getLossPercentage()));
        }

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
    // READ — Total LOP% as Double (used by LeaveBalanceService)
    // ═══════════════════════════════════════════════════════════════

    public Double getTotalLossOfPayPercentage(Long empId, Integer year) {
        Double total = lopRepo.getTotalLossPercentageByEmployeeIdAndYear(empId, year);
        return total != null ? total : 0.0;
    }

    // ═══════════════════════════════════════════════════════════════
    // READ — All LOP records for an employee (all years)
    // ═══════════════════════════════════════════════════════════════

    public List<LossOfPayRecord> getAllForEmployee(Long empId) {
        return lopRepo.findByEmployeeIdOrderByYearDescMonthDesc(empId);
    }

    // ═══════════════════════════════════════════════════════════════
    // READ — LOP records for a specific year
    // ═══════════════════════════════════════════════════════════════

    public List<LossOfPayRecord> getForEmployeeAndYear(Long empId, Integer year) {
        return lopRepo.findByEmployeeIdAndYear(empId, year);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private LossOfPayRecord getOrCreate(Long empId, Integer year, Integer month) {
        return lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month)
                .orElseGet(() -> {
                    LossOfPayRecord lop = new LossOfPayRecord();
                    lop.setEmployeeId(empId);
                    lop.setYear(year);
                    lop.setMonth(month);
                    lop.setLossPercentage(0.0);
                    lop.setCreatedAt(LocalDateTime.now());
                    lop.setUpdatedAt(LocalDateTime.now());
                    return lop;
                });
    }

    private void validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
    }
}