package com.emp_management.feature.appraisal.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "appraisal_definition_question")
public class AppraisalDefinitionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id", nullable = false)
    private AppraisalDefinition definition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AppraisalQuestionMaster question;

    @Column(name = "display_order")
    private Integer displayOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppraisalDefinition getDefinition() { return definition; }
    public void setDefinition(AppraisalDefinition definition) { this.definition = definition; }
    public AppraisalQuestionMaster getQuestion() { return question; }
    public void setQuestion(AppraisalQuestionMaster question) { this.question = question; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}