package com.example.employeeLeaveApplication.feature.separation.entity;

import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import com.example.employeeLeaveApplication.shared.enums.SeparationType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "separations")
public class Separation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    @Enumerated(EnumType.STRING)
    private SeparationType type; // RESIGNATION / TERMINATION / RETIREMENT

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    private SeparationStatus status;

    private LocalDate noticeStartDate;
    private LocalDate noticeEndDate;
    private LocalDate actualRelievingDate;

    private LocalDateTime managerApprovedAt;
    private LocalDateTime hrApprovedAt;
    private LocalDateTime ceoApprovedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ───── Auto timestamps ─────
    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ───── Getters & Setters ─────

    // generate using IntelliJ (Alt + Insert)

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

    public SeparationType getType() {
        return type;
    }

    public void setType(SeparationType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public SeparationStatus getStatus() {
        return status;
    }

    public void setStatus(SeparationStatus status) {
        this.status = status;
    }

    public LocalDate getNoticeStartDate() {
        return noticeStartDate;
    }

    public void setNoticeStartDate(LocalDate noticeStartDate) {
        this.noticeStartDate = noticeStartDate;
    }

    public LocalDate getNoticeEndDate() {
        return noticeEndDate;
    }

    public void setNoticeEndDate(LocalDate noticeEndDate) {
        this.noticeEndDate = noticeEndDate;
    }

    public LocalDate getActualRelievingDate() {
        return actualRelievingDate;
    }

    public void setActualRelievingDate(LocalDate actualRelievingDate) {
        this.actualRelievingDate = actualRelievingDate;
    }

    public LocalDateTime getManagerApprovedAt() {
        return managerApprovedAt;
    }

    public void setManagerApprovedAt(LocalDateTime managerApprovedAt) {
        this.managerApprovedAt = managerApprovedAt;
    }

    public LocalDateTime getHrApprovedAt() {
        return hrApprovedAt;
    }

    public void setHrApprovedAt(LocalDateTime hrApprovedAt) {
        this.hrApprovedAt = hrApprovedAt;
    }

    public LocalDateTime getCeoApprovedAt() {
        return ceoApprovedAt;
    }

    public void setCeoApprovedAt(LocalDateTime ceoApprovedAt) {
        this.ceoApprovedAt = ceoApprovedAt;
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
}