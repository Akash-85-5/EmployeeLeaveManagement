package com.example.employeeLeaveApplication.feature.employee.dto;

public class EmployeeProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private Long managerId;
    private boolean active;
    private String joiningDate;

    private String biometricStatus;
    private String vpnStatus;

    private String verificationStatus;
    private boolean forcePasswordChange;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
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

    public String getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getBiometricStatus() {
        return biometricStatus;
    }

    public void setBiometricStatus(String biometricStatus) {
        this.biometricStatus = biometricStatus;
    }

    public String getVpnStatus() {
        return vpnStatus;
    }

    public void setVpnStatus(String vpnStatus) {
        this.vpnStatus = vpnStatus;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }
}
