package com.example.employeeLeaveApplication.feature.leave.carryforward.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CarryForwardLeaveRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Reason is required")
    private String reason;

    // Optional: half-day support
    private Boolean isHalfDay = false;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getIsHalfDay() { return isHalfDay; }
    public void setIsHalfDay(Boolean isHalfDay) { this.isHalfDay = isHalfDay; }
}