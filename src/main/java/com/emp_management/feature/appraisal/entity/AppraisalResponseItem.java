package com.emp_management.feature.appraisal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appraisal_response_item")
public class AppraisalResponseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private AppraisalResponse response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AppraisalQuestionMaster question;

    @Column(name = "employee_self_rating")
    private Integer employeeSelfRating;

    @Column(name = "employee_remarks", length = 2000)
    private String employeeRemarks;

    @Column(name = "l1_revised_rating")
    private Integer l1RevisedRating;

    @Column(name = "l1_revised_remarks", length = 2000)
    private String l1RevisedRemarks;

    @Column(name = "l2_final_rating")
    private Integer l2FinalRating;

    @Column(name = "l2_final_remarks", length = 2000)
    private String l2FinalRemarks;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppraisalResponse getResponse() { return response; }
    public void setResponse(AppraisalResponse response) { this.response = response; }
    public AppraisalQuestionMaster getQuestion() { return question; }
    public void setQuestion(AppraisalQuestionMaster question) { this.question = question; }
    public Integer getEmployeeSelfRating() { return employeeSelfRating; }
    public void setEmployeeSelfRating(Integer employeeSelfRating) { this.employeeSelfRating = employeeSelfRating; }
    public String getEmployeeRemarks() { return employeeRemarks; }
    public void setEmployeeRemarks(String employeeRemarks) { this.employeeRemarks = employeeRemarks; }
    public Integer getL1RevisedRating() { return l1RevisedRating; }
    public void setL1RevisedRating(Integer l1RevisedRating) { this.l1RevisedRating = l1RevisedRating; }
    public String getL1RevisedRemarks() { return l1RevisedRemarks; }
    public void setL1RevisedRemarks(String l1RevisedRemarks) { this.l1RevisedRemarks = l1RevisedRemarks; }
    public Integer getL2FinalRating() { return l2FinalRating; }
    public void setL2FinalRating(Integer l2FinalRating) { this.l2FinalRating = l2FinalRating; }
    public String getL2FinalRemarks() { return l2FinalRemarks; }
    public void setL2FinalRemarks(String l2FinalRemarks) { this.l2FinalRemarks = l2FinalRemarks; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}