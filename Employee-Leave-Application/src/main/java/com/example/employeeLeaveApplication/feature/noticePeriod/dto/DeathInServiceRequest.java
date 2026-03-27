package com.example.employeeLeaveApplication.feature.noticePeriod.dto;

import java.time.LocalDate;


// ═══════════════════════════════════════════════════════════════
// 3. DEATH IN SERVICE REQUEST
// Fields: employeeId, initiatorId (from portal), deathDate
// ═══════════════════════════════════════════════════════════════

class DeathInServiceRequest {
    private Long      employeeId;
    private Long      initiatorId;
    private LocalDate deathDate;

    public Long      getEmployeeId()        { return employeeId; }
    public void      setEmployeeId(Long e)  { this.employeeId = e; }
    public Long      getInitiatorId()       { return initiatorId; }
    public void      setInitiatorId(Long i) { this.initiatorId = i; }
    public LocalDate getDeathDate()         { return deathDate; }
    public void      setDeathDate(LocalDate d) { this.deathDate = d; }
}
