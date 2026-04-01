package com.emp_management.feature.leave.carryforward.entity;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Carry-forward leave application with multi-level approval support.
 *
 * Approval lifecycle:
 *   PENDING (currentApprovalLevel=1) → level-1 approves
 *       → single-level flow  : APPROVED  (balance deducted)
 *       → two-level flow     : PENDING (currentApprovalLevel=2)
 *                                → level-2 approves → APPROVED (balance deducted)
 *
 *   REJECTED at any level → no balance change
 *   CANCELLED (by employee) → balance restored if was APPROVED
 *
 * Approval matrix (set at application time via ApprovalMatrixService):
 *   EMPLOYEE    → TEAM_LEADER  → MANAGER  (2 levels)
 *   TEAM_LEADER → MANAGER      → HR       (2 levels)
 *   MANAGER     → HR                      (1 level)
 *   HR          → CEO                     (1 level)
 *   ADMIN       → HR                      (1 level)
 */
@Entity
@Table(name = "carry_forward_leave_application")
public class CarryForwardLeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Applicant ─────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee employee;

    /** Role of the applicant at the time of submission — drives the approval chain. */
    @Column(name = "applicant_role", nullable = false)
    private String applicantRole;

    @Column(name = "carry_year", nullable = false)
    private Integer year;

    // ── Leave window ──────────────────────────────────────────────
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "days", nullable = false, precision = 4, scale = 1)
    private BigDecimal days;

    @Column(nullable = false)
    private String reason;

    // ── Status & approval level ───────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    /**
     * Which approval step we are currently waiting on (1 or 2).
     * Incremented each time a level is completed.
     * When currentApprovalLevel > totalApprovalLevels → APPROVED.
     */
    @Column(name = "current_approval_level", nullable = false)
    private Integer currentApprovalLevel = 1;

    /** Total levels required for this applicant's role (1 or 2). */
    @Column(name = "total_approval_levels", nullable = false)
    private Integer totalApprovalLevels;

    // ── Level 1 approval ──────────────────────────────────────────
    /** Role required to perform level-1 approval (e.g. "TEAM_LEADER"). */
    @Column(name = "level1_required_role", nullable = false)
    private String level1RequiredRole;

    @Column(name = "level1_approved_by")
    private String level1ApprovedBy;

    @Column(name = "level1_approved_at")
    private LocalDateTime level1ApprovedAt;

    // ── Level 2 approval ──────────────────────────────────────────
    /** Role required to perform level-2 approval. Null for single-level flows. */
    @Column(name = "level2_required_role")
    private String level2RequiredRole;

    /** Final approver — level-1 approver id for single-level, level-2 for two-level. */
    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ── Rejection ─────────────────────────────────────────────────
    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    /** Which approval level performed the rejection (1 or 2). */
    @Column(name = "rejected_at_level")
    private Integer rejectedAtLevel;

    // ── Timestamps ────────────────────────────────────────────────
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getLevel1ApprovedBy() {
        return level1ApprovedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public void setLevel1ApprovedBy(String level1ApprovedBy) {
        this.level1ApprovedBy = level1ApprovedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public String getApplicantRole() { return applicantRole; }
    public void setApplicantRole(String applicantRole) { this.applicantRole = applicantRole; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getDays() { return days; }
    public void setDays(BigDecimal days) { this.days = days; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }


    public Integer getCurrentApprovalLevel() { return currentApprovalLevel; }
    public void setCurrentApprovalLevel(Integer currentApprovalLevel) { this.currentApprovalLevel = currentApprovalLevel; }

    public Integer getTotalApprovalLevels() { return totalApprovalLevels; }
    public void setTotalApprovalLevels(Integer totalApprovalLevels) { this.totalApprovalLevels = totalApprovalLevels; }

    public String getLevel1RequiredRole() { return level1RequiredRole; }
    public void setLevel1RequiredRole(String level1RequiredRole) { this.level1RequiredRole = level1RequiredRole; }


    public LocalDateTime getLevel1ApprovedAt() { return level1ApprovedAt; }
    public void setLevel1ApprovedAt(LocalDateTime level1ApprovedAt) { this.level1ApprovedAt = level1ApprovedAt; }

    public String getLevel2RequiredRole() { return level2RequiredRole; }
    public void setLevel2RequiredRole(String level2RequiredRole) { this.level2RequiredRole = level2RequiredRole; }


    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }


    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Integer getRejectedAtLevel() { return rejectedAtLevel; }
    public void setRejectedAtLevel(Integer rejectedAtLevel) { this.rejectedAtLevel = rejectedAtLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}