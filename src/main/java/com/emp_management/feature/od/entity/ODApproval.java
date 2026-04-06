package com.emp_management.feature.od.entity;

import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Audit trail of every approve / reject decision made on an ODRequest.
 * Mirrors LeaveApproval exactly — one row per decision, per approver, per level.
 *
 * Relationship to ODRequest is by odId (Long FK), not a JPA @ManyToOne,
 * to keep the pattern identical to LeaveApproval.leaveId.
 */
@Entity
@Table(name = "od_approval")
public class ODApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → od_request.id */
    @Column(name = "od_id", nullable = false)
    private Long odId;

    /** empId of the approver who made this decision */
    @Column(name = "approver_id", nullable = false)
    private String approverId;

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

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOdId() { return odId; }
    public void setOdId(Long odId) { this.odId = odId; }

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