package com.emp_management.feature.appraisal.entity;

import com.emp_management.shared.enums.AppraisalStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appraisal_response")
public class AppraisalResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id", nullable = false)
    private AppraisalDefinition definition;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppraisalStatus status = AppraisalStatus.DRAFT;

    @Column(name = "l1_reviewer_id")
    private String l1ReviewerId;

    @Column(name = "l2_reviewer_id")
    private String l2ReviewerId;

    @Column(name = "employee_overall_remarks", length = 2000)
    private String employeeOverallRemarks;

    @Column(name = "l1_overall_remarks", length = 2000)
    private String l1OverallRemarks;

    @Column(name = "l2_overall_remarks", length = 2000)
    private String l2OverallRemarks;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "auto_submitted_at")
    private LocalDateTime autoSubmittedAt;

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
    public AppraisalDefinition getDefinition() { return definition; }
    public void setDefinition(AppraisalDefinition definition) { this.definition = definition; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public AppraisalStatus getStatus() { return status; }
    public void setStatus(AppraisalStatus status) { this.status = status; }
    public String getL1ReviewerId() { return l1ReviewerId; }
    public void setL1ReviewerId(String l1ReviewerId) { this.l1ReviewerId = l1ReviewerId; }
    public String getL2ReviewerId() { return l2ReviewerId; }
    public void setL2ReviewerId(String l2ReviewerId) { this.l2ReviewerId = l2ReviewerId; }
    public String getEmployeeOverallRemarks() { return employeeOverallRemarks; }
    public void setEmployeeOverallRemarks(String employeeOverallRemarks) { this.employeeOverallRemarks = employeeOverallRemarks; }
    public String getL1OverallRemarks() { return l1OverallRemarks; }
    public void setL1OverallRemarks(String l1OverallRemarks) { this.l1OverallRemarks = l1OverallRemarks; }
    public String getL2OverallRemarks() { return l2OverallRemarks; }
    public void setL2OverallRemarks(String l2OverallRemarks) { this.l2OverallRemarks = l2OverallRemarks; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getAutoSubmittedAt() { return autoSubmittedAt; }
    public void setAutoSubmittedAt(LocalDateTime autoSubmittedAt) { this.autoSubmittedAt = autoSubmittedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}