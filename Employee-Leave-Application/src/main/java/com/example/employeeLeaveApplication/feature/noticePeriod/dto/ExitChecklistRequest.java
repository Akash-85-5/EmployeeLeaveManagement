package com.example.employeeLeaveApplication.feature.noticePeriod.dto;

// ═══════════════════════════════════════════════════════════════
// 5. EXIT CHECKLIST REQUEST (Admin fills at end of notice period)
// Fields: adminId, laptopReturned, idCardReturned, ktCompleted
// ═══════════════════════════════════════════════════════════════
class ExitChecklistRequest {
    private Long    adminId;
    private boolean laptopReturned;
    private boolean idCardReturned;
    private boolean ktCompleted;

    public Long    getAdminId()               { return adminId; }
    public void    setAdminId(Long a)         { this.adminId = a; }
    public boolean isLaptopReturned()         { return laptopReturned; }
    public void    setLaptopReturned(boolean b) { this.laptopReturned = b; }
    public boolean isIdCardReturned()         { return idCardReturned; }
    public void    setIdCardReturned(boolean b) { this.idCardReturned = b; }
    public boolean isKtCompleted()            { return ktCompleted; }
    public void    setKtCompleted(boolean b)  { this.ktCompleted = b; }
}