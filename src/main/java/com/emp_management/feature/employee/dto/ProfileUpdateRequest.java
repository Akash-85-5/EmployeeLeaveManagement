package com.emp_management.feature.employee.dto;

import com.emp_management.shared.enums.BloodGroup;
import com.emp_management.shared.enums.Gender;
import com.emp_management.shared.enums.MaritalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/**
 * General profile PUT request.
 * Covers both Employee-table fields and EmployeePersonalDetails fields.
 * ALL fields are optional — only non-null values are applied.
 *
 * Employee-table fields (editable):
 *   name, reportingId, departmentId, roleId, branchId
 *
 * PersonalDetails fields:
 *   all personal, bank, spouse, children fields
 *
 * Experience entries (for EXPERIENCED employees):
 *   null  → entries untouched
 *   present → full replacement with optional files
 */
public class ProfileUpdateRequest {

    // ── Employee table fields ─────────────────────────────────────
    private String name;
    private LocalDate joiningDate;
    // ── Name ──────────────────────────────────────────────────────
    private String firstName;
    private String lastName;

    // ── Contact ───────────────────────────────────────────────────
    @Pattern(regexp = "^([6-9]\\d{9})?$", message = "Enter a valid 10-digit Indian mobile number")
    private String contactNumber;

    private Gender gender;
    private MaritalStatus maritalStatus;
    private String aadharNumber;

    @Email(message = "Enter a valid email address")
    private String personalEmail;

    private LocalDate dateOfBirth;
    private String presentAddress;
    private String permanentAddress;
    private BloodGroup bloodGroup;

    @Pattern(regexp = "^([6-9]\\d{9})?$", message = "Enter a valid 10-digit emergency contact number")
    private String emergencyContactNumber;

    private String designation;
    private String skillSet;

    // ── Bank details ──────────────────────────────────────────────
    private String accountNumber;
    private String bankName;

    @Pattern(regexp = "^([A-Z]{4}0[A-Z0-9]{6})?$", message = "Enter a valid IFSC code")
    private String ifscCode;

    private String bankBranchName;

    // ── UAN (experienced only) ────────────────────────────────────
    private String uanNumber;
    private String pfNumber;

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

    // ── Spouse details ────────────────────────────────────────────
    private String spouseName;
    private LocalDate spouseDateOfBirth;
    private String spouseOccupation;

    @Pattern(regexp = "^([6-9]\\d{9})?$", message = "Enter a valid 10-digit spouse contact number")
    private String spouseContactNumber;

    // ── Children ──────────────────────────────────────────────────
    /** null → unchanged. Non-null (even empty) → fully replaces list. */
    @Valid
    private List<ChildDto> children;

    // ── Experience entries (experienced only) ─────────────────────
    /** null → unchanged. Non-null → full replacement. */
    @Valid
    private List<ExperienceEntryDto> experiences;

    // ── Getters & Setters ─────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }


    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

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
    public void setFatherDateOfBirth(LocalDate d) { this.fatherDateOfBirth = d; }

    public String getFatherOccupation() { return fatherOccupation; }
    public void setFatherOccupation(String fatherOccupation) { this.fatherOccupation = fatherOccupation; }

    public Boolean getFatherAlive() { return fatherAlive; }
    public void setFatherAlive(Boolean fatherAlive) { this.fatherAlive = fatherAlive; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public LocalDate getMotherDateOfBirth() { return motherDateOfBirth; }
    public void setMotherDateOfBirth(LocalDate d) { this.motherDateOfBirth = d; }

    public String getMotherOccupation() { return motherOccupation; }
    public void setMotherOccupation(String motherOccupation) { this.motherOccupation = motherOccupation; }

    public Boolean getMotherAlive() { return motherAlive; }
    public void setMotherAlive(Boolean motherAlive) { this.motherAlive = motherAlive; }

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

    public List<ExperienceEntryDto> getExperiences() { return experiences; }
    public void setExperiences(List<ExperienceEntryDto> experiences) { this.experiences = experiences; }

    public String getPfNumber() {
        return pfNumber;
    }

    public void setPfNumber(String pfNumber) {
        this.pfNumber = pfNumber;
    }
}