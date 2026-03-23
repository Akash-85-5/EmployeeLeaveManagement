package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.AccessRequestStatus;
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
public class AccessRequestForAdminDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String employeeDesignation;
    private LeaveType accessType;
    private AccessRequestStatus status;
    private String reason;
    private LocalDateTime submittedAt;

    // Manager decision
    private String managerDecision;
    private String managerRemarks;
    private LocalDateTime managerDecisionAt;
    private String managerName;

    // For admin action
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
