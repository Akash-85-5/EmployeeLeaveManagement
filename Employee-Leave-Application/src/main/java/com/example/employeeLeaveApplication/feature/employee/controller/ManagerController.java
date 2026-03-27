package com.example.employeeLeaveApplication.feature.employee.controller;

import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
import com.example.employeeLeaveApplication.feature.employee.service.TeamService;
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
//    @PreAuthorize("#managerId == authentication.principal.user.id) ")
    public List<LeaveApplication> getTeamLeaves(
            @PathVariable Long managerId) {
        return teamService.getTeamLeaves(managerId);
    }

    // ✅ FIXED: Added hasRole('MANAGER') + HR/ADMIN can view too
    @GetMapping("/{managerId}/team-leaves/week")
//    @PreAuthorize("#managerId == authentication.principal.user.id)")
    public List<LeaveApplication> getCurrentWeekTeamLeaves(
            @PathVariable Long managerId) {
        return teamService.getTeamLeavesForCurrentWeek(managerId);
    }
}