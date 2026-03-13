package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.LeaveStatus;

public class LeaveDecisionRequest {
    private Long leaveId;
    private Long managerId;
    private LeaveStatus decision;
    private String comments;
    private Long approverId;

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public Long getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(Long leaveId) {
        this.leaveId = leaveId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public LeaveStatus getDecision() {
        return decision;
    }

    public void setDecision(LeaveStatus decision) {
        this.decision = decision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
