package com.example.employeeLeaveApplication.feature.payroll.dto;

/**
 * Admin uses this to set the PF number after employee profile is verified.
 * PUT /api/admin/employees/{employeeId}/pf-number
 */
public class PfUpdateRequest {

    private String pfNumber;

    public String getPfNumber() { return pfNumber; }
    public void setPfNumber(String pfNumber) { this.pfNumber = pfNumber; }
}