package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.shared.enums.WfhType;
import java.time.LocalDate;

public class WorkFromHomeRequest {

    private WfhType type;

    private LocalDate startDate;

    private LocalDate endDate;

    private String reason;

    private String workPlan;

    // GETTERS

    public WfhType getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getReason() {
        return reason;
    }

    public String getWorkPlan() {
        return workPlan;
    }

    // SETTERS

    public void setType(WfhType type) {
        this.type = type;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setWorkPlan(String workPlan) {
        this.workPlan = workPlan;
    }
}