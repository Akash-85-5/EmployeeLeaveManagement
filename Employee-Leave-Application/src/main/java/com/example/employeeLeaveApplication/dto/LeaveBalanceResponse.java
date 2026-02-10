package com.example.employeeLeaveApplication.dto;

import java.util.List;

public class LeaveBalanceResponse {

    private Long employeeId;
    private String employeeName;
    private Integer year;

    // Total leave statistics
    private Double totalAllocated;
    private Double totalUsed;
    private Double totalRemaining;

    private Double compOffBalance;
    private Double compOffEarned;
    private Double compOffUsed;

    // Loss of Pay (only from monthly violations)
    private Double lopPercentage;

    // Carry Forward
    private Double carriedFromLastYear;
    private Double eligibleToCarry;

    // Monthly Stats
    private Integer currentMonthApproved;
    private Boolean exceededMonthlyLimit;

    // Working Days
    private Integer totalWorkingDays;

    // Breakdown per leave type
    private List<LeaveTypeBreakdown> breakdown;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(Double totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public Double getTotalUsed() {
        return totalUsed;
    }

    public void setTotalUsed(Double totalUsed) {
        this.totalUsed = totalUsed;
    }

    public Double getTotalRemaining() {
        return totalRemaining;
    }

    public void setTotalRemaining(Double totalRemaining) {
        this.totalRemaining = totalRemaining;
    }

    public Double getCompOffBalance() {
        return compOffBalance;
    }

    public void setCompOffBalance(Double compOffBalance) {
        this.compOffBalance = compOffBalance;
    }

    public Double getCompOffEarned() {
        return compOffEarned;
    }

    public void setCompOffEarned(Double compOffEarned) {
        this.compOffEarned = compOffEarned;
    }

    public Double getCompOffUsed() {
        return compOffUsed;
    }

    public void setCompOffUsed(Double compOffUsed) {
        this.compOffUsed = compOffUsed;
    }

    public Double getLopPercentage() {
        return lopPercentage;
    }

    public void setLopPercentage(Double lopPercentage) {
        this.lopPercentage = lopPercentage;
    }

    public Double getCarriedFromLastYear() {
        return carriedFromLastYear;
    }

    public void setCarriedFromLastYear(Double carriedFromLastYear) {
        this.carriedFromLastYear = carriedFromLastYear;
    }

    public Double getEligibleToCarry() {
        return eligibleToCarry;
    }

    public void setEligibleToCarry(Double eligibleToCarry) {
        this.eligibleToCarry = eligibleToCarry;
    }

    public Integer getCurrentMonthApproved() {
        return currentMonthApproved;
    }

    public void setCurrentMonthApproved(Integer currentMonthApproved) {
        this.currentMonthApproved = currentMonthApproved;
    }

    public Boolean getExceededMonthlyLimit() {
        return exceededMonthlyLimit;
    }

    public void setExceededMonthlyLimit(Boolean exceededMonthlyLimit) {
        this.exceededMonthlyLimit = exceededMonthlyLimit;
    }

    public Integer getTotalWorkingDays() {
        return totalWorkingDays;
    }

    public void setTotalWorkingDays(Integer totalWorkingDays) {
        this.totalWorkingDays = totalWorkingDays;
    }

    public List<LeaveTypeBreakdown> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(List<LeaveTypeBreakdown> breakdown) {
        this.breakdown = breakdown;
    }
}
