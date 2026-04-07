package com.emp_management.feature.workfromhome.entity;

import com.emp_management.shared.enums.ApprovalLevel;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wfh_reminder")
public class WfhReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long wfhRequestId;

    private int reminderCount;

    private LocalDateTime reminderSentAt;

    @Enumerated(EnumType.STRING)
    private ApprovalLevel approvalLevelAtReminder;

    // getters and setters

    public Long getWfhRequestId() {
        return wfhRequestId;
    }

    public void setWfhRequestId(Long wfhRequestId) {
        this.wfhRequestId = wfhRequestId;
    }

    public int getReminderCount() {
        return reminderCount;
    }

    public void setReminderCount(int reminderCount) {
        this.reminderCount = reminderCount;
    }

    public Long getId() {
        return id;
    }

    public ApprovalLevel getApprovalLevelAtReminder() {
        return approvalLevelAtReminder;
    }

    public void setApprovalLevelAtReminder(ApprovalLevel approvalLevelAtReminder) {
        this.approvalLevelAtReminder = approvalLevelAtReminder;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }
}
