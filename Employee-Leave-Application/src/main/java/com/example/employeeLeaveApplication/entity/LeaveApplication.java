// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveApplication.java (WITH COMPLETE AUDIT TRAIL)
// Location: src/main/java/com/example/notificationservice/entity/
// ═══════════════════════════════════════════════════════════════════

package com.example.employeeLeaveApplication.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.employeeLeaveApplication.enums.HalfDayType;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.enums.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "leave_application")
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    private HalfDayType halfDayType;

    @Column(name = "leave_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private BigDecimal days;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    // ═══════════════════════════════════════════════════════════════
    // APPROVAL AUDIT FIELDS
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "approved_by")
    private Long approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "approved_role")
    private Role approvedRole;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ═══════════════════════════════════════════════════════════════
    // DEDUCTION TRACKING
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "carry_forward_used")
    private Double carryForwardUsed = 0.0;

    @Column(name = "comp_off_used")
    private Double compOffUsed = 0.0;

    @Column(name = "loss_of_pay_applied")
    private Double lossOfPayApplied = 0.0;

    // ═══════════════════════════════════════════════════════════════
    // TIMESTAMP AUDIT
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "ESCALATED")
    private Boolean escalated = false;
    private LocalDateTime escalatedAt;

    @NotNull
    @Column(name = "manager_id", nullable = false)
    private Long managerId;

//     ═══════════════════════════════════════════════════════════════
//     OPTIMISTIC LOCKING
//     ═══════════════════════════════════════════════════════════════

    @Version
    @Column(name = "version")
    private Long version;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.startDate != null) {
            this.year = this.startDate.getYear();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.startDate != null) {
            this.year = this.startDate.getYear();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════════

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Boolean getEscalated() {
        return escalated;
    }

    public void setEscalated(Boolean escalated) {
        this.escalated = escalated;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public HalfDayType getHalfDayType() {
        return halfDayType;
    }

    public void setHalfDayType(HalfDayType halfDayType) {
        this.halfDayType = halfDayType;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getDays() {
        return days;
    }

    public void setDays(BigDecimal days) {
        this.days = days;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Role getApprovedRole() {
        return approvedRole;
    }

    public void setApprovedRole(Role approvedRole) {
        this.approvedRole = approvedRole;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Double getCarryForwardUsed() {
        return carryForwardUsed;
    }

    public void setCarryForwardUsed(Double carryForwardUsed) {
        this.carryForwardUsed = carryForwardUsed;
    }

    public Double getCompOffUsed() {
        return compOffUsed;
    }

    public void setCompOffUsed(Double compOffUsed) {
        this.compOffUsed = compOffUsed;
    }

    public Double getLossOfPayApplied() {
        return lossOfPayApplied;
    }

    public void setLossOfPayApplied(Double lossOfPayApplied) {
        this.lossOfPayApplied = lossOfPayApplied;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }


}