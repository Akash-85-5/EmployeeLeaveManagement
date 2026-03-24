package com.example.employeeLeaveApplication.feature.od.entity;

import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import com.example.employeeLeaveApplication.shared.enums.ODStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ODRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    private String employeeName;

    private String reason;

    private LocalDate startDate;

    private LocalDate endDate;

    private LeaveType leaveType = LeaveType.ON_DUTY;

    @Enumerated(EnumType.STRING)
    private ODStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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