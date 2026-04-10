package com.emp_management.feature.attendance.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance_summary")
public class AttendanceSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @Column(name = "attendance_status")
    private String attendanceStatus;

    @Column(name = "check_in")
    private LocalTime checkIn;

    @Column(name = "check_out")
    private LocalTime checkOut;

    // ✅ NOW CONSISTENT: LocalTime everywhere
    @Column(name = "working_hours")
    private LocalTime workingHours;

    @Column(name = "punch_records")
    private String punchRecords;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "late_by")
    private LocalTime lateBy;

    @Column(name = "early_going_by")
    private LocalTime earlyGoingBy;

    @Column(name = "leave_id")
    private Long leaveId;

    @Column(name = "wfh_id")
    private Long wfhId;

    @Column(name = "lop_triggered")
    private boolean lopTriggered;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "created_by")
    private LocalDateTime createdBy;

    @Column(name = "updated_by")
    private LocalDateTime updatedBy;

    // 🔥 AUTO CALCULATE WORKING HOURS
//    public void calculateWorkingHours() {
//        if (checkIn != null && checkOut != null) {
//
//            Duration duration = Duration.between(checkIn, checkOut);
//
//            // ⚠️ Handle overnight shift (important)
//            if (duration.isNegative()) {
//                duration = duration.plusHours(24);
//            }
//
//            long hours = duration.toHours();
//            long minutes = duration.toMinutes() % 60;
//
//            this.workingHours = LocalTime.of((int) hours, (int) minutes);
//        }
//    }
//
//    @PrePersist
//    public void onCreate() {
//        this.createdAt = LocalDateTime.now();
//        this.updatedAt = LocalDateTime.now();
//        calculateWorkingHours(); // ✅ auto-set before insert
//    }
//
//    @PreUpdate
//    public void onUpdate() {
//        this.updatedAt = LocalDateTime.now();
//        calculateWorkingHours(); // ✅ auto-update
//    }

    // ---------------- GETTERS & SETTERS ----------------

    public Long getId() { return id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

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

    public LocalTime getWorkingHours() { return workingHours; }
    public void setWorkingHours(LocalTime workingHours) { this.workingHours = workingHours; }

    public String getPunchRecords() { return punchRecords; }
    public void setPunchRecords(String punchRecords) { this.punchRecords = punchRecords; }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(LocalDateTime createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(LocalDateTime updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalTime getLateBy() { return lateBy; }
    public void setLateBy(LocalTime lateBy) { this.lateBy = lateBy; }

    public LocalTime getEarlyGoingBy() { return earlyGoingBy; }
    public void setEarlyGoingBy(LocalTime earlyGoingBy) { this.earlyGoingBy = earlyGoingBy; }

    public Long getLeaveId() { return leaveId; }
    public void setLeaveId(Long leaveId) { this.leaveId = leaveId; }

    public Long getWfhId() { return wfhId; }
    public void setWfhId(Long wfhId) { this.wfhId = wfhId; }

    public boolean isLopTriggered() { return lopTriggered; }
    public void setLopTriggered(boolean lopTriggered) { this.lopTriggered = lopTriggered; }
}