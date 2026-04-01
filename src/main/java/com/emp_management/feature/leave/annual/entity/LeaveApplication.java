package com.emp_management.feature.leave.annual.entity;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.HalfDayType;
import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_application")
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── FK to Employee (String PK: emp_code) ─────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ── FK to LeaveType entity ────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
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
    private RequestStatus status = RequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_approval_level")
    private ApprovalLevel currentApprovalLevel;

    @Column(name = "required_approval_levels")
    private Integer requiredApprovalLevels;

    @Column(name = "current_approver_id")
    private String currentApproverId;

    @Column(name = "first_approver_id")
    private String firstApproverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_approver_decision")
    private RequestStatus firstApproverDecision;

    @Column(name = "first_approver_decided_at")
    private LocalDateTime firstApproverDecidedAt;

    @Column(name = "second_approver_id")
    private String secondApproverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "second_approver_decision")
    private RequestStatus secondApproverDecision;

    @Column(name = "second_approver_decided_at")
    private LocalDateTime secondApproverDecidedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_role")
    private String approvedRole;

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
        if (this.startDate != null) this.year = this.startDate.getYear();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.startDate != null) this.year = this.startDate.getYear();
    }

    // ── Convenience helpers ───────────────────────────────────────

    /** Shortcut used throughout services instead of getEmployee().getEmpId() */
    @Transient
    public String getEmployeeId() {
        return employee != null ? employee.getEmpId() : null;
    }

    /** Shortcut for notifications / display */
    @Transient
    public String getEmployeeName() {
        return employee != null ? employee.getName() : null;
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    public HalfDayType getHalfDayType() { return halfDayType; }
    public void setHalfDayType(HalfDayType halfDayType) { this.halfDayType = halfDayType; }

    public HalfDayType getStartDateHalfDayType() { return startDateHalfDayType; }
    public void setStartDateHalfDayType(HalfDayType startDateHalfDayType) { this.startDateHalfDayType = startDateHalfDayType; }

    public HalfDayType getEndDateHalfDayType() { return endDateHalfDayType; }
    public void setEndDateHalfDayType(HalfDayType endDateHalfDayType) { this.endDateHalfDayType = endDateHalfDayType; }

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

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public ApprovalLevel getCurrentApprovalLevel() { return currentApprovalLevel; }
    public void setCurrentApprovalLevel(ApprovalLevel currentApprovalLevel) { this.currentApprovalLevel = currentApprovalLevel; }

    public Integer getRequiredApprovalLevels() { return requiredApprovalLevels; }
    public void setRequiredApprovalLevels(Integer requiredApprovalLevels) { this.requiredApprovalLevels = requiredApprovalLevels; }

    public String getCurrentApproverId() { return currentApproverId; }
    public void setCurrentApproverId(String currentApproverId) { this.currentApproverId = currentApproverId; }

    public String getFirstApproverId() { return firstApproverId; }
    public void setFirstApproverId(String firstApproverId) { this.firstApproverId = firstApproverId; }

    public RequestStatus getFirstApproverDecision() { return firstApproverDecision; }
    public void setFirstApproverDecision(RequestStatus firstApproverDecision) { this.firstApproverDecision = firstApproverDecision; }

    public LocalDateTime getFirstApproverDecidedAt() { return firstApproverDecidedAt; }
    public void setFirstApproverDecidedAt(LocalDateTime firstApproverDecidedAt) { this.firstApproverDecidedAt = firstApproverDecidedAt; }

    public String getSecondApproverId() { return secondApproverId; }
    public void setSecondApproverId(String secondApproverId) { this.secondApproverId = secondApproverId; }

    public RequestStatus getSecondApproverDecision() { return secondApproverDecision; }
    public void setSecondApproverDecision(RequestStatus secondApproverDecision) { this.secondApproverDecision = secondApproverDecision; }

    public LocalDateTime getSecondApproverDecidedAt() { return secondApproverDecidedAt; }
    public void setSecondApproverDecidedAt(LocalDateTime secondApproverDecidedAt) { this.secondApproverDecidedAt = secondApproverDecidedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedRole() { return approvedRole; }
    public void setApprovedRole(String approvedRole) { this.approvedRole = approvedRole; }

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