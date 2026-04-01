package com.emp_management.feature.employee.dto;

import com.emp_management.shared.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ProfileResponse {

    // ── From User/Employee — UNCHANGED ────────────────────────────
    private String id;
    private String name;
    private String email;
    private String role;
    private String reportingId;
    private String reportingName;
    private boolean active;
    private boolean mustChangePassword;
    private LocalDate joiningDate;
    private String biometricStatus;
    private String vpnStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Personal details status — UNCHANGED ───────────────────────
    private boolean personalDetailsComplete;
    private boolean personalDetailsLocked;

    // ── NEW: verification status shown on profile page ────────────
    // null = not submitted yet
    // PENDING = submitted, waiting for HR
    // VERIFIED = HR approved
    // REJECTED = HR rejected, employee can resubmit
    private VerificationStatus verificationStatus;

    // ── NEW: shown only when REJECTED ─────────────────────────────
    private String hrRemarks;

    // ── NEW: FRESHER or EXPERIENCED ───────────────────────────────
    private EmployeeExperience employeeExperience;

    // ── UNCHANGED personal fields ─────────────────────────────────
    private String contactNumber;
    private Gender gender;
    private MaritalStatus maritalStatus;
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

    // ── NEW fields ────────────────────────────────────────────────
    private String firstName;
    private String lastName;
    private String accountNumber;
    private String bankName;
    private String pfNumber;      // admin-filled, shown in profile
    private String unaNumber;     // experienced only

    // ── NEW: document paths (shown as download links in profile) ──
    private String aadhaarDocPath;
    private String tcDocPath;               // fresher only
    private String offerLetterDocPath;      // fresher only
    private String experienceCertDocPath;   // experienced only
    private String leavingLetterDocPath;    // experienced only

    // ── NEW: experienced-only text fields ─────────────────────────
    private String previousRole;
    private String oldCompanyName;
    private LocalDate oldCompanyFromDate;
    private LocalDate oldCompanyEndDate;

    // Getters & Setters

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String  getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String  getReportingId() {
        return reportingId;
    }

    public void setReportingId(String reportingId) {
        this.reportingId = reportingId;
    }

    public String getReportingName() {
        return reportingName;
    }

    public void setReportingName(String reportingName) {
        this.reportingName = reportingName;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

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

    public boolean isPersonalDetailsComplete() { return personalDetailsComplete; }
    public void setPersonalDetailsComplete(boolean personalDetailsComplete) { this.personalDetailsComplete = personalDetailsComplete; }

    public boolean isPersonalDetailsLocked() { return personalDetailsLocked; }
    public void setPersonalDetailsLocked(boolean personalDetailsLocked) { this.personalDetailsLocked = personalDetailsLocked; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getHrRemarks() { return hrRemarks; }
    public void setHrRemarks(String hrRemarks) { this.hrRemarks = hrRemarks; }

    public EmployeeExperience getEmployeeExperience() {
        return employeeExperience;
    }

    public void setEmployeeExperience(EmployeeExperience employeeExperience) {
        this.employeeExperience = employeeExperience;
    }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public MaritalStatus getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }

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


    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }


    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getPfNumber() { return pfNumber; }
    public void setPfNumber(String pfNumber) { this.pfNumber = pfNumber; }

    public String getUnaNumber() { return unaNumber; }
    public void setUnaNumber(String unaNumber) { this.unaNumber = unaNumber; }

    public String getAadhaarDocPath() { return aadhaarDocPath; }
    public void setAadhaarDocPath(String aadhaarDocPath) { this.aadhaarDocPath = aadhaarDocPath; }

    public String getTcDocPath() { return tcDocPath; }
    public void setTcDocPath(String tcDocPath) { this.tcDocPath = tcDocPath; }

    public String getOfferLetterDocPath() { return offerLetterDocPath; }
    public void setOfferLetterDocPath(String offerLetterDocPath) { this.offerLetterDocPath = offerLetterDocPath; }

    public String getExperienceCertDocPath() { return experienceCertDocPath; }
    public void setExperienceCertDocPath(String experienceCertDocPath) { this.experienceCertDocPath = experienceCertDocPath; }

    public String getLeavingLetterDocPath() { return leavingLetterDocPath; }
    public void setLeavingLetterDocPath(String leavingLetterDocPath) { this.leavingLetterDocPath = leavingLetterDocPath; }

    public String getPreviousRole() { return previousRole; }
    public void setPreviousRole(String previousRole) { this.previousRole = previousRole; }

    public String getOldCompanyName() { return oldCompanyName; }
    public void setOldCompanyName(String oldCompanyName) { this.oldCompanyName = oldCompanyName; }

    public LocalDate getOldCompanyFromDate() { return oldCompanyFromDate; }
    public void setOldCompanyFromDate(LocalDate oldCompanyFromDate) { this.oldCompanyFromDate = oldCompanyFromDate; }

    public LocalDate getOldCompanyEndDate() { return oldCompanyEndDate; }
    public void setOldCompanyEndDate(LocalDate oldCompanyEndDate) { this.oldCompanyEndDate = oldCompanyEndDate; }
}