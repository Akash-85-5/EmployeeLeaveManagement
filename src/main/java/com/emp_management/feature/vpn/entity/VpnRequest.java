package com.emp_management.feature.vpn.entity;

import com.emp_management.shared.enums.VpnRequestStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vpn_requests")
public class VpnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_id", nullable = false)
    private String applicantId;

    @Column(name = "applicant_role", nullable = false)
    private String applicantRole;

    @Column(name = "manager_approver_id")
    private String managerApproverId;

    @Column(nullable = false, length = 255)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VpnRequestStatus status = VpnRequestStatus.PENDING_MANAGER;

    @Column(name = "manager_remarks", length = 500)
    private String managerRemarks;

    @Column(name = "manager_actioned_at")
    private LocalDateTime managerActionedAt;

    @Column(name = "admin_remarks", length = 500)
    private String adminRemarks;

    @Column(name = "admin_id")
    private String adminId;

    @Column(name = "admin_actioned_at")
    private LocalDateTime adminActionedAt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VpnRequestStatus.PENDING_MANAGER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }

    // FIX: setId was missing — needed for testing/mapping scenarios
    public void setId(Long id) { this.id = id; }

    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

    public String getApplicantRole() { return applicantRole; }
    public void setApplicantRole(String applicantRole) { this.applicantRole = applicantRole; }

    public String getManagerApproverId() { return managerApproverId; }
    public void setManagerApproverId(String managerApproverId) { this.managerApproverId = managerApproverId; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public VpnRequestStatus getStatus() { return status; }
    public void setStatus(VpnRequestStatus status) { this.status = status; }

    public String getManagerRemarks() { return managerRemarks; }
    public void setManagerRemarks(String managerRemarks) { this.managerRemarks = managerRemarks; }

    public LocalDateTime getManagerActionedAt() { return managerActionedAt; }
    public void setManagerActionedAt(LocalDateTime managerActionedAt) { this.managerActionedAt = managerActionedAt; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public LocalDateTime getAdminActionedAt() { return adminActionedAt; }
    public void setAdminActionedAt(LocalDateTime adminActionedAt) { this.adminActionedAt = adminActionedAt; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}