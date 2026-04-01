package com.emp_management.feature.leave.annual.dto;

import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.entity.LeaveAttachment;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaveApplicationWithAttachmentsDto {

    private LeaveApplication leaveApplication;
    private List<LeaveAttachment> attachments;
    private int attachmentCount;

    // ── Constructors ──────────────────────────────────────────────

    public LeaveApplicationWithAttachmentsDto() {}

    public LeaveApplicationWithAttachmentsDto(LeaveApplication leaveApplication,
                                              List<LeaveAttachment> attachments) {
        this.leaveApplication = leaveApplication;
        this.attachments = attachments != null ? attachments : List.of();
        this.attachmentCount = this.attachments.size();
    }

    // ── Getters & Setters ──────────────────────────────────────────

    public LeaveApplication getLeaveApplication() {
        return leaveApplication;
    }

    public void setLeaveApplication(LeaveApplication leaveApplication) {
        this.leaveApplication = leaveApplication;
    }

    public List<LeaveAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<LeaveAttachment> attachments) {
        this.attachments = attachments != null ? attachments : List.of();
        this.attachmentCount = this.attachments.size();
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(int attachmentCount) {
        this.attachmentCount = attachmentCount;
    }
}