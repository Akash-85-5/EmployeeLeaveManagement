package com.example.employeeLeaveApplication.feature.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "biometric_logs",
        indexes = {
                @Index(name = "idx_bio_emp_date", columnList = "employee_id, punch_date"),
                @Index(name = "idx_bio_device",   columnList = "device_id")
        }
)
public class BiometricLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    // Date part of the punch — used for daily grouping
    @Column(name = "punch_date", nullable = false)
    private LocalDate punchDate;

    // Full timestamp of the punch
    @Column(name = "punch_time", nullable = false)
    private LocalDateTime punchTime;

    // Time only — for quick IN/OUT extraction
    @Column(name = "punch_time_only")
    private LocalTime punchTimeOnly;

    // IN / OUT / MID — set by scheduler after day-end processing
    @Column(name = "punch_type", length = 10)
    private String punchType;

    // FINGERPRINT / FACE / CARD / PIN
    @Column(name = "verification_type", length = 20)
    private String verificationType;

    // Whether this record has been processed by the daily scheduler
    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.punchTime != null) {
            this.punchDate     = this.punchTime.toLocalDate();
            this.punchTimeOnly = this.punchTime.toLocalTime();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public LocalDate getPunchDate() { return punchDate; }
    public void setPunchDate(LocalDate punchDate) { this.punchDate = punchDate; }

    public LocalDateTime getPunchTime() { return punchTime; }
    public void setPunchTime(LocalDateTime punchTime) { this.punchTime = punchTime; }

    public LocalTime getPunchTimeOnly() { return punchTimeOnly; }
    public void setPunchTimeOnly(LocalTime punchTimeOnly) { this.punchTimeOnly = punchTimeOnly; }

    public String getPunchType() { return punchType; }
    public void setPunchType(String punchType) { this.punchType = punchType; }

    public String getVerificationType() { return verificationType; }
    public void setVerificationType(String verificationType) { this.verificationType = verificationType; }

    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}