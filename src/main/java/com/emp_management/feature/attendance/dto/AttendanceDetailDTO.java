package com.emp_management.feature.attendance.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceDetailDTO {

    private String employeeId;
    private String employeeName;
    private LocalDate date;
    private String status;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private Double workingHours;
    private boolean lopTriggered;

    // 🔹 GETTERS & SETTERS

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalTime checkIn) {
        this.checkIn = checkIn;
    }

    public LocalTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalTime checkOut) {
        this.checkOut = checkOut;
    }

    public Double getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(Double workingHours) {
        this.workingHours = workingHours;
    }

    public boolean isLopTriggered() {
        return lopTriggered;
    }

    public void setLopTriggered(boolean lopTriggered) {
        this.lopTriggered = lopTriggered;
    }
}