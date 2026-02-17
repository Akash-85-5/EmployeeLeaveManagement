package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.entity.LeaveApplication;

public class TeamVisibilityDTO {

    private Long employeeId;
    private String employeeName;
    private LeaveApplication leaveThisWeek; // null if no leave

    public TeamVisibilityDTO(
            Long employeeId,
            String employeeName,
            LeaveApplication leaveThisWeek
    ) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.leaveThisWeek = leaveThisWeek;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public LeaveApplication getLeaveThisWeek() {
        return leaveThisWeek;
    }
}
