package com.emp_management.feature.appraisal.entity;

import com.emp_management.shared.enums.AppraisalReviewLevel;
import com.emp_management.shared.enums.AppraisalStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appraisal_review_log")
public class AppraisalReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_level")
    private AppraisalReviewLevel reviewLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private AppraisalStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private AppraisalStatus toStatus;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    @PrePersist
    protected void onCreate() { this.actedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getResponseId() { return responseId; }
    public void setResponseId(Long responseId) { this.responseId = responseId; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public AppraisalReviewLevel getReviewLevel() { return reviewLevel; }
    public void setReviewLevel(AppraisalReviewLevel reviewLevel) { this.reviewLevel = reviewLevel; }
    public AppraisalStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(AppraisalStatus fromStatus) { this.fromStatus = fromStatus; }
    public AppraisalStatus getToStatus() { return toStatus; }
    public void setToStatus(AppraisalStatus toStatus) { this.toStatus = toStatus; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getActedAt() { return actedAt; }
}