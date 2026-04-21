package com.emp_management.feature.notification.entity;

import com.emp_management.shared.enums.ApprovalLevel;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_reminder")
public class LeaveReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leave_application_id", nullable = false)
    private Long leaveApplicationId;

    @Column(name = "reminder_sent_at", nullable = false)
    private LocalDateTime reminderSentAt;

    @Column(name = "reminder_count", nullable = false)
    private Integer reminderCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level_at_reminder")
    private ApprovalLevel approvalLevelAtReminder;

    // getter + setter
    public ApprovalLevel getApprovalLevelAtReminder() { return approvalLevelAtReminder; }
    public void setApprovalLevelAtReminder(ApprovalLevel approvalLevelAtReminder) {
        this.approvalLevelAtReminder = approvalLevelAtReminder;
    }

    @PrePersist
    protected void onCreate() {
        this.reminderSentAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLeaveApplicationId() {
        return leaveApplicationId;
    }

    public void setLeaveApplicationId(Long leaveApplicationId) {
        this.leaveApplicationId = leaveApplicationId;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }

    public Integer getReminderCount() {
        return reminderCount;
    }

    public void setReminderCount(Integer reminderCount) {
        this.reminderCount = reminderCount;
    }
}
