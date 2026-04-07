package com.emp_management.feature.employee.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Each row = one experience entry for an EXPERIENCED employee.
 * An employee can have 1..N experience entries.
 *
 * Each entry carries:
 *  - experience certificate file path
 *  - company name
 *  - from/end dates
 *  - role held
 *
 * The last company's relieving letter is stored in a separate
 * column on this record — the frontend marks one entry as
 * "last company" by including the relieving letter file.
 * All other entries will have relievingLetterPath = null.
 */
@Entity
@Table(name = "experienced_document")
public class ExperiencedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many experience entries → one personal details record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_details_id", nullable = false)
    private EmployeePersonalDetails personalDetails;

    // ── Mandatory for every experience entry ──────────────────────

    @Column(name = "company_name", length = 200, nullable = false)
    private String companyName;

    @Column(name = "role", length = 100, nullable = false)
    private String role;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Stored path for the experience certificate of this company. */
    @Column(name = "experience_cert_path", nullable = false)
    private String experienceCertPath;

    // ── Only the last company carries a relieving letter ──────────
    /** Null for all entries except the most recent employer. */
    @Column(name = "relieving_letter_path")
    private String relievingLetterPath;

    // ── ID Proof & Passport Photo — stored on the FIRST entry only ─
    /**
     * The employee uploads their ID proof once.
     * Stored on the first ExperiencedDocument entry; null on all others.
     */
    @Column(name = "id_proof_path")
    private String idProofPath;

    /**
     * Passport-size photo — uploaded once per employee.
     * Stored on the first ExperiencedDocument entry; null on all others.
     */
    @Column(name = "passport_photo_path")
    private String passportPhotoPath;

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmployeePersonalDetails getPersonalDetails() { return personalDetails; }
    public void setPersonalDetails(EmployeePersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getExperienceCertPath() { return experienceCertPath; }
    public void setExperienceCertPath(String experienceCertPath) {
        this.experienceCertPath = experienceCertPath;
    }

    public String getRelievingLetterPath() { return relievingLetterPath; }
    public void setRelievingLetterPath(String relievingLetterPath) {
        this.relievingLetterPath = relievingLetterPath;
    }

    public String getIdProofPath() { return idProofPath; }
    public void setIdProofPath(String idProofPath) { this.idProofPath = idProofPath; }

    public String getPassportPhotoPath() { return passportPhotoPath; }
    public void setPassportPhotoPath(String passportPhotoPath) { this.passportPhotoPath = passportPhotoPath; }
}