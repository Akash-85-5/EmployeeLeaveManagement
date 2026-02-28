package com.example.employeeLeaveApplication.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.employeeLeaveApplication.enums.BiometricVpnStatus;
import com.example.employeeLeaveApplication.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import static com.example.employeeLeaveApplication.enums.BiometricVpnStatus.PENDING;


@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public BiometricVpnStatus getBiometricStatus() {
        return biometricStatus;
    }

    public void setBiometricStatus(BiometricVpnStatus biometricStatus) {
        this.biometricStatus = biometricStatus;
    }

    public BiometricVpnStatus getVpnStatus() {
        return vpnStatus;
    }

    public void setVpnStatus(BiometricVpnStatus vpnStatus) {
        this.vpnStatus = vpnStatus;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public LocalDateTime getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public void setOnboardingCompletedAt(LocalDateTime onboardingCompletedAt) {
        this.onboardingCompletedAt = onboardingCompletedAt;
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

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}