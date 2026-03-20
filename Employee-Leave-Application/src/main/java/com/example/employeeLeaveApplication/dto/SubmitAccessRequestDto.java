package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAccessRequestDto {
    private LeaveType accessType; // VPN or BIOMETRIC
    private String reason;                 // Why they need this access
}