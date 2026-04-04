package com.emp_management.feature.employee.dto;

import java.time.LocalDate;

/**
 * Flat DTO for one experienced employee's document entry.
 * Used inside ProfileResponse to avoid circular reference
 * (ExperiencedDocument entity → personalDetails → employee → ...).
 */
public class ExperiencedDocumentDto {

    private Long id;

    // ── Experience details ────────────────────────────────────────
    private String companyName;
    private String role;
    private LocalDate fromDate;
    private LocalDate endDate;

    // ── File paths ────────────────────────────────────────────────
    private String experienceCertPath;
    private String relievingLetterPath;   // non-null only for last company entry
    private String idProofPath;           // non-null only on first entry
    private String passportPhotoPath;     // non-null only on first entry

    // ── Getters & Setters ─────────────────────────────────────────

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
    public void setExperienceCertPath(String experienceCertPath) { this.experienceCertPath = experienceCertPath; }

    public String getRelievingLetterPath() { return relievingLetterPath; }
    public void setRelievingLetterPath(String relievingLetterPath) { this.relievingLetterPath = relievingLetterPath; }

    public String getIdProofPath() { return idProofPath; }
    public void setIdProofPath(String idProofPath) { this.idProofPath = idProofPath; }

    public String getPassportPhotoPath() { return passportPhotoPath; }
    public void setPassportPhotoPath(String passportPhotoPath) { this.passportPhotoPath = passportPhotoPath; }
}