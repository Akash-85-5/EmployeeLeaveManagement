package com.emp_management.feature.employee.dto;

import com.emp_management.shared.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only profile response DTO.
 * No JPA entities — all nested types are plain DTOs.
 */
public class ProfileResponse {

    // ── Employee / User core ──────────────────────────────────────
    private String id;
    private String name;
    private String email;
    private String role;
    private String departmentName;
    private String reportingId;
    private String reportingName;
    private boolean active;
    private boolean mustChangePassword;
    private LocalDate joiningDate;
    private String biometricStatus;
    private String vpnStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String branch;
    private String country;
    private String companyName;

    // ── Personal details meta ─────────────────────────────────────
    private boolean personalDetailsComplete;
    private boolean personalDetailsLocked;
    private VerificationStatus verificationStatus;
    private String hrRemarks;
    private EmployeeExperience employeeExperience;

    // ── Personal fields ───────────────────────────────────────────
    private String firstName;
    private String lastName;
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
    private LocalDate fatherDateOfBirth;
    private LocalDate motherDateOfBirth;
    private String fatherOccupation;
    private String motherOccupation;
    private String motherName;
    private String designation;
    private List<String> skillSet;

    // ── Bank ──────────────────────────────────────────────────────
    private String accountNumber;
    private String bankName;
    private String ifscCode;
    private String bankBranchName;
    private String pfNumber;
    private String uanNumber;

    // ── Spouse (only when MARRIED) ────────────────────────────────
    private String spouseName;
    private LocalDate spouseDateOfBirth;
    private String spouseOccupation;
    private String spouseContactNumber;

    // ── Children ──────────────────────────────────────────────────
    private List<ChildDto> children;

    // ── FRESHER document paths ────────────────────────────────────
    private String idProofPath;
    private String tenthMarksheetPath;
    private String twelfthMarksheetPath;
    private String degreeCertificatePath;
    private String offerLetterPath;
    private String passportPhotoPath;

    private List<ExperiencedDocumentDto> experiencedDocuments;

    // ── Getters & Setters ─────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getReportingId() { return reportingId; }
    public void setReportingId(String reportingId) { this.reportingId = reportingId; }

    public String getReportingName() { return reportingName; }
    public void setReportingName(String reportingName) { this.reportingName = reportingName; }

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

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public boolean isPersonalDetailsComplete() { return personalDetailsComplete; }
    public void setPersonalDetailsComplete(boolean personalDetailsComplete) { this.personalDetailsComplete = personalDetailsComplete; }

    public boolean isPersonalDetailsLocked() { return personalDetailsLocked; }
    public void setPersonalDetailsLocked(boolean personalDetailsLocked) { this.personalDetailsLocked = personalDetailsLocked; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getHrRemarks() { return hrRemarks; }
    public void setHrRemarks(String hrRemarks) { this.hrRemarks = hrRemarks; }

    public EmployeeExperience getEmployeeExperience() { return employeeExperience; }
    public void setEmployeeExperience(EmployeeExperience employeeExperience) { this.employeeExperience = employeeExperience; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

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
    public void setEmergencyContactNumber(String s) { this.emergencyContactNumber = s; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public List<String> getSkillSet() { return skillSet; }
    public void setSkillSet(List<String> skillSet) { this.skillSet = skillSet; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBankBranchName() { return bankBranchName; }
    public void setBankBranchName(String bankBranchName) { this.bankBranchName = bankBranchName; }

    public String getPfNumber() { return pfNumber; }
    public void setPfNumber(String pfNumber) { this.pfNumber = pfNumber; }

    public String getUanNumber() { return uanNumber; }
    public void setUanNumber(String uanNumber) { this.uanNumber = uanNumber; }

    public String getSpouseName() { return spouseName; }
    public void setSpouseName(String spouseName) { this.spouseName = spouseName; }

    public LocalDate getSpouseDateOfBirth() { return spouseDateOfBirth; }
    public void setSpouseDateOfBirth(LocalDate d) { this.spouseDateOfBirth = d; }

    public String getSpouseOccupation() { return spouseOccupation; }
    public void setSpouseOccupation(String spouseOccupation) { this.spouseOccupation = spouseOccupation; }

    public String getSpouseContactNumber() { return spouseContactNumber; }
    public void setSpouseContactNumber(String s) { this.spouseContactNumber = s; }

    public List<ChildDto> getChildren() { return children; }
    public void setChildren(List<ChildDto> children) { this.children = children; }

    public String getIdProofPath() { return idProofPath; }
    public void setIdProofPath(String idProofPath) { this.idProofPath = idProofPath; }

    public String getTenthMarksheetPath() { return tenthMarksheetPath; }
    public void setTenthMarksheetPath(String p) { this.tenthMarksheetPath = p; }

    public String getTwelfthMarksheetPath() { return twelfthMarksheetPath; }
    public void setTwelfthMarksheetPath(String p) { this.twelfthMarksheetPath = p; }

    public String getDegreeCertificatePath() { return degreeCertificatePath; }
    public void setDegreeCertificatePath(String p) { this.degreeCertificatePath = p; }

    public String getOfferLetterPath() { return offerLetterPath; }
    public void setOfferLetterPath(String offerLetterPath) { this.offerLetterPath = offerLetterPath; }

    public String getPassportPhotoPath() { return passportPhotoPath; }
    public void setPassportPhotoPath(String passportPhotoPath) { this.passportPhotoPath = passportPhotoPath; }

    public List<ExperiencedDocumentDto> getExperiencedDocuments() { return experiencedDocuments; }
    public void setExperiencedDocuments(List<ExperiencedDocumentDto> experiencedDocuments) {
        this.experiencedDocuments = experiencedDocuments;
    }

    public LocalDate getFatherDateOfBirth() {
        return fatherDateOfBirth;
    }

    public void setFatherDateOfBirth(LocalDate fatherDateOfBirth) {
        this.fatherDateOfBirth = fatherDateOfBirth;
    }

    public LocalDate getMotherDateOfBirth() {
        return motherDateOfBirth;
    }

    public void setMotherDateOfBirth(LocalDate motherDateOfBirth) {
        this.motherDateOfBirth = motherDateOfBirth;
    }

    public String getFatherOccupation() {
        return fatherOccupation;
    }

    public void setFatherOccupation(String fatherOccupation) {
        this.fatherOccupation = fatherOccupation;
    }

    public String getMotherOccupation() {
        return motherOccupation;
    }

    public void setMotherOccupation(String motherOccupation) {
        this.motherOccupation = motherOccupation;
    }
}