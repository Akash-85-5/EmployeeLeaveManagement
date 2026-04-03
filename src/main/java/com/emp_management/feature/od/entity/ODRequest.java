package com.emp_management.feature.od.entity;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OD (On-Duty) Request Entity
 *
 * Mirrors LeaveApplication structure exactly.
 * Supports:
 *  1.  OD application with future-date validation
 *  2.  Two-level approval workflow
 *  3.  Auto-escalation (handled inside ODService scheduler)
 *  4.  Cancel when PENDING only
 *  5.  No cancel when APPROVED
 *  6.  Approve / Reject at any pending level
 *  7.  Full audit trail (createdAt, updatedAt, @Version)
 *  8.  Overlap prevention
 *  9.  Notification triggers on every state change
 *  10. Cannot apply on dates with approved leave
 */
@Entity
@Table(name = "od_request")
public class ODRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Employee ──────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_code", nullable = false)
    private Employee employee;

    // ── OD Details ────────────────────────────────────────────────
    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "od_year", nullable = false)
    private Integer year;

    // ── Approval Flow (mirrors LeaveApplication exactly) ──────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_approval_level")
    private ApprovalLevel currentApprovalLevel;

    @Column(name = "required_approval_levels")
    private Integer requiredApprovalLevels;

    @Column(name = "current_approver_id")
    private String currentApproverId;

    @Column(name = "first_approver_id")
    private String firstApproverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_approver_decision")
    private RequestStatus firstApproverDecision;

    @Column(name = "first_approver_decided_at")
    private LocalDateTime firstApproverDecidedAt;

    @Column(name = "second_approver_id")
    private String secondApproverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "second_approver_decision")
    private RequestStatus secondApproverDecision;

    @Column(name = "second_approver_decided_at")
    private LocalDateTime secondApproverDecidedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_role")
    private String approvedRole;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ── Escalation ────────────────────────────────────────────────
    @Column(name = "escalated")
    private Boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    // ── Audit ─────────────────────────────────────────────────────
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // ── Lifecycle ─────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.startDate != null) this.year = this.startDate.getYear();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.startDate != null) this.year = this.startDate.getYear();
    }

    // ── Convenience helpers (mirrors LeaveApplication) ────────────
    @Transient
    public String getEmployeeId() {
        return employee != null ? employee.getEmpId() : null;
    }

    @Transient
    public String getEmployeeName() {
        return employee != null ? employee.getName() : null;
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public ApprovalLevel getCurrentApprovalLevel() { return currentApprovalLevel; }
    public void setCurrentApprovalLevel(ApprovalLevel currentApprovalLevel) { this.currentApprovalLevel = currentApprovalLevel; }

    public Integer getRequiredApprovalLevels() { return requiredApprovalLevels; }
    public void setRequiredApprovalLevels(Integer requiredApprovalLevels) { this.requiredApprovalLevels = requiredApprovalLevels; }

    public String getCurrentApproverId() { return currentApproverId; }
    public void setCurrentApproverId(String currentApproverId) { this.currentApproverId = currentApproverId; }

    public String getFirstApproverId() { return firstApproverId; }
    public void setFirstApproverId(String firstApproverId) { this.firstApproverId = firstApproverId; }

    public RequestStatus getFirstApproverDecision() { return firstApproverDecision; }
    public void setFirstApproverDecision(RequestStatus firstApproverDecision) { this.firstApproverDecision = firstApproverDecision; }

    public LocalDateTime getFirstApproverDecidedAt() { return firstApproverDecidedAt; }
    public void setFirstApproverDecidedAt(LocalDateTime firstApproverDecidedAt) { this.firstApproverDecidedAt = firstApproverDecidedAt; }

    public String getSecondApproverId() { return secondApproverId; }
    public void setSecondApproverId(String secondApproverId) { this.secondApproverId = secondApproverId; }

    public RequestStatus getSecondApproverDecision() { return secondApproverDecision; }
    public void setSecondApproverDecision(RequestStatus secondApproverDecision) { this.secondApproverDecision = secondApproverDecision; }

    public LocalDateTime getSecondApproverDecidedAt() { return secondApproverDecidedAt; }
    public void setSecondApproverDecidedAt(LocalDateTime secondApproverDecidedAt) { this.secondApproverDecidedAt = secondApproverDecidedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedRole() { return approvedRole; }
    public void setApprovedRole(String approvedRole) { this.approvedRole = approvedRole; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Boolean getEscalated() { return escalated; }
    public void setEscalated(Boolean escalated) { this.escalated = escalated; }

    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}