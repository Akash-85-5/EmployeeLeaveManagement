package com.emp_management.feature.employee.entity;

import com.emp_management.shared.entity.Branch;
import com.emp_management.shared.entity.Department;
import com.emp_management.shared.entity.Role;
import com.emp_management.shared.enums.BiometricVpnStatus;
import com.emp_management.shared.enums.EmployeeExperience;
import com.emp_management.shared.enums.SeparationStatus;
import com.emp_management.shared.enums.SeparationType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.emp_management.shared.enums.BiometricVpnStatus.PENDING;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @Column(name = "employee_id", unique = true, length = 20)
    private String empId;

    @Column(name = "team_id")
    private Long teamId;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_experience")
    private EmployeeExperience employeeExperience;

    @Column(name = "reporting_id")
    private String reportingId;

    @Column(name = "active")
    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private EmployeeSeparation separation;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private EmployeeOnboarding onboarding;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
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

    public EmployeeExperience getEmployeeExperience() {
        return employeeExperience;
    }

    public void setEmployeeExperience(EmployeeExperience employeeExperience) {
        this.employeeExperience = employeeExperience;
    }

    public String getReportingId() {
        return reportingId;
    }

    public void setReportingId(String reportingId) {
        this.reportingId = reportingId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
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

    public EmployeeSeparation getSeparation() {
        return separation;
    }

    public void setSeparation(EmployeeSeparation separation) {
        this.separation = separation;
    }

    public EmployeeOnboarding getOnboarding() {
        return onboarding;
    }

    public void setOnboarding(EmployeeOnboarding onboarding) {
        this.onboarding = onboarding;
    }
}

