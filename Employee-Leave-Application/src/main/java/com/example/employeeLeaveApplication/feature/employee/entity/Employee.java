package com.example.employeeLeaveApplication.feature.employee.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.employeeLeaveApplication.shared.enums.BiometricVpnStatus;
import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import com.example.employeeLeaveApplication.shared.enums.SeparationType;

import jakarta.persistence.*;

import static com.example.employeeLeaveApplication.shared.enums.BiometricVpnStatus.PENDING;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "team_leader_id")
    private Long teamLeaderId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "active")
    private boolean active = true;

    // ─────────────────────────────────────────────────────────────
    // 🔥 SEPARATION / NOTICE PERIOD FIELDS
    // ─────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "separation_status")
    private SeparationStatus separationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "separation_type")
    private SeparationType separationType;

    @Column(name = "notice_start_date")
    private LocalDate noticeStartDate;

    @Column(name = "notice_end_date")
    private LocalDate noticeEndDate;

    @Column(name = "relieved_date")
    private LocalDate relievedDate;

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD FIELDS (Onboarding & Compliance)
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "biometric_status")
    private BiometricVpnStatus biometricStatus = PENDING; // PENDING, COMPLETED, FAILED

    @Enumerated(EnumType.STRING)
    @Column(name = "vpn_status")
    private BiometricVpnStatus vpnStatus = PENDING; // PENDING, COMPLETED, FAILED

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    // ═══════════════════════════════════════════════════════════════
    // AUDIT FIELDS
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════════

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public Long getTeamLeaderId() { return teamLeaderId; }
    public void setTeamLeaderId(Long teamLeaderId) { this.teamLeaderId = teamLeaderId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public SeparationStatus getSeparationStatus() { return separationStatus; }
    public void setSeparationStatus(SeparationStatus separationStatus) { this.separationStatus = separationStatus; }

    public SeparationType getSeparationType() { return separationType; }
    public void setSeparationType(SeparationType separationType) { this.separationType = separationType; }

    public LocalDate getNoticeStartDate() { return noticeStartDate; }
    public void setNoticeStartDate(LocalDate noticeStartDate) { this.noticeStartDate = noticeStartDate; }

    public LocalDate getNoticeEndDate() { return noticeEndDate; }
    public void setNoticeEndDate(LocalDate noticeEndDate) { this.noticeEndDate = noticeEndDate; }

    public LocalDate getRelievedDate() { return relievedDate; }
    public void setRelievedDate(LocalDate relievedDate) { this.relievedDate = relievedDate; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public BiometricVpnStatus getBiometricStatus() { return biometricStatus; }
    public void setBiometricStatus(BiometricVpnStatus biometricStatus) { this.biometricStatus = biometricStatus; }

    public BiometricVpnStatus getVpnStatus() { return vpnStatus; }
    public void setVpnStatus(BiometricVpnStatus vpnStatus) { this.vpnStatus = vpnStatus; }

    public LocalDateTime getOnboardingCompletedAt() { return onboardingCompletedAt; }
    public void setOnboardingCompletedAt(LocalDateTime onboardingCompletedAt) { this.onboardingCompletedAt = onboardingCompletedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}