package com.emp_management.feature.attendance.entity;

import com.emp_management.shared.converter.SafeLocalTimeConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;

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
    @Convert(converter = SafeLocalTimeConverter.class)
    private LocalTime checkIn;

    @Column(name = "check_out")
    @Convert(converter = SafeLocalTimeConverter.class)
    private LocalTime checkOut;

    @Column(name = "working_hours")
    @Convert(converter = SafeLocalTimeConverter.class)
    private LocalTime workingHours;

    @Column(name = "punch_records")
    private String punchRecords;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "late_by")
    @Convert(converter = SafeLocalTimeConverter.class)
    private LocalTime lateBy;

    @Column(name = "early_going_by")
    @Convert(converter = SafeLocalTimeConverter.class)
    private LocalTime earlyGoingBy;

    @Column(name = "leave_id")
    private Long leaveId;

    @Column(name = "wfh_id")
    private Long wfhId;

    @Column(name = "lop_triggered")
    private boolean lopTriggered;

    @Column(name = "biometric_in_id")
    private Long biometricInId;

    @Column(name = "biometric_out_id")
    private Long biometricOutId;

    @Column(name = "ot_hours")
    private Double otHours;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    // ---------------- GETTERS & SETTERS ----------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

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

    public Long getBiometricInId() { return biometricInId; }
    public void setBiometricInId(Long biometricInId) { this.biometricInId = biometricInId; }

    public Long getBiometricOutId() { return biometricOutId; }
    public void setBiometricOutId(Long biometricOutId) { this.biometricOutId = biometricOutId; }

    public Double getOtHours() { return otHours; }
    public void setOtHours(Double otHours) { this.otHours = otHours; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}