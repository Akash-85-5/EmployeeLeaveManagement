package com.emp_management.feature.leave.annual.dto;

import com.emp_management.shared.enums.RequestStatus;
import jakarta.validation.constraints.NotBlank;

public class LeaveDecisionRequest {
    private Long leaveId;
    private RequestStatus decision;
    @NotBlank(message = "Remark is required")
    private String comments;
    private String approverId;

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public Long getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(Long leaveId) {
        this.leaveId = leaveId;
    }

    public RequestStatus getDecision() {
        return decision;
    }

    public void setDecision(RequestStatus decision) {
        this.decision = decision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
