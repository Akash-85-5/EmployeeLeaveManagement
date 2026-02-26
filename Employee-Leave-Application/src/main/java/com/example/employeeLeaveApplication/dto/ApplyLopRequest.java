package com.example.employeeLeaveApplication.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ApplyLopRequest {

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "year is required")
    @Min(value = 2000, message = "year must be >= 2000")
    @Max(value = 9999, message = "year must be <= 9999")
    private Integer year;

    @NotNull(message = "month is required")
    @Min(value = 1, message = "month must be between 1 and 12")
    @Max(value = 12, message = "month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "excessDays is required")
    @Min(value = 0, message = "excessDays cannot be negative")
    private Double excessDays;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public Double getExcessDays() { return excessDays; }
    public void setExcessDays(Double excessDays) { this.excessDays = excessDays; }
}