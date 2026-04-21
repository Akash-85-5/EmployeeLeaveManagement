package com.emp_management.feature.leave.compoff.dto;

import com.emp_management.shared.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CompOffPendingDTO {
    private Long compoffId;
    private String employeeId;
    private String employeeName;
    private LocalDate workedDate;
    private RequestStatus status;
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

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String  employeeId) {
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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public BigDecimal getDays() {
        return days;
    }

    public void setDays(BigDecimal days) {
        this.days = days;
    }
}
