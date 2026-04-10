package com.emp_management.feature.leave.annual.dto;

import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.entity.LeaveAttachment;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaveApplicationWithAttachmentsDto {

    private LeaveApplicationResponseDTO leaveApplicationResponseDTO;
    private List<LeaveAttachment> attachments;
    private int attachmentCount;
    private List<LeaveRemarkDto> remarks;

    // ── Constructors ──────────────────────────────────────────────

    public LeaveApplicationWithAttachmentsDto() {}

    public LeaveApplicationWithAttachmentsDto(LeaveApplicationResponseDTO leaveApplicationResponseDTO,
                                              List<LeaveAttachment> attachments) {
        this.leaveApplicationResponseDTO = leaveApplicationResponseDTO;
        this.attachments = attachments != null ? attachments : List.of();
        this.attachmentCount = this.attachments.size();
    }

    // ── Getters & Setters ──────────────────────────────────────────

    public List<LeaveRemarkDto> getRemarks() {
        return remarks;
    }

    public void setRemarks(List<LeaveRemarkDto> remarks) {
        this.remarks = remarks;
    }
    public List<LeaveAttachment> getAttachments() {
        return attachments;
    }

    public LeaveApplicationResponseDTO getLeaveApplicationResponseDTO() {
        return leaveApplicationResponseDTO;
    }

    public void setLeaveApplicationResponseDTO(LeaveApplicationResponseDTO leaveApplicationResponseDTO) {
        this.leaveApplicationResponseDTO = leaveApplicationResponseDTO;
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