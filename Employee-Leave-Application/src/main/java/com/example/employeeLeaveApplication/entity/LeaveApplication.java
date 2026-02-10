package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.HalfDayType;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import jakarta.persistence.*;
import org.antlr.v4.runtime.misc.NotNull;

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
    private Integer year;


    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private BigDecimal days;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status = LeaveStatus.PENDING;

    @NotNull
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;


    @OneToMany(mappedBy = "leaveApplication",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<LeaveAttachment> attachments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        populateYear();
    }

    @PreUpdate
    private void populateYear() {
        if (this.startDate != null) {
            this.year = this.startDate.getYear();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public List<LeaveAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<LeaveAttachment> attachments) {
        this.attachments = attachments;
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
}



