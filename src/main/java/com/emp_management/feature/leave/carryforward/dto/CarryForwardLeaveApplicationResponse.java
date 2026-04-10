package com.emp_management.feature.leave.carryforward.dto;

import com.emp_management.shared.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CarryForwardLeaveApplicationResponse {

    private Long id;

    // ── Applicant ─────────────────────────────────────────────────
    private String employeeId;
    private String employeeName;
    private String applicantRole;
    private Integer year;

    // ── Leave window ──────────────────────────────────────────────
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private String reason;

    // ── Status & approval level ───────────────────────────────────
    private RequestStatus status;
    private Integer currentApprovalLevel;
    private Integer totalApprovalLevels;

    /** Human-readable summary of what happens next, e.g. "Awaiting MANAGER approval". */
    private String nextAction;

    // ── Level 1 approval ──────────────────────────────────────────
    private String level1RequiredRole;
    private String level1ApprovedBy;
    private String level1ApprovedByName;
    private LocalDateTime level1ApprovedAt;

    // ── Level 2 / final approval ──────────────────────────────────
    private String level2RequiredRole;
    private String approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;

    // ── Rejection ─────────────────────────────────────────────────
    private String rejectedBy;
    private String rejectedByName;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private Integer rejectedAtLevel;

    // ── Balance context ───────────────────────────────────────────
    private Double cfBalanceBefore;
    private Double cfBalanceAfter;

    private LocalDateTime createdAt;

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

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

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public Integer getCurrentApprovalLevel() { return currentApprovalLevel; }
    public void setCurrentApprovalLevel(Integer currentApprovalLevel) { this.currentApprovalLevel = currentApprovalLevel; }

    public Integer getTotalApprovalLevels() { return totalApprovalLevels; }
    public void setTotalApprovalLevels(Integer totalApprovalLevels) { this.totalApprovalLevels = totalApprovalLevels; }

    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }

    public String getLevel1RequiredRole() { return level1RequiredRole; }
    public void setLevel1RequiredRole(String level1RequiredRole) { this.level1RequiredRole = level1RequiredRole; }

    public String  getLevel1ApprovedBy() { return level1ApprovedBy; }
    public void setLevel1ApprovedBy(String level1ApprovedBy) { this.level1ApprovedBy = level1ApprovedBy; }

    public String getLevel1ApprovedByName() { return level1ApprovedByName; }
    public void setLevel1ApprovedByName(String level1ApprovedByName) { this.level1ApprovedByName = level1ApprovedByName; }

    public LocalDateTime getLevel1ApprovedAt() { return level1ApprovedAt; }
    public void setLevel1ApprovedAt(LocalDateTime level1ApprovedAt) { this.level1ApprovedAt = level1ApprovedAt; }

    public String getLevel2RequiredRole() { return level2RequiredRole; }
    public void setLevel2RequiredRole(String level2RequiredRole) { this.level2RequiredRole = level2RequiredRole; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }

    public String getRejectedByName() { return rejectedByName; }
    public void setRejectedByName(String rejectedByName) { this.rejectedByName = rejectedByName; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Integer getRejectedAtLevel() { return rejectedAtLevel; }
    public void setRejectedAtLevel(Integer rejectedAtLevel) { this.rejectedAtLevel = rejectedAtLevel; }

    public Double getCfBalanceBefore() { return cfBalanceBefore; }
    public void setCfBalanceBefore(Double cfBalanceBefore) { this.cfBalanceBefore = cfBalanceBefore; }

    public Double getCfBalanceAfter() { return cfBalanceAfter; }
    public void setCfBalanceAfter(Double cfBalanceAfter) { this.cfBalanceAfter = cfBalanceAfter; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}