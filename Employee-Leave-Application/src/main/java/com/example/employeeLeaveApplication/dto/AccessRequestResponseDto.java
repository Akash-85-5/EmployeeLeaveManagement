package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.LeaveType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessRequestResponseDto {
    private Long id;
    private Long employeeId;
    private LeaveType accessType;          // VPN or BIOMETRIC
    private String status;              // Current status
    private String reason;              // Employee's reason
    private LocalDateTime submittedAt;

    // Manager decision info (visible after manager reviews)
    private String managerDecision;
    private String managerRemarks;
    private LocalDateTime managerDecisionAt;

    // Admin decision info (visible after admin reviews)
    private String adminDecision;
    private String adminRemarks;
    private LocalDateTime adminDecisionAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
