package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.service.ReportsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportsService reportService;

    public ReportsController(ReportsService reportService) {
        this.reportService = reportService;
    }

    // ==================== ANNUAL LEAVE SUMMARY ====================
    @GetMapping("/annual/{year}")
    public ResponseEntity<?> getAnnualLeaveSummary(@PathVariable Integer year) {
        return ResponseEntity.ok(reportService.getAnnualLeaveSummary(year));
    }

    // ==================== EMPLOYEE LEAVE HISTORY ====================
    @GetMapping("/employee/{employeeId}/history")
    public ResponseEntity<?> getEmployeeLeaveHistory(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(reportService.getEmployeeLeaveHistory(employeeId, year));
    }

    // ==================== EMPLOYEE DETAILED SUMMARY ====================
    @GetMapping("/employee/{employeeId}/summary")
    public ResponseEntity<?> getEmployeeLeaveSummary(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(reportService.getEmployeeLeaveSummary(employeeId, year));
    }

    // ==================== DEPARTMENT-WISE LEAVE REPORT ====================
//    @GetMapping("/department/{deptId}/leaves")
//    public ResponseEntity<?> getDepartmentLeaveReport(
//            @PathVariable Long deptId,
//            @RequestParam(required = false) Integer year
//    ) {
//        return ResponseEntity.ok(reportService.getDepartmentLeaveReport(deptId, year));
//    }

    // ==================== MANAGER'S TEAM LEAVE REPORT ====================
    @GetMapping("/manager/{managerId}/team-report")
    public ResponseEntity<?> getTeamLeaveReport(
            @PathVariable Long managerId,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(reportService.getManagerTeamReport(managerId, year));
    }

    // ==================== LEAVE TRENDS ====================
    @GetMapping("/trends")
    public ResponseEntity<?> getLeaveTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getLeaveTrends(startDate, endDate));
    }

    // ==================== LEAVE TYPE DISTRIBUTION ====================
    @GetMapping("/distribution/leave-types")
    public ResponseEntity<?> getLeaveTypeDistribution(
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(reportService.getLeaveTypeDistribution(year));
    }

    // ==================== MONTHLY LEAVE STATISTICS ====================
    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<?> getMonthlyStatistics(
            @PathVariable Integer year,
            @PathVariable Integer month
    ) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
    }

    // ==================== LOSS OF PAY REPORT ====================
    @GetMapping("/lop/{year}")
    public ResponseEntity<?> getLossOfPayReport(@PathVariable Integer year) {
        return ResponseEntity.ok(reportService.getLossOfPayReport(year));
    }

    // ==================== COMP-OFF UTILIZATION REPORT ====================
    @GetMapping("/compoff/utilization")
    public ResponseEntity<?> getCompOffUtilization() {
        return ResponseEntity.ok(reportService.getCompOffUtilizationReport());
    }

    // ==================== EXPORT REPORT (CSV/PDF) - PLACEHOLDER ====================
    @GetMapping("/export/{reportType}")
    public ResponseEntity<String> exportReport(
            @PathVariable String reportType,
            @RequestParam String format,
            @RequestParam(required = false) Map<String, Object> filters) {

        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body("Report export is not yet available. " +
                        "Requested: " + reportType + " in format " + format);
    }
}