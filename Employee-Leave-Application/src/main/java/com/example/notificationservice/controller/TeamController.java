package com.example.notificationservice.controller;

import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.service.TeamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/leaves/{managerId}")
    public List<LeaveApplication> getTeamLeaves(@PathVariable Long managerId) {
        return teamService.getTeamLeaves(managerId);
    }
}
