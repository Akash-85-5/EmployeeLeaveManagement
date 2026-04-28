package com.emp_management.feature.appraisal.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "appraisal_definition")
public class AppraisalDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appraisal_year", nullable = false, length = 10)
    private String appraisalYear;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date", nullable = false)
    private LocalDate closeDate;

    @Column(name = "default_l1_reviewer_id")
    private String defaultL1ReviewerId;

    @Column(name = "default_l2_reviewer_id")
    private String defaultL2ReviewerId;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Column(name = "auto_submit_enabled", nullable = false)
    private Boolean autoSubmitEnabled = true;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAppraisalYear() { return appraisalYear; }
    public void setAppraisalYear(String appraisalYear) { this.appraisalYear = appraisalYear; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }
    public String getDefaultL1ReviewerId() { return defaultL1ReviewerId; }
    public void setDefaultL1ReviewerId(String defaultL1ReviewerId) { this.defaultL1ReviewerId = defaultL1ReviewerId; }
    public String getDefaultL2ReviewerId() { return defaultL2ReviewerId; }
    public void setDefaultL2ReviewerId(String defaultL2ReviewerId) { this.defaultL2ReviewerId = defaultL2ReviewerId; }
    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }
    public Boolean getAutoSubmitEnabled() { return autoSubmitEnabled; }
    public void setAutoSubmitEnabled(Boolean autoSubmitEnabled) { this.autoSubmitEnabled = autoSubmitEnabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}