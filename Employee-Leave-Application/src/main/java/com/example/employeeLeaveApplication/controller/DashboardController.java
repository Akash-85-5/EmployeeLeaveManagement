package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.EmployeeDashboardResponse;
import com.example.employeeLeaveApplication.dto.MonthlyStatsResponse;
import com.example.employeeLeaveApplication.dto.TeamMemberBalance;
import com.example.employeeLeaveApplication.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")

public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService){
        this.dashboardService=dashboardService;
    }

    @GetMapping("/employee/{employeeId}")
    public EmployeeDashboardResponse getDashboard(
            @PathVariable Long employeeId) {
        return dashboardService.getDashboard(employeeId);
    }


    @GetMapping("/monthly-stats/{employeeId}")
    public MonthlyStatsResponse getMonthlyStats(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return dashboardService.getMonthlyStats(employeeId, year, month);
    }

    @GetMapping("/team-balances/{managerId}")
    public List<TeamMemberBalance> getTeamBalances(
            @PathVariable Long managerId,
            @RequestParam Integer year
    ) {
        return dashboardService.getTeamBalances(managerId, year);
    }

    @GetMapping("/pending-count/{managerId}")
    public int getPendingCount(@PathVariable Long managerId) {
        return dashboardService.getPendingCount(managerId);
    }


}
