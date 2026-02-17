package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.service.TeamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
public class
ManagerController {

    private final TeamService teamService;

    public ManagerController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/{managerId}/team-leaves")
    public List<LeaveApplication> getTeamLeaves(
            @PathVariable Long managerId
    ) {
        return teamService.getTeamLeaves(managerId);
    }
    @GetMapping("/{managerId}/team-leaves/week")
    public List<LeaveApplication> getCurrentWeekTeamLeaves(
            @PathVariable Long managerId
    ) {
        return teamService.getTeamLeavesForCurrentWeek(managerId);
    }

}
