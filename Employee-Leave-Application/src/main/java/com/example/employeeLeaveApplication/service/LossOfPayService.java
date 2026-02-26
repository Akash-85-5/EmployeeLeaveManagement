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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LossOfPayService {

    private final LossOfPayRecordRepository lopRepo;

    private static final Logger log = LoggerFactory.getLogger(LossOfPayService.class);


    @Transactional
    public void applyLossOfPay(Long empId, Integer year, Integer month, Double excessDays) {

        validateMonth(month);

        log.info("[LOP] Applying loss of pay: employee={}, year={}, month={}, excessDays={}",
                empId, year, month, excessDays);

        // Calculate loss percentage using centralized constant
        double lossPercentage = excessDays * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY;

        // Get or create LOP record
        LossOfPayRecord lop = getOrCreate(empId, year, month);

        // Update the record
        lop.setExcessDays(excessDays);
        lop.setLossPercentage(lossPercentage);
        lop.setUpdatedAt(LocalDateTime.now());

        lopRepo.save(lop);

        log.info("[LOP] Applied {}% loss of pay for {} excess days", lossPercentage, excessDays);
    }


    /**
     * Get total accumulated loss of pay for the year
     */
    public Double getTotalLossOfPayPercentage(Long empId, Integer year) {
        Double total = lopRepo.getTotalLossPercentageByEmployeeIdAndYear(empId, year);
        return total != null ? total : 0.0;
    }

    /**
     * Get all LOP records for an employee (all years)
     */
    public java.util.List<LossOfPayRecord> getAllForEmployee(Long empId) {
        return lopRepo.findByEmployeeIdOrderByYearDescMonthDesc(empId);
    }

    /**
     * Get LOP records for an employee in a specific year
     */
    public java.util.List<LossOfPayRecord> getForEmployeeAndYear(Long empId, Integer year) {
        return lopRepo.findByEmployeeIdAndYear(empId, year);
    }
    // ═══════════════════════════════════════════════════════════════════
// ADD THIS METHOD to LossOfPayService.java
// ═══════════════════════════════════════════════════════════════════

    /**
     * Restore loss of pay (when leave is cancelled)
     * Deletes the LOP record for that month
     */
    @Transactional
    public void restoreLossOfPay(Long empId, Integer year, Integer month) {

        log.info("[LOP-RESTORE] Restoring LOP: employee={}, year={}, month={}",
                empId, year, month);

        Optional<LossOfPayRecord> lopOpt = lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month);

        if (lopOpt.isPresent()) {
            lopRepo.delete(lopOpt.get());
            log.info("[LOP-RESTORE] Deleted LOP record for month {}", month);
        } else {
            log.warn("[LOP-RESTORE] No LOP record found to restore");
        }
    }

    /**
     * ==========================================================
     * Apply LOP when monthly approved leave limit (>2) exceeded
     * Rule: Apply ONLY ONCE per month (1%)
     * ==========================================================
     */
    @Transactional
    public void applyMonthlyLimitViolation(Long empId, Integer year, Integer month) {

        validateMonth(month);

        LossOfPayRecord lop = getOrCreate(empId, year, month);

        // ✅ increment instead of overwrite
        lop.setViolationCount(lop.getViolationCount() + 1);
        lop.setLossPercentage(lop.getLossPercentage() + 1.0);

        lop.setReason("Monthly limit exceeded (>2 approved leaves)");
        lop.setUpdatedAt(LocalDateTime.now());

        lopRepo.save(lop);
    }


    /**
     * ==========================================================
     * Find existing LOP record or create new one
     * ==========================================================
     */
    private LossOfPayRecord getOrCreate(Long empId, Integer year, Integer month) {

        return lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month)
                .orElseGet(() -> {
                    LossOfPayRecord lop = new LossOfPayRecord();
                    lop.setEmployeeId(empId);
                    lop.setYear(year);
                    lop.setMonth(month);
                    lop.setLossPercentage(0.0);
                    lop.setViolationCount(0);
                    lop.setCreatedAt(LocalDateTime.now());
                    lop.setUpdatedAt(LocalDateTime.now());
                    return lop;
                });
    }

    /**
     * ==========================================================
     * Defensive validation
     * ==========================================================
     */
    private void validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
    }
}
