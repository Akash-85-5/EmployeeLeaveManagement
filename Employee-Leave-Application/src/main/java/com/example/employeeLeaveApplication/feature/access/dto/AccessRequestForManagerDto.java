package com.example.employeeLeaveApplication.feature.access.dto;

import com.example.employeeLeaveApplication.shared.enums.AccessRequestStatus;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessRequestForManagerDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private LeaveType accessType;
    private AccessRequestStatus status;
    private String reason;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
