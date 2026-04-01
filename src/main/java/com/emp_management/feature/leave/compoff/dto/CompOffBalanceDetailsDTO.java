package com.emp_management.feature.leave.compoff.dto;

import jakarta.persistence.Column;

import java.math.BigDecimal;

// 🔄 UPDATED FILE
// Added: usedDays field
// Reason: Show earned, used and available together in balance response

public class CompOffBalanceDetailsDTO {

    // ===================== EXISTING =====================
    private String employeeId;
    @Column(name = "balance_year")
    private Integer year;
    private BigDecimal totalApprovedDays;
    private BigDecimal availableDays;

    // ✅ NEW FIELD
    // Reason: Frontend needs to show how many days have been used
    private BigDecimal usedDays;

    // ===================== EXISTING (UPDATED) =====================
    // Added usedDays param to constructor
    public CompOffBalanceDetailsDTO(String employeeId,
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
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
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