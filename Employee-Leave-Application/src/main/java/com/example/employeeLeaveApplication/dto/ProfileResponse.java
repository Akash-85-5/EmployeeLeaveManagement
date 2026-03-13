package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.BloodGroup;
import com.example.employeeLeaveApplication.enums.Gender;
import com.example.employeeLeaveApplication.enums.MaritalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ProfileResponse {

    // ── From User/Employee ───────────────────────────────────────
    private Long id;
    private String name;
    private String email;
    private String role;
    private Long managerId;
    private String managerName;
    private Long teamLeaderId;
    private String teamLeaderName;
    private boolean active;
    private boolean mustChangePassword;
    private LocalDate joiningDate;
    private String biometricStatus;
    private String vpnStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String contactNumber;
    private Gender gender;
    private String aadharNumber;
    private String personalEmail;
    private LocalDate dateOfBirth;
    private String presentAddress;
    private String permanentAddress;
    private BloodGroup bloodGroup;
    private String emergencyContactNumber;
    private String fatherName;
    private String motherName;
    private String designation;
    private List<String> skillSet;

    private boolean personalDetailsComplete;

    private boolean personalDetailsLocked;

    // Getters & Setters
    public boolean isPersonalDetailsLocked() { return personalDetailsLocked; }
    public void setPersonalDetailsLocked(boolean personalDetailsLocked) {
        this.personalDetailsLocked = personalDetailsLocked;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public Long getTeamLeaderId() { return teamLeaderId; }
    public void setTeamLeaderId(Long teamLeaderId) { this.teamLeaderId = teamLeaderId; }

    public String getTeamLeaderName() { return teamLeaderName; }
    public void setTeamLeaderName(String teamLeaderName) { this.teamLeaderName = teamLeaderName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public String getBiometricStatus() { return biometricStatus; }
    public void setBiometricStatus(String biometricStatus) { this.biometricStatus = biometricStatus; }

    public String getVpnStatus() { return vpnStatus; }
    public void setVpnStatus(String vpnStatus) { this.vpnStatus = vpnStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }

    public String getPersonalEmail() { return personalEmail; }
    public void setPersonalEmail(String personalEmail) { this.personalEmail = personalEmail; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getPresentAddress() { return presentAddress; }
    public void setPresentAddress(String presentAddress) { this.presentAddress = presentAddress; }

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getEmergencyContactNumber() { return emergencyContactNumber; }
    public void setEmergencyContactNumber(String emergencyContactNumber) { this.emergencyContactNumber = emergencyContactNumber; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public List<String> getSkillSet() { return skillSet; }
    public void setSkillSet(List<String> skillSet) { this.skillSet = skillSet; }

    public boolean isPersonalDetailsComplete() { return personalDetailsComplete; }
    public void setPersonalDetailsComplete(boolean personalDetailsComplete) { this.personalDetailsComplete = personalDetailsComplete; }
}