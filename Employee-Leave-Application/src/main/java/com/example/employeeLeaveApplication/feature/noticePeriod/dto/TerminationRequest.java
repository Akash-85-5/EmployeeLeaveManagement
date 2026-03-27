package com.example.employeeLeaveApplication.feature.noticePeriod.dto;

import com.example.employeeLeaveApplication.shared.enums.TerminationGrounds;
import com.example.employeeLeaveApplication.shared.enums.TerminationType;


// ═══════════════════════════════════════════════════════════════
// 2. TERMINATION REQUEST
// Fields: employeeId, initiatorId (from portal), terminationType,
//         terminationGrounds (only if WITHOUT_NOTICE_PERIOD), reason
// ═══════════════════════════════════════════════════════════════

class TerminationRequest {
    private Long               employeeId;
    private Long               initiatorId;
    private TerminationType    terminationType;

    // Only required when terminationType = WITHOUT_NOTICE_PERIOD.
    // Tells the system WHY no notice period is allowed.
    // PROBATION / INTERNSHIP / MISCONDUCT
    private TerminationGrounds terminationGrounds;

    // Free text: the actual reason typed by the manager / HR
    // e.g. "Found sharing confidential client data externally"
    private String             reason;

    public Long               getEmployeeId()         { return employeeId; }
    public void               setEmployeeId(Long e)   { this.employeeId = e; }
    public Long               getInitiatorId()        { return initiatorId; }
    public void               setInitiatorId(Long i)  { this.initiatorId = i; }
    public TerminationType    getTerminationType()    { return terminationType; }
    public void               setTerminationType(TerminationType t) { this.terminationType = t; }
    public TerminationGrounds getTerminationGrounds() { return terminationGrounds; }
    public void               setTerminationGrounds(TerminationGrounds g) { this.terminationGrounds = g; }
    public String             getReason()             { return reason; }
    public void               setReason(String r)    { this.reason = r; }
}
