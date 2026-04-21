package com.emp_management.feature.employee.entity;

import com.emp_management.shared.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core personal details for every employee (fresher + experienced).
 *
 * Document attachments are stored in separate tables:
 *   - FresherDocument     (one-to-one)
 *   - ExperiencedDocument (one-to-many)
 *
 * Children are stored in EmployeeChild (one-to-many).
 */
@Entity
@Table(name = "employee_personal_details")
public class EmployeePersonalDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    // ── HR verification ───────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "hr_remarks", columnDefinition = "TEXT")
    private String hrRemarks;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // ── Lock / submission ─────────────────────────────────────────
    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // ── Name ──────────────────────────────────────────────────────
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Column(name = "first_name", length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    @Column(name = "last_name", length = 100)
    private String lastName;

    // ── Contact ───────────────────────────────────────────────────
    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    @Column(name = "contact_number", length = 15)
    private String contactNumber;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @NotNull(message = "Marital status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Personal email is required")
    @Email(message = "Enter a valid email address")
    @Column(name = "personal_email")
    private String personalEmail;

    @NotBlank(message = "Present address is required")
    @Size(max = 500)
    @Column(name = "present_address", length = 500)
    private String presentAddress;

    @NotBlank(message = "Permanent address is required")
    @Size(max = 500)
    @Column(name = "permanent_address", length = 500)
    private String permanentAddress;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group")
    private BloodGroup bloodGroup;

    @NotBlank(message = "Emergency contact number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit emergency contact number")
    @Column(name = "emergency_contact_number", length = 15)
    private String emergencyContactNumber;

    // ── ID proof number (replaces aadharNumber) ───────────────────
    /**
     * The ID proof number the employee enters manually
     * (e.g. Aadhaar, PAN, Passport number).
     * Actual document file is stored in FresherDocument / ExperiencedDocument.
     */
    @NotBlank(message = "ID proof number is required")
    @Column(name = "aadhar_number", length = 12)
    private String aadharNumber;

    // ── Bank details ──────────────────────────────────────────────
    @NotBlank(message = "Account number is required")
    @Size(max = 30)
    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100)
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Enter a valid IFSC code (e.g. SBIN0001234)")
    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    @NotBlank(message = "Bank branch name is required")
    @Size(max = 150)
    @Column(name = "bank_branch_name", length = 150)
    private String bankBranchName;

    // ── PF / UAN — admin / experienced ───────────────────────────
    /** Filled by Admin only after verification. */
    @Column(name = "pf_number", length = 50)
    private String pfNumber;

    /** Required for EXPERIENCED employees; null for FRESHER. */
    @Column(name = "uan_number", length = 50)
    private String uanNumber;

    // ── Professional info ─────────────────────────────────────────
    @NotBlank(message = "Designation is required")
    @Size(max = 100)
    @Column(name = "designation", length = 100)
    private String designation;

    /** Comma-separated list stored as a single string. */
    @Column(name = "skill_set", length = 1000)
    private String skillSet;

    // ── Father details ────────────────────────────────────────────
    @Column(name = "father_name", length = 100)
    private String fatherName;

    @Column(name = "father_date_of_birth")
    private LocalDate fatherDateOfBirth;

    @Column(name = "father_occupation", length = 100)
    private String fatherOccupation;

    @Column(name = "father_alive")
    private Boolean fatherAlive;

    // ── Mother details ────────────────────────────────────────────
    @Column(name = "mother_name", length = 100)
    private String motherName;

    @Column(name = "mother_date_of_birth")
    private LocalDate motherDateOfBirth;

    @Column(name = "mother_occupation", length = 100)
    private String motherOccupation;

    @Column(name = "mother_alive")
    private Boolean motherAlive;

    // ── Spouse details (mandatory when maritalStatus = MARRIED) ───
    @Column(name = "spouse_name", length = 100)
    private String spouseName;

    @Column(name = "spouse_date_of_birth")
    private LocalDate spouseDateOfBirth;

    @Column(name = "spouse_occupation", length = 100)
    private String spouseOccupation;

    @Column(name = "spouse_contact_number", length = 15)
    private String spouseContactNumber;
    @Column(name = "birthday_email_sent_date")
    private LocalDate birthEmailSentDate;

    // ── Children (one-to-many) ────────────────────────────────────
    /**
     * CascadeType.ALL + orphanRemoval = true means:
     *  - saving this entity saves children
     *  - deleting a child from the list deletes the DB row
     * The service always replaces the list on resubmit by clearing
     * and re-adding, so orphanRemoval handles the cleanup.
     */
    @OneToMany(mappedBy = "personalDetails",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<EmployeeChild> children = new ArrayList<>();

    // ── Document relations ────────────────────────────────────────
    @OneToOne(mappedBy = "personalDetails",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private FresherDocument fresherDocument;

    @OneToMany(mappedBy = "personalDetails",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ExperiencedDocument> experiencedDocuments = new ArrayList<>();

    // ── Audit ─────────────────────────────────────────────────────
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

    public LocalDate getBirthEmailSentDate() {
        return birthEmailSentDate;
    }

    public void setBirthEmailSentDate(LocalDate birthEmailSentDate) {
        this.birthEmailSentDate = birthEmailSentDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus v) { this.verificationStatus = v; }

    public String getHrRemarks() { return hrRemarks; }
    public void setHrRemarks(String hrRemarks) { this.hrRemarks = hrRemarks; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

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
    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public String getAadharNumber() {
        return aadharNumber;
    }
    public void setAadharNumber(String aadharNumber) {
        this.aadharNumber = aadharNumber;
    }

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

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getSkillSet() { return skillSet; }
    public void setSkillSet(String skillSet) { this.skillSet = skillSet; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public LocalDate getFatherDateOfBirth() { return fatherDateOfBirth; }
    public void setFatherDateOfBirth(LocalDate fatherDateOfBirth) { this.fatherDateOfBirth = fatherDateOfBirth; }

    public String getFatherOccupation() { return fatherOccupation; }
    public void setFatherOccupation(String fatherOccupation) { this.fatherOccupation = fatherOccupation; }

    public Boolean getFatherAlive() { return fatherAlive; }
    public void setFatherAlive(Boolean fatherAlive) { this.fatherAlive = fatherAlive; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public LocalDate getMotherDateOfBirth() { return motherDateOfBirth; }
    public void setMotherDateOfBirth(LocalDate motherDateOfBirth) { this.motherDateOfBirth = motherDateOfBirth; }

    public String getMotherOccupation() { return motherOccupation; }
    public void setMotherOccupation(String motherOccupation) { this.motherOccupation = motherOccupation; }

    public Boolean getMotherAlive() { return motherAlive; }
    public void setMotherAlive(Boolean motherAlive) { this.motherAlive = motherAlive; }

    public String getSpouseName() { return spouseName; }
    public void setSpouseName(String spouseName) { this.spouseName = spouseName; }

    public LocalDate getSpouseDateOfBirth() {
        return spouseDateOfBirth;
    }

    public void setSpouseDateOfBirth(LocalDate spouseDateOfBirth) {
        this.spouseDateOfBirth = spouseDateOfBirth;
    }

    public String getSpouseOccupation() {
        return spouseOccupation;
    }

    public void setSpouseOccupation(String spouseOccupation) {
        this.spouseOccupation = spouseOccupation;
    }

    public String getSpouseContactNumber() { return spouseContactNumber; }
    public void setSpouseContactNumber(String spouseContactNumber) {
        this.spouseContactNumber = spouseContactNumber;
    }

    public List<EmployeeChild> getChildren() { return children; }
    public void setChildren(List<EmployeeChild> children) { this.children = children; }

    public FresherDocument getFresherDocument() { return fresherDocument; }
    public void setFresherDocument(FresherDocument fresherDocument) {
        this.fresherDocument = fresherDocument;
    }

    public List<ExperiencedDocument> getExperiencedDocuments() { return experiencedDocuments; }
    public void setExperiencedDocuments(List<ExperiencedDocument> experiencedDocuments) {
        this.experiencedDocuments = experiencedDocuments;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}