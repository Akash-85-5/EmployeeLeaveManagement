package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.AccessRequestStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
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
    private LeaveType accessType; // VPN or BIOMETRIC

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AccessRequestStatus status = AccessRequestStatus.DRAFT;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Employee's reason for requesting access

    // ─── MANAGER DECISION ──────────────────────────────────────
    @Column(name = "manager_id")
    private Long managerId; // Manager who will review

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
}