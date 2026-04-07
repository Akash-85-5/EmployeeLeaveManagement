package com.emp_management.feature.dashboard.dto;

import java.util.List;

public class MonthlyStatsResponse {

    private String employeeId;    // was Long
    private Integer year;
    private Integer month;
    private Integer totalApprovedCount;
    private Double totalDays;
    private Boolean exceededLimit;
    private List<LeaveTypeStat> breakdown;

    public MonthlyStatsResponse() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getTotalApprovedCount() { return totalApprovedCount; }
    public void setTotalApprovedCount(Integer totalApprovedCount) { this.totalApprovedCount = totalApprovedCount; }

    public Double getTotalDays() { return totalDays; }
    public void setTotalDays(Double totalDays) { this.totalDays = totalDays; }

    public Boolean getExceededLimit() { return exceededLimit; }
    public void setExceededLimit(Boolean exceededLimit) { this.exceededLimit = exceededLimit; }

    public List<LeaveTypeStat> getBreakdown() { return breakdown; }
    public void setBreakdown(List<LeaveTypeStat> breakdown) { this.breakdown = breakdown; }

    public static class LeaveTypeStat {
        private String leaveType;
        private Integer count;
        private Double totalDays;

        public LeaveTypeStat() {}
        public LeaveTypeStat(String leaveType, Integer count, Double totalDays) {
            this.leaveType = leaveType;
            this.count     = count;
            this.totalDays = totalDays;
        }

        public String getLeaveType() { return leaveType; }
        public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }

        public Double getTotalDays() { return totalDays; }
        public void setTotalDays(Double totalDays) { this.totalDays = totalDays; }
    }
}