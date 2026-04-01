package com.emp_management.feature.leave.compoff.entity;

import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "comp_off")
public class CompOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "reporting_id", nullable = false)
    private String reportingId;

    // The holiday/weekend the employee actually worked
    @Column(name = "worked_date", nullable = false)
    private LocalDate workedDate;

    /**
     * 1) Requesting for comp-off knowing wanted leave days
     * This field captures that "wanted" date.
     * Even if filled, status must remain PENDING until teammates approve.
     */
    // ✅ ADDED EXPLICIT COLUMN MAPPING
    @Column(name = "planned_leave_date")
    private LocalDate plannedLeaveDate;

    /**
     * Workflow Status:
     * - PENDING: Initial state for all requests.
     * - EARNED: Approved by teammates (Balance added).
     * - USED: Linked to a LeaveApplication and consumed.
     * - REJECTED: Denied by teammates.
     */
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    // The credit amount (1.0 for full day, 0.5 for half day)
    @Column(precision = 3, scale = 1)
    private BigDecimal days;

    /**
     * When the employee applies via the Leave Application dropdown,
     * this ID links this credit to that specific leave record.
     */
    @Column(name = "used_leave_application_id")
    private Long usedLeaveApplicationId;

    @Column(name = "year")
    private Integer year;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    public void setYearFromWorkedDate() {
        this.createdAt = LocalDateTime.now();
        if (this.workedDate != null) {
            this.year = this.workedDate.getYear();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }



    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    // Optional: Reason for working on a holiday
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportingId() {
        return reportingId;
    }

    public void setReportingId(String reportingId) {
        this.reportingId = reportingId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getWorkedDate() {
        return workedDate;
    }

    public void setWorkedDate(LocalDate workedDate) {
        this.workedDate = workedDate;
    }

    public LocalDate getPlannedLeaveDate() {
        return plannedLeaveDate;
    }

    public void setPlannedLeaveDate(LocalDate plannedLeaveDate) {
        this.plannedLeaveDate = plannedLeaveDate;
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

    public Long getUsedLeaveApplicationId() {
        return usedLeaveApplicationId;
    }

    public void setUsedLeaveApplicationId(Long usedLeaveApplicationId) {
        this.usedLeaveApplicationId = usedLeaveApplicationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
