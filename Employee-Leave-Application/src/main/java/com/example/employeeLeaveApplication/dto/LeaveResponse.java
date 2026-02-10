package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.entity.LeaveApplication;

public class LeaveResponse {

    private LeaveApplication leaveApplication;
    private String warning;

    public LeaveResponse(LeaveApplication leaveApplication, String warning) {
        this.leaveApplication = leaveApplication;
        this.warning = warning;
    }

    public LeaveApplication getLeaveApplication() {
        return leaveApplication;
    }

    public void setLeaveApplication(LeaveApplication leaveApplication) {
        this.leaveApplication = leaveApplication;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}

