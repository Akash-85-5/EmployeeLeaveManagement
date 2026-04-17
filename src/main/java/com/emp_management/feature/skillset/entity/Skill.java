package com.emp_management.feature.skillset.entity;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.enums.SkillCategory;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "skill")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Employee FK ────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ── Core skill fields ──────────────────────────────────────────────────
    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private SkillCategory category;          // TECHNICAL | TOOLS | INTERPERSONAL

    @Column(name = "rating")
    private Integer rating;                  // 1-5 star proficiency

    // ── Learn / Apply provenance ───────────────────────────────────────────
    @Column(name = "learned_at")
    private String learnedAt;               // e.g. "Udemy", "Coursera"

    @Column(name = "applied_at")
    private String appliedAt;               // e.g. "Leave App", "CI/CD Pipeline"

    // ── Dates ─────────────────────────────────────────────────────────────
    @Column(name = "date_learned")
    private LocalDate dateLearned;

    @Column(name = "cert_date")
    private LocalDate certDate;

    // ── Proof / certificate file (stored on disk) ──────────────────────────
    @Column(name = "proof_file_path")
    private String proofFilePath;           // Absolute or relative path on server

    @Column(name = "proof_file_name")
    private String proofFileName;           // Original filename shown in UI

    // ── Audit ──────────────────────────────────────────────────────────────
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public SkillCategory getCategory() { return category; }
    public void setCategory(SkillCategory category) { this.category = category; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getLearnedAt() { return learnedAt; }
    public void setLearnedAt(String learnedAt) { this.learnedAt = learnedAt; }

    public String getAppliedAt() { return appliedAt; }
    public void setAppliedAt(String appliedAt) { this.appliedAt = appliedAt; }

    public LocalDate getDateLearned() { return dateLearned; }
    public void setDateLearned(LocalDate dateLearned) { this.dateLearned = dateLearned; }

    public LocalDate getCertDate() { return certDate; }
    public void setCertDate(LocalDate certDate) { this.certDate = certDate; }

    public String getProofFilePath() { return proofFilePath; }
    public void setProofFilePath(String proofFilePath) { this.proofFilePath = proofFilePath; }

    public String getProofFileName() { return proofFileName; }
    public void setProofFileName(String proofFileName) { this.proofFileName = proofFileName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}