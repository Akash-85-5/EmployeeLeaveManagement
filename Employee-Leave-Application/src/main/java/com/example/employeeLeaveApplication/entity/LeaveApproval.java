// src/main/java/com/example/employeeLeaveApplication/entity/LeaveApproval.java
package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.ApprovalLevel;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.Role;
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
    private Long approverId;

    /** Which level this approval record belongs to */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false)
    private ApprovalLevel approvalLevel;

    /** Role of the approver */
    @Enumerated(EnumType.STRING)
    @Column(name = "approver_role")
    private Role approverRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private LeaveStatus decision;

    @Column(name = "comments")
    private String comments;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLeaveId() { return leaveId; }
    public void setLeaveId(Long leaveId) { this.leaveId = leaveId; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public ApprovalLevel getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(ApprovalLevel approvalLevel) { this.approvalLevel = approvalLevel; }

    public Role getApproverRole() { return approverRole; }
    public void setApproverRole(Role approverRole) { this.approverRole = approverRole; }

    public LeaveStatus getDecision() { return decision; }
    public void setDecision(LeaveStatus decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}