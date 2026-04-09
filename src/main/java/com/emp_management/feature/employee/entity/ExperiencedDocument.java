package com.emp_management.feature.employee.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Each row = one experience entry for an EXPERIENCED employee.
 *
 * Documents per entry:
 *   - experienceCertPath   (mandatory on POST, optional on PUT)
 *   - joiningLetterPath    (optional — letter issued when employee joined that company)
 *   - relievingLetterPath  (only for the last company entry)
 *
 * idProofPath and passportPhotoPath are employee-level documents stored
 * on the FIRST entry only; null on all subsequent entries.
 */
@Entity
@Table(name = "experienced_document")
public class ExperiencedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_details_id", nullable = false)
    private EmployeePersonalDetails personalDetails;

    // ── Per-entry fields ──────────────────────────────────────────
    @Column(name = "company_name", length = 200, nullable = false)
    private String companyName;

    @Column(name = "role", length = 100, nullable = false)
    private String role;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "experience_cert_path", nullable = false)
    private String experienceCertPath;

    /** Joining letter for this company (optional). */
    @Column(name = "joining_letter_path")
    private String joiningLetterPath;

    /** Relieving letter — only on the last-company entry; null on all others. */
    @Column(name = "relieving_letter_path")
    private String relievingLetterPath;

    // ── Employee-level docs — first entry only ────────────────────
    /** ID proof file — stored on the first entry; null on all others. */
    @Column(name = "id_proof_path")
    private String idProofPath;

    /** Passport-size photo — stored on the first entry; null on all others. */
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

    public String getJoiningLetterPath() { return joiningLetterPath; }
    public void setJoiningLetterPath(String joiningLetterPath) {
        this.joiningLetterPath = joiningLetterPath;
    }

    public String getRelievingLetterPath() { return relievingLetterPath; }
    public void setRelievingLetterPath(String relievingLetterPath) {
        this.relievingLetterPath = relievingLetterPath;
    }

    public String getIdProofPath() { return idProofPath; }
    public void setIdProofPath(String idProofPath) { this.idProofPath = idProofPath; }

    public String getPassportPhotoPath() { return passportPhotoPath; }
    public void setPassportPhotoPath(String passportPhotoPath) {
        this.passportPhotoPath = passportPhotoPath;
    }
}