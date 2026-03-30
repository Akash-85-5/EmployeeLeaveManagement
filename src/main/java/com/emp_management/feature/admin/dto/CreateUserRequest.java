package com.emp_management.feature.admin.dto;

import com.emp_management.shared.entity.Role;
import com.emp_management.shared.enums.EmployeeExperience;

import java.time.LocalDate;

public class CreateUserRequest {
    private String empId;
    private String name;
    private String email;
    private Long role;
    private Long reportingId;
    private Long teamId;
    private Long departmentId;
    private Long branchId;
    private Long countryId;
    private Long companyId;
    private EmployeeExperience employeeExperience;
    private LocalDate joiningDate;

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
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

    public Long getRole() {
        return role;
    }

    public void setRole(Long role) {
        this.role = role;
    }

    public Long getReportingId() {
        return reportingId;
    }

    public void setReportingId(Long reportingId) {
        this.reportingId = reportingId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public Long getCountryId() {
        return countryId;
    }

    public void setCountryId(Long countryId) {
        this.countryId = countryId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public EmployeeExperience getEmployeeExperience() {
        return employeeExperience;
    }

    public void setEmployeeExperience(EmployeeExperience employeeExperience) {
        this.employeeExperience = employeeExperience;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }
}
