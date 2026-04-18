package com.emp_management.feature.attendance.dto;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public class AttendanceExportRequest {
    @NotEmpty(message = "Employee list cannot be empty")
    private List<String> empIds;
    private LocalDate fromDate;
    private LocalDate toDate;

    // Getters and Setters
    public List<String> getEmpIds() {
        return empIds;
    }

    public void setEmpIds(List<String> empIds) {
        this.empIds = empIds;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
}