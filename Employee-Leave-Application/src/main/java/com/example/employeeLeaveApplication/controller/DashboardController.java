package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.*;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
// ✅ REMOVED @CrossOrigin here — configure globally in CorsConfig.java instead
public class DashboardController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(
            @PathVariable Long employeeId) {
        log.info("[API] GET employee dashboard: {}", employeeId);
        try {
            return ResponseEntity.ok(dashboardService.getEmployeeDashboard(employeeId));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/monthly-stats/{employeeId}")
    public MonthlyStatsResponse getMonthlyStats(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return dashboardService.getMonthlyStats(employeeId, year, month);
    }

    // ✅ FIXED: renamed from /manager/{managerId} to /manager/summary/{managerId}
    // Old: @GetMapping("/manager/{managerId}") -- conflicted with /manager/team-balances/{id}
    @GetMapping("/manager/summary/{managerId}")
    public ResponseEntity<ManagerDashboardResponse> getManagerDashboard(
            @PathVariable Long managerId) {
        log.info("[API] GET manager dashboard: {}", managerId);
        try {
            return ResponseEntity.ok(dashboardService.getManagerDashboard(managerId));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/hr")
    public ResponseEntity<HRDashboardResponse> getHRDashboard() {
        log.info("[API] GET HR dashboard");
        try {
            return ResponseEntity.ok(dashboardService.getHRDashboard());
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(
            @PathVariable Long adminId) {
        log.info("[API] GET admin dashboard: {}", adminId);
        try {
            return ResponseEntity.ok(dashboardService.getAdminDashboard(adminId));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/leave-counts/{employeeId}")
    public ResponseEntity<Map<LeaveStatus, Long>> getLeaveCountsByStatus(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {
        log.info("[API] GET leave counts: employee={}, year={}", employeeId, year);
        try {
            return ResponseEntity.ok(dashboardService.getLeaveCountsByStatus(employeeId, year));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/manager/team-balances/{managerId}")
    public ResponseEntity<List<TeamMemberBalance>> getTeamBalances(
            @PathVariable Long managerId,
            @RequestParam Integer year) {
        log.info("[API] GET team balances: manager={}, year={}", managerId, year);
        try {
            return ResponseEntity.ok(dashboardService.getTeamBalances(managerId, year));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/manager/pending-count/{managerId}")
    public ResponseEntity<Integer> getPendingCount(@PathVariable Long managerId) {
        log.info("[API] GET pending count: manager={}", managerId);
        try {
            return ResponseEntity.ok(dashboardService.getPendingTeamRequestsCount(managerId));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ FIXED: now returns DTO list, not raw LeaveApplication entities
    @GetMapping("/manager/pending-requests/{managerId}")
    public ResponseEntity<List<ManagerDashboardResponse.TeamPendingLeaveDTO>> getPendingRequests(
            @PathVariable Long managerId) {
        log.info("[API] GET pending requests: manager={}", managerId);
        try {
            List<ManagerDashboardResponse.TeamPendingLeaveDTO> requests =
                    dashboardService.getPendingTeamRequestDTOs(managerId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ FIXED: returns EmployeeSummaryDTO, not raw Employee entity
    @GetMapping("/hr/on-leave")
    public ResponseEntity<List<EmployeeSummaryDTO>> getEmployeesOnLeave() {
        log.info("[API] GET employees on leave");
        try {
            return ResponseEntity.ok(dashboardService.getEmployeesCurrentlyOnLeaveDTOs());
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ FIXED: returns EmployeeSummaryDTO, not raw Employee entity
    @GetMapping("/hr/managers-upcoming-leave")
    public ResponseEntity<List<EmployeeSummaryDTO>> getManagersWithUpcomingLeave() {
        log.info("[API] GET managers with upcoming leave");
        try {
            return ResponseEntity.ok(dashboardService.getManagersWithUpcomingLeaveDTOs());
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ FIXED: returns EmployeeSummaryDTO, not raw Employee entity
    @GetMapping("/hr/admins-upcoming-leave")
    public ResponseEntity<List<EmployeeSummaryDTO>> getAdminsWithUpcomingLeave() {
        log.info("[API] GET admins with upcoming leave");
        try {
            return ResponseEntity.ok(dashboardService.getAdminsWithUpcomingLeaveDTOs());
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ── MANAGER ──────────────────────────────────────────

    @GetMapping("/manager/team-on-leave/{managerId}")
    public ResponseEntity<List<TeamMemberBalance>> getTeamMembersOnLeaveToday(
            @PathVariable Long managerId) {
        return ResponseEntity.ok(dashboardService.getTeamMembersOnLeaveToday(managerId));
    }

    @GetMapping("/manager/team-calendar/{managerId}")
    public ResponseEntity<Map<String, List<TeamMemberBalance>>> getTeamLeaveCalendar(
            @PathVariable Long managerId) {
        return ResponseEntity.ok(dashboardService.getTeamLeaveCalendar(managerId));
    }

// ── HR ───────────────────────────────────────────────

    @GetMapping("/hr/company-stats")
    public ResponseEntity<Map<String, Object>> getCompanyWideStats(
            @RequestParam Integer year) {
        return ResponseEntity.ok(dashboardService.getCompanyWideStats(year));
    }

    @GetMapping("/hr/low-balance")
    public ResponseEntity<List<TeamMemberBalance>> getEmployeesWithLowBalance(
            @RequestParam Integer year,
            @RequestParam(defaultValue = "5.0") Double threshold) {
        return ResponseEntity.ok(dashboardService.getEmployeesWithLowBalance(year, threshold));
    }

    @GetMapping("/hr/high-lop")
    public ResponseEntity<List<TeamMemberBalance>> getEmployeesWithHighLOP(
            @RequestParam Integer year,
            @RequestParam(defaultValue = "5.0") Double threshold) {
        return ResponseEntity.ok(dashboardService.getEmployeesWithHighLOP(year, threshold));
    }
    // ── ADMIN ────────────────────────────────────────────
    @GetMapping("/admin/carry-forward-eligible")
    public ResponseEntity<List<TeamMemberBalance>> getCarryForwardEligible(
            @RequestParam Integer year) {
        return ResponseEntity.ok(dashboardService.getCarryForwardEligible(year));
    }

    @GetMapping("/admin/exceeding-monthly-limit")
    public ResponseEntity<List<TeamMemberBalance>> getEmployeesExceedingMonthlyLimit(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(dashboardService.getEmployeesExceedingMonthlyLimit(year, month));
    }
}