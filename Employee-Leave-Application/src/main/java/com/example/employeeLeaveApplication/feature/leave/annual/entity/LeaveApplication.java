package com.example.employeeLeaveApplication.feature.leave.annual.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.employeeLeaveApplication.shared.enums.ApprovalLevel;
import com.example.employeeLeaveApplication.shared.enums.HalfDayType;
import com.example.employeeLeaveApplication.shared.enums.LeaveStatus;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import com.example.employeeLeaveApplication.shared.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "leave_application")
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Deprecated
    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_type")
    private HalfDayType halfDayType;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_date_half_day_type")
    private HalfDayType startDateHalfDayType;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_date_half_day_type")
    private HalfDayType endDateHalfDayType;

    @Column(name = "is_appointment")
    private Boolean isAppointment = false;

    @Column(name = "leave_year", nullable = false)
    private Integer year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private BigDecimal days;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_approval_level")
    private ApprovalLevel currentApprovalLevel;

    @Column(name = "required_approval_levels")
    private Integer requiredApprovalLevels;

    @Column(name = "current_approver_id")
    private Long currentApproverId;

    @Column(name = "first_approver_id")
    private Long firstApproverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_approver_decision")
    private LeaveStatus firstApproverDecision;

    @Column(name = "first_approver_decided_at")
    private LocalDateTime firstApproverDecidedAt;

    @Column(name = "second_approver_id")
    private Long secondApproverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "second_approver_decision")
    private LeaveStatus secondApproverDecision;

    @Column(name = "second_approver_decided_at")
    private LocalDateTime secondApproverDecidedAt;


    @Column(name = "approved_by")
    private Long approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "approved_role")
    private Role approvedRole;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "carry_forward_used")
    private Double carryForwardUsed = 0.0;

    @Column(name = "comp_off_used")
    private Double compOffUsed = 0.0;

    @Column(name = "loss_of_pay_applied")
    private Double lossOfPayApplied = 0.0;

    @Column(name = "pending_lop_days")
    private Double pendingLopDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "escalated")
    private Boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

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

    // ── Getters & Setters ──────────────────────────────────────────

    public Long getCurrentApproverId() {
        return currentApproverId;
    }

    public void setCurrentApproverId(Long currentApproverId) {
        this.currentApproverId = currentApproverId;
    }

    public LeaveStatus getSecondApproverDecision() {
        return secondApproverDecision;
    }

    public void setSecondApproverDecision(LeaveStatus secondApproverDecision) {
        this.secondApproverDecision = secondApproverDecision;
    }

    public LocalDateTime getSecondApproverDecidedAt() {
        return secondApproverDecidedAt;
    }

    public void setSecondApproverDecidedAt(LocalDateTime secondApproverDecidedAt) {
        this.secondApproverDecidedAt = secondApproverDecidedAt;
    }

    public Boolean getAppointment() {
        return isAppointment;
    }

    public void setAppointment(Boolean appointment) {
        isAppointment = appointment;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    @Deprecated
    public HalfDayType getHalfDayType() { return halfDayType; }
    @Deprecated
    public void setHalfDayType(HalfDayType halfDayType) { this.halfDayType = halfDayType; }

    public HalfDayType getStartDateHalfDayType() { return startDateHalfDayType; }
    public void setStartDateHalfDayType(HalfDayType startDateHalfDayType) {
        this.startDateHalfDayType = startDateHalfDayType;
    }

    public HalfDayType getEndDateHalfDayType() { return endDateHalfDayType; }
    public void setEndDateHalfDayType(HalfDayType endDateHalfDayType) {
        this.endDateHalfDayType = endDateHalfDayType;
    }

    public Boolean getIsAppointment() { return isAppointment; }
    public void setIsAppointment(Boolean isAppointment) { this.isAppointment = isAppointment; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getDays() { return days; }
    public void setDays(BigDecimal days) { this.days = days; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }

    public ApprovalLevel getCurrentApprovalLevel() { return currentApprovalLevel; }
    public void setCurrentApprovalLevel(ApprovalLevel currentApprovalLevel) {
        this.currentApprovalLevel = currentApprovalLevel;
    }

    public Integer getRequiredApprovalLevels() { return requiredApprovalLevels; }
    public void setRequiredApprovalLevels(Integer requiredApprovalLevels) {
        this.requiredApprovalLevels = requiredApprovalLevels;
    }


    public LeaveStatus getFirstApproverDecision() { return firstApproverDecision; }
    public void setFirstApproverDecision(LeaveStatus firstApproverDecision) {
        this.firstApproverDecision = firstApproverDecision;
    }

    public LocalDateTime getFirstApproverDecidedAt() { return firstApproverDecidedAt; }
    public void setFirstApproverDecidedAt(LocalDateTime firstApproverDecidedAt) {
        this.firstApproverDecidedAt = firstApproverDecidedAt;
    }

    public Long getFirstApproverId() {
        return firstApproverId;
    }

    public void setFirstApproverId(Long firstApproverId) {
        this.firstApproverId = firstApproverId;
    }

    public Long getSecondApproverId() { return secondApproverId; }
    public void setSecondApproverId(Long secondApproverId) { this.secondApproverId = secondApproverId; }

    public LeaveStatus getManagerDecision() { return secondApproverDecision; }
    public void setManagerDecision(LeaveStatus managerDecision) { this.secondApproverDecision = managerDecision; }

    public LocalDateTime getManagerDecidedAt() { return secondApproverDecidedAt; }
    public void setManagerDecidedAt(LocalDateTime managerDecidedAt) {
        this.secondApproverDecidedAt = managerDecidedAt;
    }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public Role getApprovedRole() { return approvedRole; }
    public void setApprovedRole(Role approvedRole) { this.approvedRole = approvedRole; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Double getCarryForwardUsed() { return carryForwardUsed; }
    public void setCarryForwardUsed(Double carryForwardUsed) { this.carryForwardUsed = carryForwardUsed; }

    public Double getCompOffUsed() { return compOffUsed; }
    public void setCompOffUsed(Double compOffUsed) { this.compOffUsed = compOffUsed; }

    public Double getLossOfPayApplied() { return lossOfPayApplied; }
    public void setLossOfPayApplied(Double lossOfPayApplied) { this.lossOfPayApplied = lossOfPayApplied; }

    public Double getPendingLopDays() { return pendingLopDays; }
    public void setPendingLopDays(Double pendingLopDays) { this.pendingLopDays = pendingLopDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getEscalated() { return escalated; }
    public void setEscalated(Boolean escalated) { this.escalated = escalated; }

    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}