package com.example.employeeLeaveApplication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerAccessDecisionDto {
    private String decision;
    private String remarks;
    private Long managerId;
}
