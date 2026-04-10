package com.emp_management.feature.attendance.controller;

import com.emp_management.feature.attendance.dto.*;
import com.emp_management.feature.attendance.service.AttendanceService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    // 🔹 Employee Calendar
    @GetMapping("/employee/{empId}")
    public List<AttendanceCalendarDTO> getEmployeeAttendance(
            @PathVariable String empId,
            @RequestParam int year,
            @RequestParam int month) {

        return service.getEmployeeMonthly(empId, year, month);
    }

    // 🔹 Daily View
    @GetMapping("/daily")
    public List<AttendanceDetailDTO> getDailyAttendance(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return service.getDailyAttendance(date);
    }

    // 🔹 All Employees (Filter + Pagination)
    @GetMapping("/all")
    public Page<AttendanceDetailDTO> getAllEmployeesAttendance(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            String status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        return service.getAllEmployeesAttendance(fromDate, toDate, status, page, size);
    }
    @GetMapping("/employee/{empId}/punch-records")
    public AttendanceCalendarDTO getPunchRecords(
            @PathVariable String empId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return service.getPunchRecords(empId, date);
    }
}