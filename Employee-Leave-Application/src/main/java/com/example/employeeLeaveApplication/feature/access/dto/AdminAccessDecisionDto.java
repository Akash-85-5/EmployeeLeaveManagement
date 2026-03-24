package com.example.employeeLeaveApplication.feature.access.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccessDecisionDto {
    private String decision;    // "APPROVED" or "REJECTED"
    private String remarks;     // Admin's remarks
}
