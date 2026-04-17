package com.emp_management.feature.skillset.dto;

import com.emp_management.feature.skillset.entity.Skill;
import com.emp_management.shared.enums.SkillCategory;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response shape that satisfies BOTH frontend Skill interfaces:
 *
 * Myskills.tsx  →  { id, name, type, rating, learn, apply, dateAdded,
 *                    dateLearned, certDate, modified, file }
 *
 * ManagerTeamSkills.tsx → { skillName, category, stars, certifiedDate,
 *                           learnedAt, appliedAt, certLink }
 *
 * We expose every field so the frontend can pick what it needs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillResponseDTO {

    // ── Identity ───────────────────────────────────────────────────────────
    private Long id;

    // ── Employee info (for manager/HR views) ──────────────────────────────
    private String empId;
    private String employeeName;
    private String department;

    // ── Skill fields ───────────────────────────────────────────────────────
    private String skillName;               // primary field
    private String name;                    // alias for Myskills.tsx compatibility
    private SkillCategory category;         // enum value
    private String type;                    // alias: matches frontend "type" field
    private String categoryLabel;           // "Technical" | "Tools" | "Platforms" | "Interpersonal"
    private Integer rating;                 // primary field
    private Integer stars;                  // alias for ManagerTeamSkills.tsx

    // ── Provenance ─────────────────────────────────────────────────────────
    private String learnedAt;               // primary field
    private String learn;                   // alias for Myskills.tsx
    private String appliedAt;               // primary field
    private String apply;                   // alias for Myskills.tsx

    // ── Dates ─────────────────────────────────────────────────────────────
    private LocalDate dateAdded;            // = createdAt.toLocalDate()
    private LocalDate dateLearned;
    private LocalDate certDate;
    private LocalDate certifiedDate;        // alias for ManagerTeamSkills.tsx
    private LocalDateTime updatedAt;
    private String modified;                // alias for Myskills.tsx (formatted string)

    // ── Proof file ─────────────────────────────────────────────────────────
    private String proofFileName;           // shown in UI: "SpringCert.pdf"
    private String proofFileUrl;            // download URL: /api/skillset/{id}/file
    private String file;                    // alias for Myskills.tsx
    private String certLink;                // alias for ManagerTeamSkills.tsx

    // ── Static factory ─────────────────────────────────────────────────────

    public static SkillResponseDTO from(Skill s) {
        SkillResponseDTO dto = new SkillResponseDTO();

        // Identity
        dto.id = s.getId();
        dto.empId = s.getEmployee().getEmpId();
        dto.employeeName = s.getEmployee().getName();
        dto.department = s.getEmployee().getDepartment() != null
                ? s.getEmployee().getDepartment().getDepartmentName()
                : null;

        // Skill fields - set both primary and alias
        dto.skillName = s.getSkillName();
        dto.name = s.getSkillName();  // alias

        dto.category = s.getCategory();
        dto.type = toCategoryLabel(s.getCategory());  // alias
        dto.categoryLabel = toCategoryLabel(s.getCategory());

        dto.rating = s.getRating();
        dto.stars = s.getRating();  // alias

        // Provenance - set both primary and alias
        dto.learnedAt = s.getLearnedAt();
        dto.learn = s.getLearnedAt();  // alias

        dto.appliedAt = s.getAppliedAt();
        dto.apply = s.getAppliedAt();  // alias

        // Dates
        dto.dateAdded = s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate() : null;
        dto.dateLearned = s.getDateLearned();

        dto.certDate = s.getCertDate();
        dto.certifiedDate = s.getCertDate();  // alias

        dto.updatedAt = s.getUpdatedAt();
        dto.modified = s.getUpdatedAt() != null
                ? s.getUpdatedAt().toString()
                : null;  // alias - frontend can parse or format

        // Proof file - set both primary and aliases
        dto.proofFileName = s.getProofFileName();
        String fileUrl = s.getProofFilePath() != null
                ? "/api/skillset/" + s.getId() + "/file"
                : null;
        dto.proofFileUrl = fileUrl;
        dto.file = fileUrl;       // alias for Myskills.tsx
        dto.certLink = fileUrl;   // alias for ManagerTeamSkills.tsx

        return dto;
    }

    private static String toCategoryLabel(SkillCategory cat) {
        if (cat == null) return null;
        return switch (cat) {
            case TECHNICAL     -> "Technical";
            case TOOLS         -> "Tools & Platforms";
            case INTERPERSONAL -> "Interpersonal";
        };
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SkillCategory getCategory() { return category; }
    public void setCategory(SkillCategory category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }

    public String getLearnedAt() { return learnedAt; }
    public void setLearnedAt(String learnedAt) { this.learnedAt = learnedAt; }

    public String getLearn() { return learn; }
    public void setLearn(String learn) { this.learn = learn; }

    public String getAppliedAt() { return appliedAt; }
    public void setAppliedAt(String appliedAt) { this.appliedAt = appliedAt; }

    public String getApply() { return apply; }
    public void setApply(String apply) { this.apply = apply; }

    public LocalDate getDateAdded() { return dateAdded; }
    public void setDateAdded(LocalDate dateAdded) { this.dateAdded = dateAdded; }

    public LocalDate getDateLearned() { return dateLearned; }
    public void setDateLearned(LocalDate dateLearned) { this.dateLearned = dateLearned; }

    public LocalDate getCertDate() { return certDate; }
    public void setCertDate(LocalDate certDate) { this.certDate = certDate; }

    public LocalDate getCertifiedDate() { return certifiedDate; }
    public void setCertifiedDate(LocalDate certifiedDate) { this.certifiedDate = certifiedDate; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getModified() { return modified; }
    public void setModified(String modified) { this.modified = modified; }

    public String getProofFileName() { return proofFileName; }
    public void setProofFileName(String proofFileName) { this.proofFileName = proofFileName; }

    public String getProofFileUrl() { return proofFileUrl; }
    public void setProofFileUrl(String proofFileUrl) { this.proofFileUrl = proofFileUrl; }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public String getCertLink() { return certLink; }
    public void setCertLink(String certLink) { this.certLink = certLink; }
}