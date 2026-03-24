package com.example.employeeLeaveApplication.feature.leave.annual.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "annual_leave_monthly_balance", uniqueConstraints = @UniqueConstraint(
        columnNames = {"employee_id", "year", "month"}))
public class AnnualLeaveMonthlyBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "year", nullable = false)
    private Integer year;

    /**
     * Month number 1–12.
     * This record represents the CUMULATIVE balance at this month.
     * i.e., month=3 means the balance available to use IN March
     * (which includes any unused days from Jan + Feb + March accrual).
     */
    @Column(name = "month", nullable = false)
    private Integer month;

    /**
     * Cumulative days available to take leave in this month.
     * = previous month's remaining + ANNUAL_LEAVE_PER_MONTH (2.0)
     * + any carry-forward days added at start of year (month=1 only).
     */
    @Column(name = "available_days", nullable = false)
    private Double availableDays = 0.0;

    /**
     * Days actually used (approved leaves) in this month.
     */
    @Column(name = "used_days", nullable = false)
    private Double usedDays = 0.0;

    /**
     * availableDays - usedDays.
     * This is the balance that rolls over to next month.
     */
    @Column(name = "remaining_days", nullable = false)
    private Double remainingDays = 0.0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Double getAvailableDays() { return availableDays; }
    public void setAvailableDays(Double availableDays) { this.availableDays = availableDays; }

    public Double getUsedDays() { return usedDays; }
    public void setUsedDays(Double usedDays) { this.usedDays = usedDays; }

    public Double getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Double remainingDays) { this.remainingDays = remainingDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}