package com.emp_management.feature.dashboard.dto;

public class TeamMemberBalance {

    private String employeeId;    // was Long
    private String employeeName;
    private Double totalAllocated;
    private Double totalUsed;
    private Double totalRemaining;
    private Double compOffBalance;
    private Double lopPercentage;
    private Integer totalWorkingDays;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public Double getTotalAllocated() { return totalAllocated; }
    public void setTotalAllocated(Double totalAllocated) { this.totalAllocated = totalAllocated; }

    public Double getTotalUsed() { return totalUsed; }
    public void setTotalUsed(Double totalUsed) { this.totalUsed = totalUsed; }

    public Double getTotalRemaining() { return totalRemaining; }
    public void setTotalRemaining(Double totalRemaining) { this.totalRemaining = totalRemaining; }

    public Double getCompOffBalance() { return compOffBalance; }
    public void setCompOffBalance(Double compOffBalance) { this.compOffBalance = compOffBalance; }

    public Double getLopPercentage() { return lopPercentage; }
    public void setLopPercentage(Double lopPercentage) { this.lopPercentage = lopPercentage; }

    public Integer getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(Integer totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }
}