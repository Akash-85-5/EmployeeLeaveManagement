package com.emp_management.feature.leave.annual.dto;

import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.HalfDayType;
import com.emp_management.shared.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class

LeaveApplicationResponseDTO {

    private Long id;
    private String employeeId;  // only the ID, not the whole Employee object
    private String leaveTypeName; // just the name from LeaveType, not the whole object

    private HalfDayType startDateHalfDayType;
    private HalfDayType endDateHalfDayType;
    private Boolean isAppointment;
    private Integer year;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private String reason;
    private RequestStatus status;
    private ApprovalLevel currentApprovalLevel;
    private Integer requiredApprovalLevels;
    private String currentApproverId;
    private String firstApproverId;
    private String rejectionReason;
    private RequestStatus firstApproverDecision;
    private LocalDateTime firstApproverDecidedAt;
    private String secondApproverId;
    private RequestStatus secondApproverDecision;
    private LocalDateTime secondApproverDecidedAt;
    private String approvedBy;
    private String approvedRole;
    private LocalDateTime approvedAt;
    private Double carryForwardUsed;
    private Double compOffUsed;
    private Double lossOfPayApplied;
    private Double pendingLopDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean escalated;
    private LocalDateTime escalatedAt;

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public Boolean getAppointment() {
        return isAppointment;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setAppointment(Boolean appointment) {
        isAppointment = appointment;
    }

    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }

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
}
