package com.example.employeeLeaveApplication.feature.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public class EmployeeDashboardResponse {

    private Long employeeId;
    private String employeeName;
    private Integer currentYear;

    // ── Yearly stats ──────────────────────────────────────────────
    private Double yearlyAllocated;
    private Double yearlyUsed;
    private Double yearlyBalance;

    // ── Monthly ANNUAL_LEAVE cumulative balance ───────────────────
    // monthlyAllocated  = cumulative available ANNUAL_LEAVE this month
    // monthlyUsed       = ANNUAL_LEAVE days used this month
    // monthlyBalance    = monthlyAllocated - monthlyUsed
    private Double monthlyAnnualAllocated;
    private Double monthlyAnnualUsed;
    private Double monthlyAnnualBalance;

    private Double monthlySickAllocated;
    private Double monthlySickUsed;
    private Double monthlySickBalance;

    private Double monthlyTotalBalance;

    // ── Carry forward ─────────────────────────────────────────────
    private Double carryForwardTotal;
    private Double carryForwardUsed;
    private Double carryForwardRemaining;

    // ── Comp-off ──────────────────────────────────────────────────
    private Double compoffBalance;

    // ── LOP ───────────────────────────────────────────────────────
    private Double lossOfPayPercentage;

    // ── Audit ─────────────────────────────────────────────────────
    private LocalDateTime lastUpdated;

    // ── Leave status counts ───────────────────────────────────────
    private Integer approvedCount = 0;
    private Integer rejectedCount = 0;
    private Integer pendingCount  = 0;

    // ── Breakdown by leave type ───────────────────────────────────
    private List<LeaveTypeBreakdown> breakdown;

    public EmployeeDashboardResponse() {
        this.lastUpdated = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Double getMonthlySickAllocated() {
        return monthlySickAllocated;
    }

    public void setMonthlySickAllocated(Double monthlySickAllocated) {
        this.monthlySickAllocated = monthlySickAllocated;
    }

    public Double getMonthlySickUsed() {
        return monthlySickUsed;
    }

    public void setMonthlySickUsed(Double monthlySickUsed) {
        this.monthlySickUsed = monthlySickUsed;
    }

    public Double getMonthlySickBalance() {
        return monthlySickBalance;
    }

    public void setMonthlySickBalance(Double monthlySickBalance) {
        this.monthlySickBalance = monthlySickBalance;
    }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public Integer getCurrentYear() { return currentYear; }
    public void setCurrentYear(Integer currentYear) { this.currentYear = currentYear; }

    public Double getYearlyAllocated() { return yearlyAllocated; }
    public void setYearlyAllocated(Double yearlyAllocated) { this.yearlyAllocated = yearlyAllocated; }

    public Double getYearlyUsed() { return yearlyUsed; }
    public void setYearlyUsed(Double yearlyUsed) { this.yearlyUsed = yearlyUsed; }

    public Double getYearlyBalance() { return yearlyBalance; }
    public void setYearlyBalance(Double yearlyBalance) { this.yearlyBalance = yearlyBalance; }

    public Double getMonthlyAnnualAllocated() {
        return monthlyAnnualAllocated;
    }

    public void setMonthlyAnnualAllocated(Double monthlyAnnualAllocated) {
        this.monthlyAnnualAllocated = monthlyAnnualAllocated;
    }

    public Double getMonthlyAnnualUsed() {
        return monthlyAnnualUsed;
    }

    public void setMonthlyAnnualUsed(Double monthlyAnnualUsed) {
        this.monthlyAnnualUsed = monthlyAnnualUsed;
    }

    public Double getMonthlyAnnualBalance() {
        return monthlyAnnualBalance;
    }

    public void setMonthlyAnnualBalance(Double monthlyAnnualBalance) {
        this.monthlyAnnualBalance = monthlyAnnualBalance;
    }


    public Double getMonthlyTotalBalance() { return monthlyTotalBalance; }
    public void setMonthlyTotalBalance(Double monthlyTotalBalance) {
        this.monthlyTotalBalance = monthlyTotalBalance;
    }

    public Double getCarryForwardTotal() { return carryForwardTotal; }
    public void setCarryForwardTotal(Double carryForwardTotal) { this.carryForwardTotal = carryForwardTotal; }

    public Double getCarryForwardUsed() { return carryForwardUsed; }
    public void setCarryForwardUsed(Double carryForwardUsed) { this.carryForwardUsed = carryForwardUsed; }

    public Double getCarryForwardRemaining() { return carryForwardRemaining; }
    public void setCarryForwardRemaining(Double carryForwardRemaining) {
        this.carryForwardRemaining = carryForwardRemaining;
    }

    public Double getCompoffBalance() { return compoffBalance; }
    public void setCompoffBalance(Double compoffBalance) { this.compoffBalance = compoffBalance; }

    public Double getLossOfPayPercentage() { return lossOfPayPercentage; }
    public void setLossOfPayPercentage(Double lossOfPayPercentage) {
        this.lossOfPayPercentage = lossOfPayPercentage;
    }

    public Integer getApprovedCount() { return approvedCount; }
    public void setApprovedCount(Integer approvedCount) { this.approvedCount = approvedCount; }

    public Integer getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(Integer rejectedCount) { this.rejectedCount = rejectedCount; }

    public Integer getPendingCount() { return pendingCount; }
    public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public List<LeaveTypeBreakdown> getBreakdown() { return breakdown; }
    public void setBreakdown(List<LeaveTypeBreakdown> breakdown) { this.breakdown = breakdown; }
}