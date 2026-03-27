package com.example.employeeLeaveApplication.feature.noticePeriod.dto;

// ═══════════════════════════════════════════════════════════════
// 4. ABSCONDING REQUEST
// Fields: employeeId, initiatorId (from portal), reason
// ═══════════════════════════════════════════════════════════════

class AbscondingRequest {
    private Long   employeeId;
    private Long   initiatorId;
    private String reason;

    public Long   getEmployeeId()        { return employeeId; }
    public void   setEmployeeId(Long e)  { this.employeeId = e; }
    public Long   getInitiatorId()       { return initiatorId; }
    public void   setInitiatorId(Long i) { this.initiatorId = i; }
    public String getReason()            { return reason; }
    public void   setReason(String r)   { this.reason = r; }
}
