package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.EmployeeDashboardResponse;
import com.example.employeeLeaveApplication.dto.MonthlyStatsResponse;
import com.example.employeeLeaveApplication.dto.TeamMemberBalance;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.service.DashboardService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ==================== EMPLOYEE DASHBOARD ====================
    @GetMapping("/employee/{employeeId}")
    public EmployeeDashboardResponse getDashboard(@PathVariable Long employeeId) {
        return dashboardService.getDashboard(employeeId);
    }

    // ==================== MONTHLY STATISTICS ====================
    @GetMapping("/monthly-stats/{employeeId}")
    public MonthlyStatsResponse getMonthlyStats(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return dashboardService.getMonthlyStats(employeeId, year, month);
    }

    // ==================== TEAM BALANCES ====================
    @GetMapping("/team-balances/{managerId}")
    public List<TeamMemberBalance> getTeamBalances(
            @PathVariable Long managerId,
            @RequestParam Integer year
    ) {
        return dashboardService.getTeamBalances(managerId, year);
    }

    // ==================== PENDING COUNT ====================
    @GetMapping("/pending-count/{managerId}")
    public int getPendingCount(@PathVariable Long managerId) {
        return dashboardService.getPendingCount(managerId);
    }

    // ==================== UPCOMING LEAVES FOR EMPLOYEE   ====================
    @GetMapping("/employee/{employeeId}/upcoming-leaves")
    public List<LeaveApplication> getUpcomingLeaves(@PathVariable Long employeeId) {
        return dashboardService.getUpcomingLeaves(employeeId);
    }

    // ==================== RECENT LEAVES FOR EMPLOYEE   ====================
    @GetMapping("/employee/{employeeId}/recent-leaves")
    public Page<LeaveApplication> getRecentLeaves(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return dashboardService.getRecentLeaves(employeeId, limit);
    }

    // ==================== ADMIN OVERALL STATISTICS  ====================
    @GetMapping("/admin/statistics")
    public ResponseEntity<?> getAdminStatistics(
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(dashboardService.getAdminStatistics(year));
    }

    // ==================== ADMIN DASHBOARD SUMMARY - NEW ====================
    @GetMapping("/admin/summary")
    public ResponseEntity<Map<String, Object>> getAdminDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getAdminDashboardSummary());
    }

    // ==================== LEAVE CALENDAR FOR EMPLOYEE - NEW ====================
    @GetMapping("/employee/{employeeId}/calendar")
    public ResponseEntity<?> getLeaveCalendar(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return ResponseEntity.ok(dashboardService.getLeaveCalendar(employeeId, year, month));
    }

    // ==================== TEAM LEAVE CALENDAR - NEW ====================
    @GetMapping("/manager/{managerId}/team-calendar")
    public ResponseEntity<?> getTeamLeaveCalendar(
            @PathVariable Long managerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(dashboardService.getTeamLeaveCalendar(managerId, startDate, endDate));
    }

    // ==================== WHO'S OUT TODAY - NEW ====================
    @GetMapping("/whos-out/today")
    public ResponseEntity<?> getWhosOutToday() {
        return ResponseEntity.ok(dashboardService.getWhosOutToday());
    }

    // ==================== WHO'S OUT BY DATE RANGE - NEW ====================
    @GetMapping("/whos-out")
    public ResponseEntity<?> getWhosOut(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(dashboardService.getWhosOut(startDate, endDate));
    }
}