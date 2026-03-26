package com.example.employeeLeaveApplication.feature.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "biometric_logs",
        indexes = {
                @Index(name = "idx_bio_emp_date",     columnList = "employee_id, punch_date"),
                @Index(name = "idx_bio_emp_code_date", columnList = "emp_code, punch_date"),
                @Index(name = "idx_bio_device",        columnList = "device_id")
        }
)
public class BiometricLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE IDENTITY
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "employee_id")               // nullable — filled after emp lookup
    private Long employeeId;

    @Column(name = "emp_code", length = 20)     // NEW — from biometric device e.g. INTERN001
    private String empCode;

    @Column(name = "employee_name")
    private String employeeName;

    // ═══════════════════════════════════════════════════════════════
    // DEVICE
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "device_id", length = 50)    // nullable = false removed — not always present
    private String deviceId;                    // e.g. WENXT

    // ═══════════════════════════════════════════════════════════════
    // PUNCH DATA
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "punch_date", nullable = false)
    private LocalDate punchDate;

    @Column(name = "punch_time")                // nullable — Excel only gives HH:MM, no seconds
    private LocalDateTime punchTime;

    @Column(name = "punch_time_only")
    private LocalTime punchTimeOnly;            // HH:MM from Excel e.g. 09:20

    // IN / OUT — from Excel token e.g. "in" or "out"
    // MID — assigned by scheduler for mid-day punches
    @Column(name = "punch_type", length = 10)
    private String punchType;

    // Order of punch within the day for this employee: 1, 2, 3...
    // 1st punch = IN, last punch = OUT, rest = MID
    @Column(name = "punch_sequence")
    private Integer punchSequence;

    // ═══════════════════════════════════════════════════════════════
    // VERIFICATION & PROCESSING
    // ═══════════════════════════════════════════════════════════════

    // FINGERPRINT / FACE / CARD / PIN — not available in Excel import, set later
    @Column(name = "verification_type", length = 20)
    private String verificationType;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    // ═══════════════════════════════════════════════════════════════
    // AUDIT
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Build punchTime from date + time if both present
        if (this.punchDate != null && this.punchTimeOnly != null) {
            this.punchTime = LocalDateTime.of(this.punchDate, this.punchTimeOnly);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════════

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmpCode() { return empCode; }
    public void setEmpCode(String empCode) { this.empCode = empCode; }

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

    public Integer getPunchSequence() { return punchSequence; }
    public void setPunchSequence(Integer punchSequence) { this.punchSequence = punchSequence; }

    public String getVerificationType() { return verificationType; }
    public void setVerificationType(String verificationType) { this.verificationType = verificationType; }

    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}