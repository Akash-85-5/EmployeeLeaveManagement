package com.emp_management.feature.dashboard.controller;


import com.emp_management.feature.dashboard.dto.*;
import com.emp_management.feature.dashboard.service.DashboardService;
import com.emp_management.feature.leave.annual.dto.LeaveApplicationResponseDTO;
import com.emp_management.shared.enums.RequestStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ── EMPLOYEE ──────────────────────────────────────────────────

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(
            @PathVariable String employeeId) {
        log.info("[API] GET employee dashboard: {}", employeeId);
        try {
            return ResponseEntity.ok(
                    dashboardService.getEmployeeDashboard(employeeId));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/monthly-stats/{employeeId}")
    public MonthlyStatsResponse getMonthlyStats(
            @PathVariable String employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return dashboardService.getMonthlyStats(employeeId, year, month);
    }

    @GetMapping("/employee/calendar/{employeeId}")
    public ResponseEntity<Map<String, List<LeaveApplicationResponseDTO>>> getEmployeeLeaveCalendar(
            @PathVariable String employeeId) {
        return ResponseEntity.ok(
                dashboardService.getMyLeaveCalendar(employeeId));
    }
    // ✅ UPDATED: Added @PreAuthorize — was missing before
    @GetMapping("/leave-counts/{employeeId}")
    public ResponseEntity<Map<RequestStatus, Long>> getLeaveCountsByStatus(
            @PathVariable String employeeId,
            @RequestParam Integer year) {
        log.info("[API] GET leave counts: employee={}, year={}",
                employeeId, year);
        try {
            return ResponseEntity.ok(
                    dashboardService.getLeaveCountsByStatus(
                            employeeId, year));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ── MANAGER ──────────────────────────────────────────────────

    @GetMapping("/manager/summary/{managerId}")
    public ResponseEntity<ManagerDashboardResponse> getManagerDashboard(
            @PathVariable String managerId) {
        log.info("[API] GET manager dashboard: {}", managerId);
        try {
            return ResponseEntity.ok(
                    dashboardService.getManagerDashboard(managerId));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ UPDATED: Added hasRole('MANAGER') check
    @GetMapping("/manager/team-balances/{managerId}")
    public ResponseEntity<List<TeamMemberBalance>> getTeamBalances(
            @PathVariable String managerId,
            @RequestParam Integer year) {
        log.info("[API] GET team balances: manager={}, year={}",
                managerId, year);
        try {
            return ResponseEntity.ok(
                    dashboardService.getTeamBalances(managerId, year));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ UPDATED: Added hasRole('MANAGER') check
    @GetMapping("/manager/pending-count/{managerId}")
    public ResponseEntity<Integer> getPendingCount(
            @PathVariable String managerId) {
        log.info("[API] GET pending count: manager={}", managerId);
        try {
            return ResponseEntity.ok(
                    dashboardService.getPendingTeamRequestsCount(
                            managerId));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ UPDATED: Added hasRole('MANAGER') check
    @GetMapping("/manager/pending-requests/{managerId}")
    public ResponseEntity<List<ManagerDashboardResponse
            .TeamPendingLeaveDTO>> getPendingRequests(
            @PathVariable String managerId) {
        log.info("[API] GET pending requests: manager={}", managerId);
        try {
            return ResponseEntity.ok(
                    dashboardService.getPendingTeamRequestDTOs(
                            managerId));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/team-on-leave/{id}")
     public ResponseEntity<List<TeamMemberBalance>> getTeamMembersOnLeaveToday(
            @PathVariable String id) {
        return ResponseEntity.ok(
                dashboardService.getTeamMembersOnLeaveToday(id));
    }

    @GetMapping("/team-calendar/{id}")
<<<<<<< HEAD
    public ResponseEntity<Map<String, List<TeamMemberBalance>>> getManagerTeamLeaveCalendar(
=======
    public ResponseEntity<Map<String, List<LeaveApplicationResponseDTO>>> getManagerTeamLeaveCalendar(
>>>>>>> 523ac0f4adb5a79fa972112d40b09821ea72603b
            @PathVariable String id) {
        return ResponseEntity.ok(
                dashboardService.getTeamLeaveCalendar(id));
    }

    // ── HR ───────────────────────────────────────────────────────

//    @GetMapping("/hr")
//    @PreAuthorize("hasRole('HR')")
//    public ResponseEntity<HRDashboardResponse> getHRDashboard() {
//        log.info("[API] GET HR dashboard");
//        try {
//            return ResponseEntity.ok(dashboardService.getHRDashboard());
//        } catch (Exception e) {
//            log.error("[API] Error: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }

    @GetMapping("/hr/on-leave")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<EmployeeSummaryDTO>> getEmployeesOnLeave() {
        log.info("[API] GET employees on leave");
        try {
            return ResponseEntity.ok(
                    dashboardService.getEmployeesCurrentlyOnLeaveDTOs());
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

//    @GetMapping("/hr/managers-upcoming-leave")
//    @PreAuthorize("hasRole('HR')")
//    public ResponseEntity<List<EmployeeSummaryDTO>> getManagersWithUpcomingLeave() {
//        log.info("[API] GET managers with upcoming leave");
//        try {
//            return ResponseEntity.ok(
//                    dashboardService.getManagersWithUpcomingLeaveDTOs());
//        } catch (Exception e) {
//            log.error("[API] Error: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    @GetMapping("/hr/admins-upcoming-leave")
//    @PreAuthorize("hasRole('HR')")
//    public ResponseEntity<List<EmployeeSummaryDTO>> getAdminsWithUpcomingLeave() {
//        log.info("[API] GET admins with upcoming leave");
//        try {
//            return ResponseEntity.ok(
//                    dashboardService.getAdminsWithUpcomingLeaveDTOs());
//        } catch (Exception e) {
//            log.error("[API] Error: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }

    // ✅ UPDATED: Added hasRole('ADMIN') access too
    @GetMapping("/hr/company-stats")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCompanyWideStats(
            @RequestParam Integer year) {
        return ResponseEntity.ok(
                dashboardService.getCompanyWideStats(year));
    }
    @GetMapping("/team-members/{Id}")
    public ResponseEntity<List<TeamMember>> getTeamMembers(
            @PathVariable String Id) {
        log.info("[API] GET team balances: manager={}",
                Id);
        try {
            return ResponseEntity.ok(
                    dashboardService.getTeamMembers(Id));
        } catch (Exception e) {
            log.error("[API] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ UPDATED: Added hasRole('ADMIN') access too
    @GetMapping("/hr/low-balance")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<List<TeamMemberBalance>> getEmployeesWithLowBalance(
            @RequestParam Integer year,
            @RequestParam(defaultValue = "5.0") Double threshold) {
        return ResponseEntity.ok(
                dashboardService.getEmployeesWithLowBalance(
                        year, threshold));
    }

    // ✅ UPDATED: Added hasRole('ADMIN') access too
    @GetMapping("/hr/high-lop")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<List<TeamMemberBalance>> getEmployeesWithHighLOP(
            @RequestParam Integer year,
            @RequestParam(defaultValue = "5.0") Double threshold) {
        return ResponseEntity.ok(
                dashboardService.getEmployeesWithHighLOP(
                        year, threshold));
    }

    // ── ADMIN ────────────────────────────────────────────────────
//
//    @GetMapping("/admin/{adminId}")
//    @PreAuthorize("#adminId == authentication.principal.employeeId")
//    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(
//            @PathVariable Long adminId) {
//        log.info("[API] GET admin dashboard: {}", adminId);
//        try {
//            return ResponseEntity.ok(
//                    dashboardService.getAdminDashboard(adminId));
//        } catch (Exception e) {
//            log.error("[API] Error: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }

    @GetMapping("/admin/carry-forward-eligible")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeamMemberBalance>> getCarryForwardEligible(
            @RequestParam Integer year) {
        return ResponseEntity.ok(
                dashboardService.getCarryForwardEligible(year));
    }

    @GetMapping("/admin/exceeding-monthly-limit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeamMemberBalance>> getEmployeesExceedingMonthlyLimit(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(
                dashboardService.getEmployeesExceedingMonthlyLimit(
                        year, month));
    }

//    @GetMapping("/teamleader/{teamLeaderId}")
//    @PreAuthorize("hasRole('TEAM_LEADER') and #teamLeaderId == authentication.principal.user.id ")
//    public ResponseEntity<TeamLeaderDashboardResponse> getTeamLeaderDashboard(
//            @PathVariable Long teamLeaderId) {
//        log.info("[API] GET team leader dashboard: {}", teamLeaderId);
//        try {
//            TeamLeaderDashboardResponse response =
//                    dashboardService.getTeamLeaderDashboard(teamLeaderId);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("[API] Error: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }
}

