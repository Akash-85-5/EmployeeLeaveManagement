package com.example.employeeLeaveApplication.feature.leave.carryforward.dto;

public class CarryForwardBalanceResponse {

    private Long employeeId;
    private String employeeName;
    private Integer year;
    private Double totalCarriedForward;
    private Double totalUsed;
    private Double remaining;

    public CarryForwardBalanceResponse() {
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

    public Double getTotalCarriedForward() {
        return totalCarriedForward;
    }

    public void setTotalCarriedForward(Double totalCarriedForward) {
        this.totalCarriedForward = totalCarriedForward;
    }

    public Double getTotalUsed() {
        return totalUsed;
    }

    public void setTotalUsed(Double totalUsed) {
        this.totalUsed = totalUsed;
    }

    public Double getRemaining() {
        return remaining;
    }

    public void setRemaining(Double remaining) {
        this.remaining = remaining;
    }
}