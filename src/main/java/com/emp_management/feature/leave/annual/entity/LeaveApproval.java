package com.emp_management.feature.leave.annual.entity;

import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_approval")
public class LeaveApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leave_id", nullable = false)
    private Long leaveId;

    /** The approver's employee ID */
    @Column(name = "approver_id", nullable = false)
    private String approverId;

    /** Which level this approval record belongs to */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false)
    private ApprovalLevel approvalLevel;

    @Column(name = "approver_role")
    private String approverRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private RequestStatus decision;

    @Column(name = "comments")
    private String comments;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLeaveId() { return leaveId; }
    public void setLeaveId(Long leaveId) { this.leaveId = leaveId; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public ApprovalLevel getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(ApprovalLevel approvalLevel) { this.approvalLevel = approvalLevel; }

    public String getApproverRole() { return approverRole; }
    public void setApproverRole(String approverRole) { this.approverRole = approverRole; }

    public RequestStatus getDecision() { return decision; }
    public void setDecision(RequestStatus decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}