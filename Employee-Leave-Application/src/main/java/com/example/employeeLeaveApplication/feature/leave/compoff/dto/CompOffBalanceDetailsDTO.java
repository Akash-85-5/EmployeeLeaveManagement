package com.example.employeeLeaveApplication.feature.leave.compoff.dto;

import java.math.BigDecimal;

// 🔄 UPDATED FILE
// Added: usedDays field
// Reason: Show earned, used and available together in balance response

public class CompOffBalanceDetailsDTO {

    // ===================== EXISTING =====================
    private Long employeeId;
    private Integer year;
    private BigDecimal totalApprovedDays;
    private BigDecimal availableDays;

    // ✅ NEW FIELD
    // Reason: Frontend needs to show how many days have been used
    private BigDecimal usedDays;

    // ===================== EXISTING (UPDATED) =====================
    // Added usedDays param to constructor
    public CompOffBalanceDetailsDTO(Long employeeId,
                                    Integer year,
                                    BigDecimal totalApprovedDays,
                                    BigDecimal usedDays,        // ✅ NEW PARAM
                                    BigDecimal availableDays) {
        this.employeeId = employeeId;
        this.year = year;
        this.totalApprovedDays = totalApprovedDays;
        this.usedDays = usedDays;                               // ✅ NEW LINE
        this.availableDays = availableDays;
    }

    // ===================== EXISTING =====================
    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getTotalApprovedDays() {
        return totalApprovedDays;
    }

    public void setTotalApprovedDays(BigDecimal totalApprovedDays) {
        this.totalApprovedDays = totalApprovedDays;
    }

    public BigDecimal getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(BigDecimal availableDays) {
        this.availableDays = availableDays;
    }

    // ✅ NEW GETTER/SETTER
    // Reason: Needed to serialize usedDays in response
    public BigDecimal getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(BigDecimal usedDays) {
        this.usedDays = usedDays;
    }
}