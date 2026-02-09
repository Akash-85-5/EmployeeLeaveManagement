package com.example.notificationservice.dto;

import com.example.notificationservice.entity.LeaveApplication;

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

