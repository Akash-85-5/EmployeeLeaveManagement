package com.emp_management.feature.admin.dto;

import com.emp_management.feature.employee.entity.EmployeeOnboarding;
import com.emp_management.shared.enums.BiometricVpnStatus;
import com.emp_management.shared.enums.EmployeeExperience;
import com.emp_management.shared.enums.EmployeeStatus;

import java.time.LocalDate;

public class UpdateUserRequest {
    private String empId;
    private String name;
    private String email;
    private Long roleId;
    private Long departmentId;
    private Long branchId;
    private Long teamId;
    private String reportingId;
    private EmployeeExperience employeeExperience;
    private LocalDate joiningDate;
    private BiometricVpnStatus biometricStatus;
    private BiometricVpnStatus vpnStatus;
    private String password;
    private EmployeeStatus status;

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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
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

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getReportingId() {
        return reportingId;
    }

    public void setReportingId(String reportingId) {
        this.reportingId = reportingId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }
}
