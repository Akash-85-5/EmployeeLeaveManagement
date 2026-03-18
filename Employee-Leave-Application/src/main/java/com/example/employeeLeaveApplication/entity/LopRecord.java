package com.example.employeeLeaveApplication.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "lop_records",
        indexes = {
                @Index(name = "idx_lop_emp_date",  columnList = "employee_id, lop_date"),
                @Index(name = "idx_lop_emp_month", columnList = "employee_id, lop_month, lop_year")
        }
)
public class LopRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK → employee.id
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    // Stored here so reports don't need a join
    @Column(name = "employee_name")
    private String employeeName;

    // EMPLOYEE / TEAM_LEADER / MANAGER / ADMIN
    // HR and CFO never appear here — they don't get LOP
    @Column(name = "employee_role", length = 20)
    private String employeeRole;

    // The specific date employee was absent
    @Column(name = "lop_date", nullable = false)
    private LocalDate lopDate;

    // 1.0 = full day, 0.5 = half day (future use)
    @Column(name = "lop_days", nullable = false)
    private Double lopDays = 1.0;

    // Stored for fast GROUP BY without date functions
    @Column(name = "lop_month", nullable = false)
    private Integer lopMonth;

    @Column(name = "lop_year", nullable = false)
    private Integer lopYear;

    // NO_PUNCH / LEAVE_NOT_APPROVED / NO_LEAVE_APPLIED
    @Column(name = "lop_reason", length = 30)
    private String lopReason = "NO_PUNCH";

    // FK → attendance_summary.id for traceability
    @Column(name = "attendance_summary_id")
    private Long attendanceSummaryId;

    // HR can reverse a wrongly inserted LOP
    @Column(name = "reversed", nullable = false)
    private boolean reversed = false;

    @Column(name = "reversed_by")
    private Long reversedBy;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", length = 300)
    private String reversalReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.lopDate != null) {
            this.lopMonth = this.lopDate.getMonthValue();
            this.lopYear  = this.lopDate.getYear();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeRole() { return employeeRole; }
    public void setEmployeeRole(String employeeRole) { this.employeeRole = employeeRole; }

    public LocalDate getLopDate() { return lopDate; }
    public void setLopDate(LocalDate lopDate) { this.lopDate = lopDate; }

    public Double getLopDays() { return lopDays; }
    public void setLopDays(Double lopDays) { this.lopDays = lopDays; }

    public Integer getLopMonth() { return lopMonth; }
    public void setLopMonth(Integer lopMonth) { this.lopMonth = lopMonth; }

    public Integer getLopYear() { return lopYear; }
    public void setLopYear(Integer lopYear) { this.lopYear = lopYear; }

    public String getLopReason() { return lopReason; }
    public void setLopReason(String lopReason) { this.lopReason = lopReason; }

    public Long getAttendanceSummaryId() { return attendanceSummaryId; }
    public void setAttendanceSummaryId(Long attendanceSummaryId) { this.attendanceSummaryId = attendanceSummaryId; }

    public boolean isReversed() { return reversed; }
    public void setReversed(boolean reversed) { this.reversed = reversed; }

    public Long getReversedBy() { return reversedBy; }
    public void setReversedBy(Long reversedBy) { this.reversedBy = reversedBy; }

    public LocalDateTime getReversedAt() { return reversedAt; }
    public void setReversedAt(LocalDateTime reversedAt) { this.reversedAt = reversedAt; }

    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}