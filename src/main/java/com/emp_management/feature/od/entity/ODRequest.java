package com.emp_management.feature.od.entity;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.ODPurpose;
import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OD (On-Duty) Request entity.
 *
 * Mirrors LeaveApplication's approval chain pattern:
 *  - employee       → FK to Employee (String empId PK)
 *  - firstApproverId / secondApproverId / currentApproverId → String empId references
 *  - escalation fields match LeaveApplication exactly
 *  - NO duplicate columns — each field has one clear purpose
 *
 * No LeaveType FK needed because OD is always "ON_DUTY" — that is captured
 * by the separate ODPurpose enum, which describes WHY the employee is on duty.
 */
@Entity
@Table(name = "od_request")
public class ODRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Employee (FK → employee.emp_id, String PK) ────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ── OD details ────────────────────────────────────────────────
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Calculated working days for this OD (same logic as LeaveApplication.days).
     * Stored so approval/reporting never needs to recompute.
     */
    @Column(nullable = false)
    private BigDecimal days;

    /** Purpose / nature of the on-duty work */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private ODPurpose purpose;

    @Column(nullable = false, length = 500)
    private String reason;

    /** Client name / location if purpose is CLIENT_VISIT */
    @Column(name = "client_location", length = 255)
    private String clientLocation;

    // ── Status ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    // ── Approval chain (mirrors LeaveApplication exactly) ─────────
    @Enumerated(EnumType.STRING)
    @Column(name = "current_approval_level")
    private ApprovalLevel currentApprovalLevel;

    @Column(name = "required_approval_levels")
    private Integer requiredApprovalLevels;

    /** empId of whoever must act RIGHT NOW */
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

    // ── Final approval stamp ──────────────────────────────────────
    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_role")
    private String approvedRole;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ── Escalation (mirrors LeaveApplication) ─────────────────────
    @Column(name = "escalated")
    private Boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    // ── Reminder / escalation tracking (used by ODReminderSchedulerService) ──
    /** How many reminders have been sent to the CURRENT approver in this cycle */
    @Column(name = "reminder_count")
    private Integer reminderCount = 0;

    /** When the last reminder was dispatched */
    @Column(name = "last_reminder_sent_at")
    private LocalDateTime lastReminderSentAt;

    /** Approval level that was active when the last reminder cycle started.
     *  Used to detect when the level changes between scheduler runs (i.e. first
     *  approver acted and leave moved to second approver). */
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_level_snapshot")
    private ApprovalLevel reminderLevelSnapshot;

    // ── Audit ─────────────────────────────────────────────────────
    @Column(name = "od_year", nullable = false)
    private Integer year;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // ── Lifecycle callbacks ───────────────────────────────────────

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

    // ── Transient helpers (same pattern as LeaveApplication) ──────

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

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getDays() { return days; }
    public void setDays(BigDecimal days) { this.days = days; }

    public ODPurpose getPurpose() { return purpose; }
    public void setPurpose(ODPurpose purpose) { this.purpose = purpose; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getClientLocation() { return clientLocation; }
    public void setClientLocation(String clientLocation) { this.clientLocation = clientLocation; }

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

    public Integer getReminderCount() { return reminderCount; }
    public void setReminderCount(Integer reminderCount) { this.reminderCount = reminderCount; }

    public LocalDateTime getLastReminderSentAt() { return lastReminderSentAt; }
    public void setLastReminderSentAt(LocalDateTime lastReminderSentAt) { this.lastReminderSentAt = lastReminderSentAt; }

    public ApprovalLevel getReminderLevelSnapshot() { return reminderLevelSnapshot; }
    public void setReminderLevelSnapshot(ApprovalLevel reminderLevelSnapshot) { this.reminderLevelSnapshot = reminderLevelSnapshot; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}