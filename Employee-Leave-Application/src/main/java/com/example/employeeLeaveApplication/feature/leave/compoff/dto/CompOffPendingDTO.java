package com.example.employeeLeaveApplication.feature.leave.compoff.dto;

import com.example.employeeLeaveApplication.shared.enums.CompOffStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CompOffPendingDTO {
    private Long compoffId;
    private Long employeeId;
    private String employeeName;
    private LocalDate workedDate;
    private CompOffStatus status;
    private BigDecimal days;
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCompoffId() {
        return compoffId;
    }

    public void setCompoffId(Long compoffId) {
        this.compoffId = compoffId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LocalDate getWorkedDate() {
        return workedDate;
    }

    public void setWorkedDate(LocalDate workedDate) {
        this.workedDate = workedDate;
    }

    public CompOffStatus getStatus() {
        return status;
    }

    public void setStatus(CompOffStatus status) {
        this.status = status;
    }

    public BigDecimal getDays() {
        return days;
    }

    public void setDays(BigDecimal days) {
        this.days = days;
    }
}
