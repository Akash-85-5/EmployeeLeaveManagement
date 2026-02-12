
package com.example.notificationservice.service;

import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.repository.LeaveApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class TeamService {

    private final LeaveApplicationRepository leaveRepo;

    public TeamService(LeaveApplicationRepository leaveRepo) {
        this.leaveRepo = leaveRepo;
    }

    // ✅ full team visibility
    public List<LeaveApplication> getTeamLeaves(Long managerId) {
        return leaveRepo.findByManagerId(managerId);
    }

    // ✅ current week visibility
    public List<LeaveApplication> getTeamLeavesForCurrentWeek(Long managerId) {

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);

        return leaveRepo.findTeamLeavesForWeek(
                managerId,
                weekStart,
                weekEnd
        );
    }
}
