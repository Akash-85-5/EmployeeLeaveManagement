package com.emp_management.feature.employee.dto;

import java.time.LocalDate;

/**
 * Flat DTO for one experienced employee document entry.
 * No back-reference to EmployeePersonalDetails — prevents circular JSON.
 */
public class ExperiencedDocumentDto {

    private Long id;
    private String companyName;
    private String role;
    private LocalDate fromDate;
    private LocalDate endDate;

    private String experienceCertPath;
    private String joiningLetterPath;
    private String relievingLetterPath;
    private String idProofPath;
    private String passportPhotoPath;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getExperienceCertPath() { return experienceCertPath; }
    public void setExperienceCertPath(String p) { this.experienceCertPath = p; }

    public String getJoiningLetterPath() { return joiningLetterPath; }
    public void setJoiningLetterPath(String p) { this.joiningLetterPath = p; }

    public String getRelievingLetterPath() { return relievingLetterPath; }
    public void setRelievingLetterPath(String p) { this.relievingLetterPath = p; }

    public String getIdProofPath() { return idProofPath; }
    public void setIdProofPath(String p) { this.idProofPath = p; }

    public String getPassportPhotoPath() { return passportPhotoPath; }
    public void setPassportPhotoPath(String p) { this.passportPhotoPath = p; }
}