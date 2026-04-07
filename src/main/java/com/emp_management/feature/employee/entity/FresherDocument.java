package com.emp_management.feature.employee.entity;

import jakarta.persistence.*;

/**
 * Stores the six fixed document paths uploaded by a FRESHER employee.
 * One-to-one with EmployeePersonalDetails.
 *
 * Documents:
 *  1. idProof               (mandatory)
 *  2. tenthMarksheet        (mandatory)
 *  3. twelfthMarksheet      (mandatory)
 *  4. degreeCertificate     (mandatory — degree OR provisional)
 *  5. offerLetter           (mandatory)
 *  6. passportSizePhoto     (mandatory)
 */
@Entity
@Table(name = "fresher_document")
public class FresherDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_details_id", nullable = false, unique = true)
    private EmployeePersonalDetails personalDetails;

    // ── 1. ID Proof ───────────────────────────────────────────────
    /** Stored file path for ID proof (Aadhaar / PAN / Passport etc.) */
    @Column(name = "id_proof_path")
    private String idProofPath;

    // ── 2. 10th Marksheet ─────────────────────────────────────────
    @Column(name = "tenth_marksheet_path")
    private String tenthMarksheetPath;

    // ── 3. 12th Marksheet ─────────────────────────────────────────
    @Column(name = "twelfth_marksheet_path")
    private String twelfthMarksheetPath;

    // ── 4. Degree / Provisional Certificate ───────────────────────
    @Column(name = "degree_certificate_path")
    private String degreeCertificatePath;

    // ── 5. Offer Letter ───────────────────────────────────────────
    @Column(name = "offer_letter_path")
    private String offerLetterPath;

    // ── 6. Passport-Size Photo ────────────────────────────────────
    @Column(name = "passport_photo_path")
    private String passportPhotoPath;

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmployeePersonalDetails getPersonalDetails() { return personalDetails; }
    public void setPersonalDetails(EmployeePersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public String getIdProofPath() { return idProofPath; }
    public void setIdProofPath(String idProofPath) { this.idProofPath = idProofPath; }

    public String getTenthMarksheetPath() { return tenthMarksheetPath; }
    public void setTenthMarksheetPath(String tenthMarksheetPath) {
        this.tenthMarksheetPath = tenthMarksheetPath;
    }

    public String getTwelfthMarksheetPath() { return twelfthMarksheetPath; }
    public void setTwelfthMarksheetPath(String twelfthMarksheetPath) {
        this.twelfthMarksheetPath = twelfthMarksheetPath;
    }

    public String getDegreeCertificatePath() { return degreeCertificatePath; }
    public void setDegreeCertificatePath(String degreeCertificatePath) {
        this.degreeCertificatePath = degreeCertificatePath;
    }

    public String getOfferLetterPath() { return offerLetterPath; }
    public void setOfferLetterPath(String offerLetterPath) {
        this.offerLetterPath = offerLetterPath;
    }

    public String getPassportPhotoPath() { return passportPhotoPath; }
    public void setPassportPhotoPath(String passportPhotoPath) {
        this.passportPhotoPath = passportPhotoPath;
    }
}