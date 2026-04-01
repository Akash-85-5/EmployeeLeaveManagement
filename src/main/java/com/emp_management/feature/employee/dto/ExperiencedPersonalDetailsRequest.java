package com.emp_management.feature.employee.dto;

import com.emp_management.shared.enums.BloodGroup;
import com.emp_management.shared.enums.Gender;
import com.emp_management.shared.enums.MaritalStatus;

import java.time.LocalDate;

public class ExperiencedPersonalDetailsRequest {

    // ── NEW name fields ───────────────────────────────────────────
    private String firstName;
    private String lastName;
    // ── EXISTING fields ───────────────────────────────────────────
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
    private String designation;
    private String skillSet;

    // ── NEW bank details ──────────────────────────────────────────
    private String accountNumber;
    private String bankName;

    // ── NEW: UNA number — REQUIRED for EXPERIENCED ────────────────
    private String unaNumber;

    // ── NOTE: pfNumber NOT included — Admin fills this later ──────

    // ── NEW: previous employment details ─────────────────────────
    private String previousRole;
    private String oldCompanyName;
    private LocalDate oldCompanyFromDate;
    private LocalDate oldCompanyEndDate;

    // ── EXISTING father details ───────────────────────────────────
    private String fatherName;
    private LocalDate fatherDateOfBirth;
    private String fatherOccupation;
    private Boolean fatherAlive;

    // ── EXISTING mother details ───────────────────────────────────
    private String motherName;
    private LocalDate motherDateOfBirth;
    private String motherOccupation;
    private Boolean motherAlive;

    // Getters & Setters


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

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
    public void setEmergencyContactNumber(String emergencyContactNumber) { this.emergencyContactNumber = emergencyContactNumber; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getSkillSet() { return skillSet; }
    public void setSkillSet(String skillSet) { this.skillSet = skillSet; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getUnaNumber() { return unaNumber; }
    public void setUnaNumber(String unaNumber) { this.unaNumber = unaNumber; }

    public String getPreviousRole() { return previousRole; }
    public void setPreviousRole(String previousRole) { this.previousRole = previousRole; }

    public String getOldCompanyName() { return oldCompanyName; }
    public void setOldCompanyName(String oldCompanyName) { this.oldCompanyName = oldCompanyName; }

    public LocalDate getOldCompanyFromDate() { return oldCompanyFromDate; }
    public void setOldCompanyFromDate(LocalDate oldCompanyFromDate) { this.oldCompanyFromDate = oldCompanyFromDate; }

    public LocalDate getOldCompanyEndDate() { return oldCompanyEndDate; }
    public void setOldCompanyEndDate(LocalDate oldCompanyEndDate) { this.oldCompanyEndDate = oldCompanyEndDate; }

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
}