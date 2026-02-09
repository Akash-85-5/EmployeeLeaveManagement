package com.example.notificationservice.dto;

public class DashboardResponse {
    private LeaveSummary annualLeave;
    private LeaveSummary sickLeave;
    private LeaveSummary casualLeave;
    private LeaveSummary compOff;

    public LeaveSummary getAnnualLeave() {
        return annualLeave;
    }

    public void setAnnualLeave(LeaveSummary annualLeave) {
        this.annualLeave = annualLeave;
    }

    public LeaveSummary getSickLeave() {
        return sickLeave;
    }

    public void setSickLeave(LeaveSummary sickLeave) {
        this.sickLeave = sickLeave;
    }

    public LeaveSummary getCasualLeave() {
        return casualLeave;
    }

    public void setCasualLeave(LeaveSummary casualLeave) {
        this.casualLeave = casualLeave;
    }

    public LeaveSummary getCompOff() {
        return compOff;
    }

    public void setCompOff(LeaveSummary compOff) {
        this.compOff = compOff;
    }
}
