package com.example.employeeLeaveApplication.feature.leave.carryforward.dto;
public class CarryForwardEligibilityResponse {

    private Long employeeId;
    private String employeeName;
    private Integer year;
    private Double yearlyAllocated;
    private Double totalUsed;
    private Double balance;
    private Boolean eligible;
    private Double eligibleAmount;
    private String reason;
    private Double carriedIn;

    public CarryForwardEligibilityResponse() {
    }

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

    public Double getYearlyAllocated() {
        return yearlyAllocated;
    }

    public void setYearlyAllocated(Double yearlyAllocated) {
        this.yearlyAllocated = yearlyAllocated;
    }

    public Double getTotalUsed() {
        return totalUsed;
    }

    public void setTotalUsed(Double totalUsed) {
        this.totalUsed = totalUsed;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Boolean getEligible() {
        return eligible;
    }

    public void setEligible(Boolean eligible) {
        this.eligible = eligible;
    }

    public Double getEligibleAmount() {
        return eligibleAmount;
    }

    public void setEligibleAmount(Double eligibleAmount) {
        this.eligibleAmount = eligibleAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    public Double getCarriedIn() {
        return carriedIn;
    }
    public void setCarriedIn(Double carriedIn) {
        this.carriedIn = carriedIn;
    }
}