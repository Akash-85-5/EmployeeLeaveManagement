package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.WfhStatus;
import com.example.employeeLeaveApplication.enums.WfhType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_from_home")
public class WorkFromHome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    private String employeeName;

    @Enumerated(EnumType.STRING)
    private WfhType type;

    private LocalDate startDate;

    private LocalDate endDate;

    private String reason;

    private String workPlan;

    @Enumerated(EnumType.STRING)
    private WfhStatus status;

    private LocalDateTime appliedAt = LocalDateTime.now();


    // GETTERS

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

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

    public WfhStatus getStatus() {
        return status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }


    // SETTERS

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

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

    public void setStatus(WfhStatus status) {
        this.status = status;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}