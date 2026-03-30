package com.emp_management.feature.auth.entity;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.enums.EmployeeStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;


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

    // Delegate convenience getters to Employee
    public String getName()      { return employee.getName(); }
    public String getEmail()     { return employee.getEmail(); }
    public String getRole()        { return employee.getRole().getRoleName(); }
    public Long getReportingId()   { return employee.getReportingId(); }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isForcePwdChange() { return forcePwdChange; }
    public void setForcePwdChange(boolean forcePwdChange) { this.forcePwdChange = forcePwdChange; }

    public EmployeeStatus getStatus() { return employeeStatus; }
    public void setStatus(EmployeeStatus employeeStatus) { this.employeeStatus = employeeStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

