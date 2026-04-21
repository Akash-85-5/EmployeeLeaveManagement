package com.emp_management.feature.leave.annual.dto;

import com.emp_management.feature.leave.carryforward.dto.CarryForwardBalanceResponse;

import java.util.List;

/**
 * Combined leave balance summary for a given employee and year.
 * Returned by GET /api/leave/balance/{employeeId}/summary
 */
public class LeaveBalanceSummaryResponse {

    private String employeeId;
    private Integer year;

    /** All initialized monthly records for ANNUAL leave this year. */
    private List<AnnualLeaveBalanceResponse> annualLeaveMonthly;

    /** All initialized monthly records for SICK leave this year. */
    private List<SickLeaveBalanceResponse> sickLeaveMonthly;

    /**
     * Carry-forward balance brought into this year from the previous year.
     * Null if no carry-forward record exists (e.g. first year of employment).
     */
    private CarryForwardBalanceResponse carryForward;

    public LeaveBalanceSummaryResponse() {}

    public LeaveBalanceSummaryResponse(String employeeId,
                                       Integer year,
                                       List<AnnualLeaveBalanceResponse> annualLeaveMonthly,
                                       List<SickLeaveBalanceResponse> sickLeaveMonthly,
                                       CarryForwardBalanceResponse carryForward) {
        this.employeeId         = employeeId;
        this.year               = year;
        this.annualLeaveMonthly = annualLeaveMonthly;
        this.sickLeaveMonthly   = sickLeaveMonthly;
        this.carryForward       = carryForward;
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public List<AnnualLeaveBalanceResponse> getAnnualLeaveMonthly() { return annualLeaveMonthly; }
    public void setAnnualLeaveMonthly(List<AnnualLeaveBalanceResponse> annualLeaveMonthly) {
        this.annualLeaveMonthly = annualLeaveMonthly;
    }

    public List<SickLeaveBalanceResponse> getSickLeaveMonthly() { return sickLeaveMonthly; }
    public void setSickLeaveMonthly(List<SickLeaveBalanceResponse> sickLeaveMonthly) {
        this.sickLeaveMonthly = sickLeaveMonthly;
    }

    public CarryForwardBalanceResponse getCarryForward() { return carryForward; }
    public void setCarryForward(CarryForwardBalanceResponse carryForward) {
        this.carryForward = carryForward;
    }
}