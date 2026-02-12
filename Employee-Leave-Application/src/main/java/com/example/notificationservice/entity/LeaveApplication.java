package com.example.notificationservice.entity;

import com.example.notificationservice.enums.HalfDayType;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.enums.LeaveType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leave_application")
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    private HalfDayType halfDayType ;

    @NotNull
    @Column(name = "leave_year")
    private Integer year;


    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private BigDecimal days;
    @Column(name = "ESCALATED")
    private Boolean escalated;
    private LocalDateTime escalatedAt;
    @NotNull
    private Long managerId;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status = LeaveStatus.PENDING;

    @NotNull
    private String reason;

    @NotNull
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "leaveApplication",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<LeaveAttachment> attachments = new ArrayList<>();

    public List<LeaveAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<LeaveAttachment> attachments) {
        this.attachments = attachments;
    }

    public Long getId() {
        return id;
    }
    public Boolean getEscalated() {
        return escalated;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public Long getManagerId() {
        return managerId;
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

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    @PrePersist
    @PreUpdate
    private void prePersistOrUpdate() {
        // Set year from startDate
        if (this.startDate != null) {
            this.year = this.startDate.getYear();
        }

        // Set submittedAt only if null (first time)
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }

    public void setEscalated(Boolean escalated) {
        this.escalated = escalated;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }


}



