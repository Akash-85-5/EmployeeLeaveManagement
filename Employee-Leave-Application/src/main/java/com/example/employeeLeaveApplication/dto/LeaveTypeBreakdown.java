package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.LeaveType;

public class LeaveTypeBreakdown {

    private LeaveType leaveType;
    private Double allocatedDays;   // ← ADD
    private Double usedDays;
    private Double remainingDays;   // ← ADD
    private Integer halfDayCount;

    // ✅ EXISTING constructor (3 args) — keep for backward compatibility
    public LeaveTypeBreakdown(LeaveType leaveType,
                              Double usedDays,
                              Integer halfDayCount) {
        this.leaveType    = leaveType;
        this.usedDays     = usedDays;
        this.halfDayCount = halfDayCount;
    }

    // ✅ NEW constructor (5 args) — used by DashboardService
    public LeaveTypeBreakdown(LeaveType leaveType,
                              Double allocatedDays,
                              Double usedDays,
                              Double remainingDays,
                              Integer halfDayCount) {
        this.leaveType     = leaveType;
        this.allocatedDays = allocatedDays;
        this.usedDays      = usedDays;
        this.remainingDays = remainingDays;
        this.halfDayCount  = halfDayCount;
    }

    // Getters & Setters
    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    public Double getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(Double allocatedDays) { this.allocatedDays = allocatedDays; }

    public Double getUsedDays() { return usedDays; }
    public void setUsedDays(Double usedDays) { this.usedDays = usedDays; }

    public Double getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Double remainingDays) { this.remainingDays = remainingDays; }

    public Integer getHalfDayCount() { return halfDayCount; }
    public void setHalfDayCount(Integer halfDayCount) { this.halfDayCount = halfDayCount; }
}