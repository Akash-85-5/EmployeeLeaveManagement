package com.emp_management.feature.employee.dto;

import com.emp_management.shared.enums.BloodGroup;
import com.emp_management.shared.enums.Gender;
import com.emp_management.shared.enums.MaritalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * JSON part of the experienced personal-details multipart request.
 *
 * Multipart files expected alongside this JSON:
 *   - idProof                        (mandatory, single file)
 *   - experienceCerts                (mandatory, List<MultipartFile> — one per experience entry)
 *   - relievingLetter                (mandatory, single file for the last company)
 *
 * The "experiences" list in this DTO must match the order of
 * files in "experienceCerts" by index.
 * The entry with isLastCompany = true determines which entry
 * gets the relievingLetterPath stored.
 */
public class ExperiencedPersonalDetailsRequest {

    // ── Name ──────────────────────────────────────────────────────
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    // ── Contact ───────────────────────────────────────────────────
    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String contactNumber;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Marital status is required")
    private MaritalStatus maritalStatus;

    @NotBlank(message = "Aadhar number is required")
    private String aadharNumber;

    @NotBlank(message = "Personal email is required")
    @Email(message = "Enter a valid email address")
    private String personalEmail;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Present address is required")
    private String presentAddress;

    @NotBlank(message = "Permanent address is required")
    private String permanentAddress;

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotBlank(message = "Emergency contact number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit emergency contact number")
    private String emergencyContactNumber;

    @NotBlank(message = "Designation is required")
    private String designation;

    private String skillSet;

    // ── Bank details ──────────────────────────────────────────────
    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Enter a valid IFSC code")
    private String ifscCode;

    @NotBlank(message = "Bank branch name is required")
    private String bankBranchName;

    // ── UAN — required for EXPERIENCED ───────────────────────────
    @NotBlank(message = "UAN number is required for experienced employees")
    private String uanNumber;

    // ── Father details ────────────────────────────────────────────
    private String fatherName;
    private LocalDate fatherDateOfBirth;
    private String fatherOccupation;
    private Boolean fatherAlive;

    // ── Mother details ────────────────────────────────────────────
    private String motherName;
    private LocalDate motherDateOfBirth;
    private String motherOccupation;
    private Boolean motherAlive;

    // ── Spouse details (mandatory when maritalStatus = MARRIED) ──
    private String spouseName;
    private Integer spouseAge;

    @Pattern(regexp = "^([6-9]\\d{9})?$", message = "Enter a valid 10-digit spouse contact number")
    private String spouseContactNumber;

    // ── Children (optional, dynamic) ──────────────────────────────
    @Valid
    private List<ChildDto> children;

    // ── Experience entries (dynamic, 1..N) ────────────────────────
    @NotNull(message = "At least one experience entry is required")
    @Size(min = 1, message = "At least one experience entry is required")
    @Valid
    private List<ExperienceEntryDto> experiences;

    // ── Getters & Setters ─────────────────────────────────────────

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

    public String getAadharNumber() {
        return aadharNumber;
    }

    public void setAadharNumber(String aadharNumber) {
        this.aadharNumber = aadharNumber;
    }

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
    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getSkillSet() { return skillSet; }
    public void setSkillSet(String skillSet) { this.skillSet = skillSet; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBankBranchName() { return bankBranchName; }
    public void setBankBranchName(String bankBranchName) { this.bankBranchName = bankBranchName; }

    public String getUanNumber() { return uanNumber; }
    public void setUanNumber(String uanNumber) { this.uanNumber = uanNumber; }

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

    public Integer getSpouseAge() { return spouseAge; }
    public void setSpouseAge(Integer spouseAge) { this.spouseAge = spouseAge; }

    public String getSpouseContactNumber() { return spouseContactNumber; }
    public void setSpouseContactNumber(String spouseContactNumber) {
        this.spouseContactNumber = spouseContactNumber;
    }

    public List<ChildDto> getChildren() { return children; }
    public void setChildren(List<ChildDto> children) { this.children = children; }

    public List<ExperienceEntryDto> getExperiences() { return experiences; }
    public void setExperiences(List<ExperienceEntryDto> experiences) { this.experiences = experiences; }
}