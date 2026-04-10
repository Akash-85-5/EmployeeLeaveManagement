package com.emp_management.feature.leave.annual.dto;

import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.RequestStatus;
import java.time.LocalDateTime;

public class LeaveRemarkDto {
    private String approverId;
    private String approverRole;
    private String comment;
    private RequestStatus decision;
    private LocalDateTime decisionDate;

    public ApprovalLevel getLevel() {
        return level;
    }

    public void setLevel(ApprovalLevel level) {
        this.level = level;
    }

    private ApprovalLevel level;
    private String rejectionReason; // new field

    // Getters & Setters
    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getApproverRole() { return approverRole; }
    public void setApproverRole(String approverRole) { this.approverRole = approverRole; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public RequestStatus getDecision() { return decision; }
    public void setDecision(RequestStatus decision) { this.decision = decision; }

    public LocalDateTime getDecisionDate() { return decisionDate; }
    public void setDecisionDate(LocalDateTime decisionDate) { this.decisionDate = decisionDate; }


    public String getRejectionReason() { return rejectionReason; }   // getter
    public void setRejectionReason(String rejectionReason) {         // setter
        this.rejectionReason = rejectionReason;
    }

    // Optional: helper method to create from LeaveApproval entity
    public static LeaveRemarkDto fromApproval(com.emp_management.feature.leave.annual.entity.LeaveApproval approval) {
        LeaveRemarkDto r = new LeaveRemarkDto();
        r.setApproverId(approval.getApproverId());
        r.setApproverRole(approval.getApproverRole());
        r.setComment(approval.getComments());
        r.setDecision(approval.getDecision());
        r.setDecisionDate(approval.getDecidedAt());
        r.setLevel(approval.getApprovalLevel());
        // Set rejection reason if decision is REJECTED
        if (approval.getDecision() == RequestStatus.REJECTED) {
            r.setRejectionReason(approval.getComments());
        }
        return r;
    }
}