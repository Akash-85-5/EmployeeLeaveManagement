package com.example.employeeLeaveApplication.feature.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/*
 * attendance_status values:
 *   PRESENT    — biometric punch found
 *   ABSENT     — no punch, no leave, no WFH → LOP inserted
 *   HALF_DAY   — ½Present from biometric device
 *   ON_LEAVE   — approved leave covers this date
 *   WFH        — approved WFH covers this date
 *   HOLIDAY    — date is in holiday_calendar
 *   WEEKEND    — Saturday or Sunday
 *   NOT_JOINED — date is before employee joining_date
 *   RELIEVED   — date is after employee relieving_date
 */
@Entity
@Table(
        name = "attendance_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_att_emp_date",
                columnNames = {"employee_id", "attendance_date"}
        ),
        indexes = {
                @Index(name = "idx_att_emp_month",  columnList = "employee_id, attendance_date"),
                @Index(name = "idx_att_emp_code",   columnList = "emp_code")
        }
)
public class AttendanceSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE IDENTITY
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "employee_id")
    private Long employeeId;                    // FK → employee.id (nullable during import)

    @Column(name = "emp_code", length = 20)
    private String empCode;                     // e.g. INTERN001, WENXT002 — from biometric device

    @Column(name = "employee_name")
    private String employeeName;

    // ═══════════════════════════════════════════════════════════════
    // ATTENDANCE CORE
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "attendance_status", nullable = false, length = 20)
    private String attendanceStatus;

    @Column(name = "shift_code", length = 10)
    private String shiftCode;                   // GS = General Shift, NS = Night Shift

    // ═══════════════════════════════════════════════════════════════
    // PUNCH TIMES
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "check_in")
    private LocalTime checkIn;                  // First punch of the day

    // Last biometric punch of the day
    @Column(name = "check_out")
    private LocalTime checkOut;                 // Last punch of the day

    // Working hours = checkOut - checkIn
    @Column(name = "working_hours")
    private Double workingHours;                // checkOut - checkIn in hours

    @Column(name = "late_by")
    private LocalTime lateBy;                   // How late the employee arrived

    @Column(name = "early_going_by")
    private LocalTime earlyGoingBy;             // How early the employee left

    @Column(name = "raw_punch_records", columnDefinition = "TEXT")
    private String rawPunchRecords;             // e.g. 09:20:in(WENXT),11:09:out(WENXT),...

    // ═══════════════════════════════════════════════════════════════
    // REFERENCES
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "biometric_in_id")
    private Long biometricInId;

    @Column(name = "biometric_out_id")
    private Long biometricOutId;

    // FK → leave_application.id if ON_LEAVE
    @Column(name = "leave_id")
    private Long leaveId;

    // FK → work_from_home.id if WFH
    @Column(name = "wfh_id")
    private Long wfhId;

    @Column(name = "lop_triggered", nullable = false)
    private boolean lopTriggered = false;

    // ═══════════════════════════════════════════════════════════════
    // AUDIT
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "created_at", nullable = false, updatable = false)
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

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }

    public String getShiftCode() { return shiftCode; }
    public void setShiftCode(String shiftCode) { this.shiftCode = shiftCode; }

    public LocalTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalTime checkIn) { this.checkIn = checkIn; }

    public LocalTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalTime checkOut) { this.checkOut = checkOut; }

    public Double getWorkingHours() { return workingHours; }
    public void setWorkingHours(Double workingHours) { this.workingHours = workingHours; }

    public LocalTime getLateBy() { return lateBy; }
    public void setLateBy(LocalTime lateBy) { this.lateBy = lateBy; }

    public LocalTime getEarlyGoingBy() { return earlyGoingBy; }
    public void setEarlyGoingBy(LocalTime earlyGoingBy) { this.earlyGoingBy = earlyGoingBy; }

    public String getRawPunchRecords() { return rawPunchRecords; }
    public void setRawPunchRecords(String rawPunchRecords) { this.rawPunchRecords = rawPunchRecords; }

    public Long getBiometricInId() { return biometricInId; }
    public void setBiometricInId(Long biometricInId) { this.biometricInId = biometricInId; }

    public Long getBiometricOutId() { return biometricOutId; }
    public void setBiometricOutId(Long biometricOutId) { this.biometricOutId = biometricOutId; }

    public Long getLeaveId() { return leaveId; }
    public void setLeaveId(Long leaveId) { this.leaveId = leaveId; }

    public Long getWfhId() { return wfhId; }
    public void setWfhId(Long wfhId) { this.wfhId = wfhId; }

    public boolean isLopTriggered() { return lopTriggered; }
    public void setLopTriggered(boolean lopTriggered) { this.lopTriggered = lopTriggered; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}