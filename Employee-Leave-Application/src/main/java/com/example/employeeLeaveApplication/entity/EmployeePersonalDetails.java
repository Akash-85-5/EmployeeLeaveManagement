package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_personal_details")
public class EmployeePersonalDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private Long employeeId;

    // ── NEW: FRESHER or EXPERIENCED ───────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false)
    private EmployeeType employeeType;

    // ── NEW: HR verification ──────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "hr_remarks", columnDefinition = "TEXT")
    private String hrRemarks;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // ── UNCHANGED: lock / submission ──────────────────────────────
    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // ── NEW: name fields ──────────────────────────────────────────
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "sur_name", length = 100)
    private String surName;

    // ── UNCHANGED: basic personal info ───────────────────────────
    @Column(name = "contact_number", length = 15)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "personal_email")
    private String personalEmail;

    @Column(name = "present_address", length = 500)
    private String presentAddress;

    @Column(name = "permanent_address", length = 500)
    private String permanentAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group")
    private BloodGroup bloodGroup;

    @Column(name = "emergency_contact_number", length = 15)
    private String emergencyContactNumber;

    // ── UNCHANGED: aadhaar number ─────────────────────────────────
    @Column(name = "aadhar_number", length = 12)
    private String aadharNumber;

    // ── NEW: aadhaar document — stored as file path ───────────────
    @Column(name = "aadhaar_doc_path")
    private String aadhaarDocPath;

    // ── NEW: bank details ─────────────────────────────────────────
    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    // ── NEW: PF number — filled by Admin only, NOT by employee ────
    @Column(name = "pf_number", length = 50)
    private String pfNumber;

    // ── NEW: UNA number — required for EXPERIENCED, null for FRESHER
    @Column(name = "una_number", length = 50)
    private String unaNumber;

    // ── UNCHANGED: professional info ─────────────────────────────
    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "skill_set", length = 1000)
    private String skillSet;

    // ── UNCHANGED: father details ─────────────────────────────────
    @Column(name = "father_name", length = 100)
    private String fatherName;

    @Column(name = "father_date_of_birth")
    private LocalDate fatherDateOfBirth;

    @Column(name = "father_occupation", length = 100)
    private String fatherOccupation;

    @Column(name = "father_alive")
    private Boolean fatherAlive;

    // ── UNCHANGED: mother details ─────────────────────────────────
    @Column(name = "mother_name", length = 100)
    private String motherName;

    @Column(name = "mother_date_of_birth")
    private LocalDate motherDateOfBirth;

    @Column(name = "mother_occupation", length = 100)
    private String motherOccupation;

    @Column(name = "mother_alive")
    private Boolean motherAlive;

    // ── NEW: FRESHER-only document paths ──────────────────────────
    @Column(name = "tc_doc_path")
    private String tcDocPath;

    @Column(name = "offer_letter_doc_path")
    private String offerLetterDocPath;

    // ── NEW: EXPERIENCED-only fields ──────────────────────────────
    @Column(name = "experience_cert_doc_path")
    private String experienceCertDocPath;

    @Column(name = "leaving_letter_doc_path")
    private String leavingLetterDocPath;

    @Column(name = "previous_role", length = 100)
    private String previousRole;

    @Column(name = "old_company_name", length = 200)
    private String oldCompanyName;

    @Column(name = "old_company_from_date")
    private LocalDate oldCompanyFromDate;

    @Column(name = "old_company_end_date")
    private LocalDate oldCompanyEndDate;

    // ── UNCHANGED: audit ──────────────────────────────────────────
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

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public EmployeeType getEmployeeType() { return employeeType; }
    public void setEmployeeType(EmployeeType employeeType) { this.employeeType = employeeType; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getHrRemarks() { return hrRemarks; }
    public void setHrRemarks(String hrRemarks) { this.hrRemarks = hrRemarks; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSurName() { return surName; }
    public void setSurName(String surName) { this.surName = surName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public MaritalStatus getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getPersonalEmail() { return personalEmail; }
    public void setPersonalEmail(String personalEmail) { this.personalEmail = personalEmail; }

    public String getPresentAddress() { return presentAddress; }
    public void setPresentAddress(String presentAddress) { this.presentAddress = presentAddress; }

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getEmergencyContactNumber() { return emergencyContactNumber; }
    public void setEmergencyContactNumber(String s) { this.emergencyContactNumber = s; }

    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }

    public String getAadhaarDocPath() { return aadhaarDocPath; }
    public void setAadhaarDocPath(String aadhaarDocPath) { this.aadhaarDocPath = aadhaarDocPath; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getPfNumber() { return pfNumber; }
    public void setPfNumber(String pfNumber) { this.pfNumber = pfNumber; }

    public String getUnaNumber() { return unaNumber; }
    public void setUnaNumber(String unaNumber) { this.unaNumber = unaNumber; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getSkillSet() { return skillSet; }
    public void setSkillSet(String skillSet) { this.skillSet = skillSet; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public LocalDate getFatherDateOfBirth() { return fatherDateOfBirth; }
    public void setFatherDateOfBirth(LocalDate d) { this.fatherDateOfBirth = d; }

    public String getFatherOccupation() { return fatherOccupation; }
    public void setFatherOccupation(String s) { this.fatherOccupation = s; }

    public Boolean getFatherAlive() { return fatherAlive; }
    public void setFatherAlive(Boolean fatherAlive) { this.fatherAlive = fatherAlive; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public LocalDate getMotherDateOfBirth() { return motherDateOfBirth; }
    public void setMotherDateOfBirth(LocalDate d) { this.motherDateOfBirth = d; }

    public String getMotherOccupation() { return motherOccupation; }
    public void setMotherOccupation(String s) { this.motherOccupation = s; }

    public Boolean getMotherAlive() { return motherAlive; }
    public void setMotherAlive(Boolean motherAlive) { this.motherAlive = motherAlive; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}