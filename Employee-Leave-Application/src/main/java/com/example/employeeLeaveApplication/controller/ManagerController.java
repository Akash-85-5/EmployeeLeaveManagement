package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.service.TeamService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final TeamService teamService;

    public ManagerController(TeamService teamService) {
        this.teamService = teamService;
    }

    // ✅ FIXED: Added hasRole('MANAGER') + HR/ADMIN can view too
    @GetMapping("/{managerId}/team-leaves")
    @PreAuthorize("(hasRole('MANAGER') and #managerId == authentication.principal.user.id) " +
            "or hasRole('HR') or hasRole('ADMIN')")
    public List<LeaveApplication> getTeamLeaves(
            @PathVariable Long managerId) {
        return teamService.getTeamLeaves(managerId);
    }

    // ✅ FIXED: Added hasRole('MANAGER') + HR/ADMIN can view too
    @GetMapping("/{managerId}/team-leaves/week")
    @PreAuthorize("(hasAnyRole('MANAGER','TEAM_LEADER') and #managerId == authentication.principal.user.id) " +
            "or hasRole('HR') or hasRole('ADMIN')")
    public List<LeaveApplication> getCurrentWeekTeamLeaves(
            @PathVariable Long managerId) {
        return teamService.getTeamLeavesForCurrentWeek(managerId);
    }
}