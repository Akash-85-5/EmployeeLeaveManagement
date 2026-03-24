package com.example.employeeLeaveApplication.feature.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public class EmployeeDashboardResponse {

    private Long employeeId;
    private String employeeName;
    private Integer currentYear;

    // Yearly stats
    private Double yearlyAllocated;
    private Double yearlyUsed;
    private Double yearlyBalance;

    // Monthly stats
    private Double monthlyAllocated;
    private Double monthlyUsed;
    private Double monthlyBalance;

    // Carry forward
    private Double carryForwardTotal;
    private Double carryForwardUsed;
    private Double carryForwardRemaining;

    // Comp-off
    private Double compoffBalance;

    // LOP
    private Double lossOfPayPercentage;

    // Last updated
    private LocalDateTime lastUpdated;

    // Leave status counts
    private Integer approvedCount = 0;
    private Integer rejectedCount = 0;
    private Integer pendingCount = 0;

    // Breakdown by leave type
    private List<LeaveTypeBreakdown> breakdown;

    public EmployeeDashboardResponse() {
        this.lastUpdated = LocalDateTime.now();
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════════

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

    public Integer getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(Integer currentYear) {
        this.currentYear = currentYear;
    }

    public Double getYearlyAllocated() {
        return yearlyAllocated;
    }

    public void setYearlyAllocated(Double yearlyAllocated) {
        this.yearlyAllocated = yearlyAllocated;
    }

    public Double getYearlyUsed() {
        return yearlyUsed;
    }

    public void setYearlyUsed(Double yearlyUsed) {
        this.yearlyUsed = yearlyUsed;
    }

    public Double getYearlyBalance() {
        return yearlyBalance;
    }

    public void setYearlyBalance(Double yearlyBalance) {
        this.yearlyBalance = yearlyBalance;
    }

    public Double getMonthlyAllocated() {
        return monthlyAllocated;
    }

    public void setMonthlyAllocated(Double monthlyAllocated) {
        this.monthlyAllocated = monthlyAllocated;
    }

    public Double getMonthlyUsed() {
        return monthlyUsed;
    }

    public void setMonthlyUsed(Double monthlyUsed) {
        this.monthlyUsed = monthlyUsed;
    }

    public Double getMonthlyBalance() {
        return monthlyBalance;
    }

    public void setMonthlyBalance(Double monthlyBalance) {
        this.monthlyBalance = monthlyBalance;
    }

    public Double getCarryForwardTotal() {
        return carryForwardTotal;
    }

    public void setCarryForwardTotal(Double carryForwardTotal) {
        this.carryForwardTotal = carryForwardTotal;
    }

    public Double getCarryForwardUsed() {
        return carryForwardUsed;
    }

    public void setCarryForwardUsed(Double carryForwardUsed) {
        this.carryForwardUsed = carryForwardUsed;
    }

    public Double getCarryForwardRemaining() {
        return carryForwardRemaining;
    }

    public void setCarryForwardRemaining(Double carryForwardRemaining) {
        this.carryForwardRemaining = carryForwardRemaining;
    }

    public Double getCompoffBalance() {
        return compoffBalance;
    }

    public void setCompoffBalance(Double compoffBalance) {
        this.compoffBalance = compoffBalance;
    }

    public Double getLossOfPayPercentage() {
        return lossOfPayPercentage;
    }

    public void setLossOfPayPercentage(Double lossOfPayPercentage) {
        this.lossOfPayPercentage = lossOfPayPercentage;
    }

    public Integer getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Integer approvedCount) {
        this.approvedCount = approvedCount;
    }

    public Integer getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(Integer rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<LeaveTypeBreakdown> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(List<LeaveTypeBreakdown> breakdown) {
        this.breakdown = breakdown;
    }
}