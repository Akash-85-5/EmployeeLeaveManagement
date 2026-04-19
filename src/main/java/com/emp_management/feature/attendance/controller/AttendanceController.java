package com.emp_management.feature.attendance.controller;

import com.emp_management.feature.attendance.dto.*;
import com.emp_management.feature.attendance.service.AttendanceService;
import com.emp_management.feature.employee.dto.NameDto;
import com.emp_management.feature.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/attendance")
public class AttendanceController {

    private final AttendanceService service;
    private final EmployeeService empService;

    public AttendanceController(AttendanceService service, EmployeeService empService) {
        this.service = service;
        this.empService = empService;
    }

    // 🔹 Employee Calendar
    @GetMapping("/{empId}")
    public Page<AttendanceCalendarDTO> getEmployeeAttendanceByRange(
            @PathVariable String empId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getEmployeeAttendanceByRange(empId, fromDate, toDate, page, size);
    }

    @GetMapping("/employee/{empId}")
    public List<AttendanceCalendarDTO> getEmployeeAttendance(
            @PathVariable String empId,
            @RequestParam int year,
            @RequestParam int month) {

        return service.getEmployeeMonthly(empId, year, month);
    }

    @GetMapping("/download/excel/{employeeId}")
    public ResponseEntity<InputStreamResource> downloadExcel(
            @PathVariable String employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        // Fallback: If dates are missing, default to the current month
        LocalDate start = (fromDate != null) ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = (toDate != null) ? toDate : LocalDate.now();

        // Fetch records using the resolved dates
        List<AttendanceCalendarDTO> records = service.getEmployeeAttendanceByRange(
                employeeId, start, end, 0, 2000).getContent();
        NameDto empName = empService.getEmployeeName(employeeId);
        ByteArrayInputStream in = service.exportAttendanceToExcel(records, empName.getEmpName(), employeeId);

        // Dynamic filename based on dates used
        String fileName = "Attendance_" + employeeId + "_" + start + "_to_" + end + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    // Daily View
    @GetMapping("/daily")
    public List<AttendanceDetailDTO> getDailyAttendance(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return service.getDailyAttendance(date);
    }

    // All Employees (Filter + Pagination)
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
        Page<AttendanceDetailDTO> res = service.getAllEmployeesAttendance(fromDate, toDate, status, page, size);
        return res;
    }

    @GetMapping("/employee/{empId}/punch-records")
    public AttendanceCalendarDTO getPunchRecords(
            @PathVariable String empId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return service.getPunchRecords(empId, date);
    }

    @GetMapping("/team/{reportingId}")
    public Page<AttendanceDetailDTO> getTeamAttendance(
            @PathVariable String reportingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getTeamAttendance(reportingId, fromDate, toDate, status, page, size);
    }

    @PostMapping("/download/team/{managerId}")
    public ResponseEntity<InputStreamResource> downloadTeamExcel(
            @PathVariable String managerId,
            @RequestBody AttendanceExportRequest request) {

        List<AttendanceDetailDTO> records = service.getTeamAttendanceExportData(
                request.getEmpIds(),
                request.getFromDate(),
                request.getToDate()
        );

        ByteArrayInputStream in = service.exportTeamAttendanceToExcel(records);

        String fileName = "Team_Attendance_" + managerId + "_" + request.getFromDate() + "_to_" + request.getToDate() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    // 🔹 Universal Download: Allows Admin/Manager to download specific employee lists
    @PostMapping("/download/selection")
    public ResponseEntity<InputStreamResource> downloadSelectedEmployeesExcel(
            @Valid @RequestBody AttendanceExportRequest request) {

        // Validate that we have employees to fetch
        if (request.getEmpIds() == null || request.getEmpIds().isEmpty()) {
            throw new IllegalArgumentException("Employee list cannot be empty");
        }

        // Use the existing service method that fetches by List<String>
        List<AttendanceDetailDTO> records = service.getTeamAttendanceExportData(
                request.getEmpIds(),
                request.getFromDate(),
                request.getToDate()
        );

        ByteArrayInputStream in = service.exportTeamAttendanceToExcel(records);

        String fileName = "Attendance_Report_" + request.getFromDate() + "_to_" + request.getToDate() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/download/all")
    public ResponseEntity<InputStreamResource> downloadAllAttendanceExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status) {

        List<AttendanceDetailDTO> records = service.getAllAttendanceExportData(fromDate, toDate, status);

        ByteArrayInputStream in = service.exportTeamAttendanceToExcel(records);

        String fileName = "Organization_Attendance_" + fromDate + "_to_" + toDate + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}