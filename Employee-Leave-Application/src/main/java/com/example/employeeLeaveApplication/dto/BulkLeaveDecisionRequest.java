package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.LeaveStatus;
import java.util.List;

public class BulkLeaveDecisionRequest {

    private List<Long> leaveIds;
    private LeaveStatus decision;
    private Long approverId;

    public List<Long> getLeaveIds() {
        return leaveIds;
    }

    public void setLeaveIds(List<Long> leaveIds) {
        this.leaveIds = leaveIds;
    }

    public LeaveStatus getDecision() {
        return decision;
    }

    public void setDecision(LeaveStatus decision) {
        this.decision = decision;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }
}
