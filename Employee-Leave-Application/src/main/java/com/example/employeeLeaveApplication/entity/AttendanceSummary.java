package com.example.employeeLeaveApplication.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/*
 * attendance_status values:
 *   PRESENT   — biometric punch found
 *   ABSENT    — no punch, no leave, no WFH → LOP inserted
 *   ON_LEAVE  — approved leave covers this date
 *   WFH       — approved WFH covers this date
 *   HOLIDAY   — date is in holiday_calendar
 *   WEEKEND   — Saturday or Sunday
 *   NOT_JOINED — date is before employee joining_date
 *   RELIEVED  — date is after employee relieving_date
 */
@Entity
@Table(
        name = "attendance_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_att_emp_date",
                columnNames = {"employee_id", "attendance_date"}
        ),
        indexes = {
                @Index(name = "idx_att_emp_month", columnList = "employee_id, attendance_date")
        }
)
public class AttendanceSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "attendance_status", nullable = false, length = 20)
    private String attendanceStatus;

    // First biometric punch of the day
    @Column(name = "check_in")
    private LocalTime checkIn;

    // Last biometric punch of the day
    @Column(name = "check_out")
    private LocalTime checkOut;

    // Working hours = checkOut - checkIn
    @Column(name = "working_hours")
    private Double workingHours;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }

    public LocalTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalTime checkIn) { this.checkIn = checkIn; }

    public LocalTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalTime checkOut) { this.checkOut = checkOut; }

    public Double getWorkingHours() { return workingHours; }
    public void setWorkingHours(Double workingHours) { this.workingHours = workingHours; }

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
