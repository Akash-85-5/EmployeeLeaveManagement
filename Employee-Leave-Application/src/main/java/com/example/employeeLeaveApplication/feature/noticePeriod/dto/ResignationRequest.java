package com.example.employeeLeaveApplication.feature.noticePeriod.dto;


// ═══════════════════════════════════════════════════════════════
// 1. RESIGNATION REQUEST
// Fields: employeeId (from portal), reason
// ═══════════════════════════════════════════════════════════════
public class ResignationRequest {
    private Long   employeeId;
    private String reason;

    public Long   getEmployeeId() { return employeeId; }
    public void   setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getReason()     { return reason; }
    public void   setReason(String reason) { this.reason = reason; }
}