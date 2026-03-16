package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.BloodGroup;
import com.example.employeeLeaveApplication.enums.Gender;
import com.example.employeeLeaveApplication.enums.MaritalStatus;
import java.time.LocalDate;

public class PersonalDetailsRequest {

    private String firstName;
    private String lastName;
    private String contactNumber;
    private Gender gender;
    private String aadharNumber;
    private String personalEmail;
    private LocalDate dateOfBirth;
    private String presentAddress;
    private String permanentAddress;
    private BloodGroup bloodGroup;
    private MaritalStatus maritalStatus;
    private String emergencyContactNumber;
    private String designation;
    private String skillSet;

    // Father
    private String fatherName;
    private LocalDate fatherDateOfBirth;
    private String fatherOccupation;
    private Boolean fatherAlive;

    // Mother
    private String motherName;
    private LocalDate motherDateOfBirth;
    private String motherOccupation;
    private Boolean motherAlive;

    // Getters & Setters
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

    public MaritalStatus getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getEmergencyContactNumber() { return emergencyContactNumber; }
    public void setEmergencyContactNumber(String emergencyContactNumber) { this.emergencyContactNumber = emergencyContactNumber; }

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
}