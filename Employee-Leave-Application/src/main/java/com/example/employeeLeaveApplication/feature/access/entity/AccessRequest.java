package com.example.employeeLeaveApplication.feature.access.entity;

import com.example.employeeLeaveApplication.shared.enums.AccessRequestStatus;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "employee_id")
    private Long employeeId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type")
    private LeaveType accessType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AccessRequestStatus status = AccessRequestStatus.DRAFT;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Employee's reason for requesting access

    // ─── MANAGER DECISION ──────────────────────────────────────
    @Column(name = "reporting_id")
    private Long reportingId; // Manager who will review

    @Column(name = "manager_decision")
    private String managerDecision; // APPROVED / REJECTED

    @Column(name = "manager_remarks", columnDefinition = "TEXT")
    private String managerRemarks; // Why approved/rejected

    @Column(name = "manager_decision_at")
    private LocalDateTime managerDecisionAt;

    // ─── ADMIN DECISION ────────────────────────────────────────
    @Column(name = "admin_decision")
    private String adminDecision; // APPROVED / REJECTED

    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks; // Additional remarks

    @Column(name = "admin_decision_at")
    private LocalDateTime adminDecisionAt;

    // ─── AUDIT FIELDS ─────────────────────────────────────────
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt; // When employee submitted

    // ─── LIFECYCLE CALLBACKS ──────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LeaveType getAccessType() {
        return accessType;
    }

    public void setAccessType(LeaveType accessType) {
        this.accessType = accessType;
    }

    public AccessRequestStatus getStatus() {
        return status;
    }

    public void setStatus(AccessRequestStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }



    public String getManagerDecision() {
        return managerDecision;
    }

    public void setManagerDecision(String managerDecision) {
        this.managerDecision = managerDecision;
    }

    public String getManagerRemarks() {
        return managerRemarks;
    }

    public void setManagerRemarks(String managerRemarks) {
        this.managerRemarks = managerRemarks;
    }

    public LocalDateTime getManagerDecisionAt() {
        return managerDecisionAt;
    }

    public void setManagerDecisionAt(LocalDateTime managerDecisionAt) {
        this.managerDecisionAt = managerDecisionAt;
    }

    public String getAdminDecision() {
        return adminDecision;
    }

    public void setAdminDecision(String adminDecision) {
        this.adminDecision = adminDecision;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }

    public LocalDateTime getAdminDecisionAt() {
        return adminDecisionAt;
    }

    public void setAdminDecisionAt(LocalDateTime adminDecisionAt) {
        this.adminDecisionAt = adminDecisionAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getReportingId() {
        return reportingId;
    }

    public void setReportingId(Long reportingId) {
        this.reportingId = reportingId;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}