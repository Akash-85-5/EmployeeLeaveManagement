package com.emp_management.feature.dashboard.dto;


import com.emp_management.feature.leave.annual.entity.LeaveType;

public class LeaveTypeBreakdown {

    // LeaveType is now an entity, not an enum — store the name as String
    private String leaveTypeName;
    private Double allocatedDays;
    private Double usedDays;
    private Double remainingDays;
    private Integer halfDayCount;
    private Long pendingCount;

    public LeaveTypeBreakdown(String leaveTypeName,
                              Double allocatedDays,
                              Double usedDays,
                              Double remainingDays,
                              Integer halfDayCount,
                              Long pendingCount) {
        this.leaveTypeName = leaveTypeName;
        this.allocatedDays = allocatedDays;
        this.usedDays      = usedDays;
        this.remainingDays = remainingDays;
        this.halfDayCount  = halfDayCount;
        this.pendingCount  = pendingCount;
    }

    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }

    public Double getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(Double allocatedDays) { this.allocatedDays = allocatedDays; }

    public Double getUsedDays() { return usedDays; }
    public void setUsedDays(Double usedDays) { this.usedDays = usedDays; }

    public Double getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Double remainingDays) { this.remainingDays = remainingDays; }

    public Integer getHalfDayCount() { return halfDayCount; }
    public void setHalfDayCount(Integer halfDayCount) { this.halfDayCount = halfDayCount; }

    public Long getPendingCount() { return pendingCount; }
    public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }
}