package com.emp_management.feature.leave.annual.dto;

import com.emp_management.feature.leave.annual.entity.LeaveType;

public class AnnualLeaveBalanceResponse {

    private String  employeeId;
    private Integer year;
    private Integer month;
    private Double  availableDays;
    private Double  usedDays;
    private Double  remainingDays;

    public AnnualLeaveBalanceResponse() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Double getAvailableDays() { return availableDays; }
    public void setAvailableDays(Double availableDays) { this.availableDays = availableDays; }

    public Double getUsedDays() { return usedDays; }
    public void setUsedDays(Double usedDays) { this.usedDays = usedDays; }

    public Double getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Double remainingDays) { this.remainingDays = remainingDays; }
}