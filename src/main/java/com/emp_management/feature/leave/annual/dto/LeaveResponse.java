package com.emp_management.feature.leave.annual.dto;

import com.emp_management.feature.leave.annual.entity.LeaveApplication;

public class LeaveResponse {

    private LeaveApplicationResponseDTO leaveApplication;
    private String warning;

    public LeaveResponse(LeaveApplicationResponseDTO leaveApplication, String warning) {
        this.leaveApplication = leaveApplication;
        this.warning = warning;
    }

    public LeaveApplicationResponseDTO getLeaveApplication() {
        return leaveApplication;
    }

    public void setLeaveApplication(LeaveApplicationResponseDTO leaveApplication) {
        this.leaveApplication = leaveApplication;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}

