package com.emp_management.feature.auth.entity;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.enums.EmployeeStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Changes from original:
 *  - Added `lastPasswordChangeAt` (Instant) used to invalidate JWTs issued before
 *    a password reset.  Every password change (change-password AND forgot-password
 *    reset) must update this field.  The JWT filter rejects tokens whose `iat` is
 *    before this value.
 *  - Removed refresh-token dependency (no longer needed).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "force_pwd_change", nullable = false)
    private boolean forcePwdChange;

    /**
     * Set to Instant.now() on every password change.
     * JWT filter: if token.iat < lastPasswordChangeAt → 401.
     * NULL means password was never changed → all tokens valid.
     */
    @Column(name = "last_password_change_at")
    private Instant lastPasswordChangeAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus employeeStatus;

    @OneToOne
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

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

    // ── Delegate convenience getters ──────────────────────────────────────

    public String getName()        { return employee.getName(); }
    public String getEmail()       { return employee.getEmail(); }
    public String getRole()        { return employee.getRole().getRoleName(); }
    public String getReportingId() { return employee.getReportingId(); }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isForcePwdChange() { return forcePwdChange; }
    public void setForcePwdChange(boolean forcePwdChange) { this.forcePwdChange = forcePwdChange; }

    public Instant getLastPasswordChangeAt() { return lastPasswordChangeAt; }
    public void setLastPasswordChangeAt(Instant lastPasswordChangeAt) {
        this.lastPasswordChangeAt = lastPasswordChangeAt;
    }

    public EmployeeStatus getStatus() { return employeeStatus; }
    public void setStatus(EmployeeStatus employeeStatus) { this.employeeStatus = employeeStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}