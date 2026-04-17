package com.emp_management.feature.skillset.dto;

import com.emp_management.shared.enums.SkillCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Payload accepted by POST /api/skillset and PUT /api/skillset/{id}.
 *
 * Matches every field editable in the frontend EditModal (Myskills.tsx):
 *   skillName, category (type), rating (proficiency stars),
 *   learnedAt, appliedAt, dateLearned, certDate.
 *
 * The proof file is handled as a separate MultipartFile parameter in the
 * controller, not inside this DTO.
 */
public class SkillRequestDTO {

    @NotBlank(message = "Skill name is required")
    private String skillName;

    @NotNull(message = "Category is required")
    private SkillCategory category;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    private String learnedAt;              // "Udemy", "Coursera", etc.
    private String appliedAt;              // "Leave App", "CI/CD Pipeline", etc.
    private LocalDate dateLearned;
    private LocalDate certDate;

    // ── Getters & Setters ──────────────────────────────────────────────────

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
}