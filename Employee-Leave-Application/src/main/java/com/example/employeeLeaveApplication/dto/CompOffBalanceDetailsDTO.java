package com.example.employeeLeaveApplication.dto;

import java.math.BigDecimal;

public class CompOffBalanceDetailsDTO {
    private Long employeeId;
    private Integer year;
    private BigDecimal totalApprovedDays;
    private BigDecimal availableDays;

    public CompOffBalanceDetailsDTO(Long employeeId,
                                    Integer year,
                                    BigDecimal totalApprovedDays,
                                    BigDecimal availableDays) {
        this.employeeId = employeeId;
        this.year = year;
        this.totalApprovedDays = totalApprovedDays;
        this.availableDays = availableDays;
    }

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
}
