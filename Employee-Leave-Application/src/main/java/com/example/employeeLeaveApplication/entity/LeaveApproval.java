package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.LeaveStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_approval")

public class  LeaveApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long leaveId;
    private Long managerId;

    @Enumerated(EnumType.STRING)
    private LeaveStatus decision;

    private String comments;
    private LocalDateTime decidedAt;

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(Long leaveId) {
        this.leaveId = leaveId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public LeaveStatus getDecision() {
        return decision;
    }

    public void setDecision(LeaveStatus decision) {
        this.decision = decision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
