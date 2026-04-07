package com.example.employeeLeaveApplication.feature.overtime.entity;

import com.example.employeeLeaveApplication.shared.enums.CompensationType;
import com.example.employeeLeaveApplication.shared.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "overtime_request")
@Data
public class OvertimeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // FIX: Removed precision and scale because Double is a floating point type
    @Column(nullable = false)
    private Double totalHours;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompensationType compensationType;

    private String proofDocumentPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    // --- Approval Level 1: Team Leader ---
    private Long tlId;
    private LocalDateTime tlDecidedAt;

    // --- Approval Level 2: Manager ---
    private Long managerId;
    private LocalDateTime managerDecidedAt;

    // --- Approval Level 3: HR ---
    private Long hrId;
    private LocalDateTime hrDecidedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}