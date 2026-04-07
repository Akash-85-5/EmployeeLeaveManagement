package com.emp_management.feature.workfromhome.entity;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.HalfDayType;
import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "wfh_request")
public class WfhRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SAME mapping style as Leave
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    private String reason;

    // USE EXISTING ENUM
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    // SAME APPROVAL FLOW FIELD
    @Column(name = "current_approver")
    private String currentApprover;

    private String remarks;
    private String approvedBy;
    private LocalDateTime approvedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_type")
    private HalfDayType halfDayType;
    @Column(name = "current_approver_id")
    private String currentApproverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_approval_level")
    private ApprovalLevel currentApprovalLevel;

    @Column(name = "second_approver_id")
    private String secondApproverId;

    @Column(name = "is_escalated")
    private boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // GETTERS / SETTERS
    public Long getId() { return id; }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getCurrentApprover() { return currentApprover; }
    public void setCurrentApprover(String currentApprover) { this.currentApprover = currentApprover; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public HalfDayType getHalfDayType() {
        return halfDayType;
    }

    public void setHalfDayType(HalfDayType halfDayType) {
        this.halfDayType = halfDayType;
    }
    // 🔥 ADD THESE METHODS

    public String getCurrentApproverId() {
        return currentApproverId;
    }

    public void setCurrentApproverId(String currentApproverId) {
        this.currentApproverId = currentApproverId;
    }

    public ApprovalLevel getCurrentApprovalLevel() {
        return currentApprovalLevel;
    }

    public void setCurrentApprovalLevel(ApprovalLevel currentApprovalLevel) {
        this.currentApprovalLevel = currentApprovalLevel;
    }

    public String getSecondApproverId() {
        return secondApproverId;
    }

    public void setSecondApproverId(String secondApproverId) {
        this.secondApproverId = secondApproverId;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }
    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

}