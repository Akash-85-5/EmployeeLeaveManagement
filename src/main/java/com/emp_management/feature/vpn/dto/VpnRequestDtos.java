package com.emp_management.feature.vpn.dto;

import com.emp_management.shared.enums.VpnRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ═══════════════════════════════════════════════════════════════════════════════
//  REQUEST DTOs
// ═══════════════════════════════════════════════════════════════════════════════

public class VpnRequestDtos {

    public static class ApplyRequest {

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        @NotBlank(message = "Purpose is required")
        private String purpose;

        // getters & setters

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }
    }

    public static class ActionRequest {

        private boolean approved;
        private String remarks;

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }

    public static class VpnRequestResponse {

        private Long id;
        private String applicantId;
        private String applicantName;
        private String applicantRole;

        private String managerApproverId;
        private String managerApproverName;

        private String purpose;

        private VpnRequestStatus status;
        private String statusLabel;

        private String managerRemarks;
        private LocalDateTime managerActionedAt;

        private String adminId;
        private String adminName;
        private String adminRemarks;
        private LocalDateTime adminActionedAt;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private LocalDate startDate;
        private LocalDate endDate;

        // 🔹 Generate ALL getters & setters
        // (I’ll show a few, rest same pattern)

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getApplicantId() { return applicantId; }
        public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

        public String getApplicantRole() { return applicantRole; }
        public void setApplicantRole(String applicantRole) { this.applicantRole = applicantRole; }

        public String getManagerApproverId() { return managerApproverId; }
        public void setManagerApproverId(String managerApproverId) { this.managerApproverId = managerApproverId; }

        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }

        public VpnRequestStatus getStatus() { return status; }
        public void setStatus(VpnRequestStatus status) { this.status = status; }

        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

        public String getManagerRemarks() { return managerRemarks; }
        public void setManagerRemarks(String managerRemarks) { this.managerRemarks = managerRemarks; }

        public LocalDateTime getManagerActionedAt() { return managerActionedAt; }
        public void setManagerActionedAt(LocalDateTime managerActionedAt) { this.managerActionedAt = managerActionedAt; }

        public String getAdminId() { return adminId; }
        public void setAdminId(String adminId) { this.adminId = adminId; }

        public String getAdminRemarks() { return adminRemarks; }
        public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

        public LocalDateTime getAdminActionedAt() { return adminActionedAt; }
        public void setAdminActionedAt(LocalDateTime adminActionedAt) { this.adminActionedAt = adminActionedAt; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public String getApplicantName() {
            return applicantName;
        }

        public void setApplicantName(String applicantName) {
            this.applicantName = applicantName;
        }

        public String getManagerApproverName() {
            return managerApproverName;
        }

        public void setManagerApproverName(String managerApproverName) {
            this.managerApproverName = managerApproverName;
        }

        public String getAdminName() {
            return adminName;
        }

        public void setAdminName(String adminName) {
            this.adminName = adminName;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
    }

}