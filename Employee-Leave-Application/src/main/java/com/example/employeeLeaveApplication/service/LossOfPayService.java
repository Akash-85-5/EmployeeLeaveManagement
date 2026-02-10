package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.LossOfPayRecord;

import com.example.employeeLeaveApplication.repository.LossOfPayRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LossOfPayService {

    private final LossOfPayRecordRepository lopRepo;

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
        lop.setLopPercentage(lop.getLopPercentage() + 1.0);

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
                    lop.setLopPercentage(0.0);
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
