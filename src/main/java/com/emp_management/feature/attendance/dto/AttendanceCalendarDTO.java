package com.emp_management.feature.attendance.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceCalendarDTO {

    private LocalDate date;
    private String status;
    private LocalTime checkIn;
    private LocalTime checkOut;

    // ✅ Double (hours as decimal like 8.5) — not LocalTime
    private LocalTime workingHours;

    // ✅ Added punch records
    private String punchRecords;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalTime checkIn) { this.checkIn = checkIn; }

    public LocalTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalTime checkOut) { this.checkOut = checkOut; }

    public LocalTime getWorkingHours() { return workingHours; }
    public void setWorkingHours(LocalTime workingHours) { this.workingHours = workingHours; }

    public String getPunchRecords() { return punchRecords; }
    public void setPunchRecords(String punchRecords) { this.punchRecords = punchRecords; }
}