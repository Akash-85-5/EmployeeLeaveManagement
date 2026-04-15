package com.emp_management.feature.leave.annual.dto;

public class MonthlyLeaveBalanceDTO {

    private AnnualLeaveBalanceResponse annualLeaveBalance;
    private SickLeaveBalanceResponse sickLeaveBalance;

    public MonthlyLeaveBalanceDTO(AnnualLeaveBalanceResponse annualLeaveBalance, SickLeaveBalanceResponse sickLeaveBalance) {
        this.annualLeaveBalance = annualLeaveBalance;
        this.sickLeaveBalance = sickLeaveBalance;
    }

    public AnnualLeaveBalanceResponse getAnnualLeaveBalance() {
        return annualLeaveBalance;
    }

    public void setAnnualLeaveBalance(AnnualLeaveBalanceResponse annualLeaveBalance) {
        this.annualLeaveBalance = annualLeaveBalance;
    }

    public SickLeaveBalanceResponse getSickLeaveBalance() {
        return sickLeaveBalance;
    }

    public void setSickLeaveBalance(SickLeaveBalanceResponse sickLeaveBalance) {
        this.sickLeaveBalance = sickLeaveBalance;
    }
}
