package com.example.notificationservice.dto;

import com.example.notificationservice.enums.LeaveStatus;

import java.util.List;
import java.util.Map;

public class EmployeeDashboardResponse {

    private List<LeaveTypeBreakdown> leaveBalances;
    private Map<LeaveStatus, Long> leaveStatusCount;

    public List<LeaveTypeBreakdown> getLeaveBalances() {
        return leaveBalances;
    }

    public void setLeaveBalances(List<LeaveTypeBreakdown> leaveBalances) {
        this.leaveBalances = leaveBalances;
    }

    public Map<LeaveStatus, Long> getLeaveStatusCount() {
        return leaveStatusCount;
    }

    public void setLeaveStatusCount(Map<LeaveStatus, Long> leaveStatusCount) {
        this.leaveStatusCount = leaveStatusCount;
    }
}

