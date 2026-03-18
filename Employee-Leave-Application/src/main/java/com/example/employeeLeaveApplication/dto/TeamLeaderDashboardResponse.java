package com.example.employeeLeaveApplication.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Team Leader Dashboard Response
 *
 * Shows:
 *  1. Team leader's own personal leave stats
 *  2. Team metrics (size, pending count, on-leave count)
 *  3. Pending leave requests from team members (TL approves first)
 *  4. Team members on leave today
 *  5. Team leave balance summary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamLeaderDashboardResponse {

    // ═══════════════════════════════════════════════════════════════
    // TEAM LEADER'S OWN STATS (reuses EmployeeDashboardResponse)
    // ═══════════════════════════════════════════════════════════════

    private EmployeeDashboardResponse personalStats;

    // ═══════════════════════════════════════════════════════════════
    // TEAM METRICS — Quick summary counts
    // ═══════════════════════════════════════════════════════════════

    private Integer teamSize;
    private Integer teamPendingRequestCount;  // Awaiting TL approval
    private Integer teamOnLeaveCount;         // On leave today

    // ═══════════════════════════════════════════════════════════════
    // PENDING TEAM LEAVE REQUESTS
    // These are PENDING leaves waiting for Team Leader's approval (first level)
    // ═══════════════════════════════════════════════════════════════

    private List<TeamPendingLeaveDTO> pendingTeamRequests;

    // ═══════════════════════════════════════════════════════════════
    // TEAM MEMBERS ON LEAVE TODAY
    // ═══════════════════════════════════════════════════════════════

    private List<TeamMemberOnLeaveDTO> teamOnLeaveToday;

    // ═══════════════════════════════════════════════════════════════
    // TEAM LEAVE BALANCE SUMMARY — per member
    // ═══════════════════════════════════════════════════════════════

    private List<TeamMemberBalanceSummaryDTO> teamBalances;

    private LocalDateTime lastUpdated;

    // ═══════════════════════════════════════════════════════════════
    // NESTED DTOs
    // ═══════════════════════════════════════════════════════════════

    /**
     * Pending leave request from a team member
     * waiting for Team Leader's first-level approval
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamPendingLeaveDTO {
        private Long leaveId;
        private Long employeeId;
        private String employeeName;
        private LeaveType leaveType;
        private String reason;
        private LeaveStatus status;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double days;
        private LocalDateTime appliedAt;
    }

    /**
     * Team member currently on leave today
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberOnLeaveDTO {
        private Long employeeId;
        private String employeeName;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double daysRemaining;
    }

    /**
     * Leave balance summary per team member
     * Useful for planning & coverage decisions
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberBalanceSummaryDTO {
        private Long employeeId;
        private String employeeName;
        private Double totalAllocated;
        private Double totalUsed;
        private Double totalRemaining;
        private Double compOffBalance;
        private Double lopPercentage;
    }
}