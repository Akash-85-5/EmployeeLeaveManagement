package com.example.employeeLeaveApplication.feature.employee.dto;

import com.example.employeeLeaveApplication.shared.enums.BloodGroup;
import com.example.employeeLeaveApplication.shared.enums.Gender;
import com.example.employeeLeaveApplication.shared.enums.MaritalStatus;
import java.time.LocalDate;

/**
 * Text fields only for FRESHER personal details submission.
 * Documents (aadhaarCard, tc, offerLetter) come as separate
 * MultipartFile parts in the same multipart/form-data request.
 *
 * Frontend sends:
 *   Content-Type: multipart/form-data
 *   - data         → this DTO serialized as JSON string
 *   - aadhaarCard  → file (PDF/JPG/PNG)
 *   - tc           → file (PDF/JPG/PNG)
 *   - offerLetter  → file (PDF/JPG/PNG)
 */
public class FresherPersonalDetailsRequest {

    // ── NEW name fields ───────────────────────────────────────────
    private String firstName;
    private String lastName;
    private String surName;

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

    // ── NOTE: unaNumber NOT included — FRESHER does not need it ──
    // ── NOTE: pfNumber NOT included — Admin fills this later ──────

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

    public String getSurName() { return surName; }
    public void setSurName(String surName) { this.surName = surName; }

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