package com.example.employeeLeaveApplication.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Manager Dashboard Response
 * Shows manager's own stats + team pending leaves
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponse {

    // ═══════════════════════════════════════════════════════════════
    // MANAGER'S OWN STATS (Same as Employee Dashboard)
    // ═══════════════════════════════════════════════════════════════

    private Long managerId;
    private String managerName;
    private Integer currentYear;

    // Yearly stats
    private Double yearlyAllocated;
    private Double yearlyUsed;
    private Double yearlyBalance;

    // Monthly stats
    private Double monthlyAllocated;
    private Double monthlyUsed;
    private Double monthlyBalance;

    // Carry forward & CompOff balances
    private Double carryForwardBalance;
    private Double compOffBalance;
    private Double lossOfPayPercentage;

    // Leave counts by status
    private Integer approvedCount;
    private Integer pendingCount;
    private Integer rejectedCount;

    // ═══════════════════════════════════════════════════════════════
    // TEAM METRICS
    // ═══════════════════════════════════════════════════════════════

    private Integer teamSize;
    private Integer teamPendingRequestCount;
    private Integer teamOnLeaveCount;

    // ═══════════════════════════════════════════════════════════════
    // TEAM PENDING REQUESTS (for quick action)
    // ═══════════════════════════════════════════════════════════════

    private List<TeamPendingLeaveDTO> pendingTeamRequests;

    // ═══════════════════════════════════════════════════════════════
    // TEAM MEMBERS ON LEAVE TODAY
    // ═══════════════════════════════════════════════════════════════

    private List<TeamMemberOnLeaveDTO> teamOnLeaveToday;

    private LocalDateTime lastUpdated;

    /**
     * Nested DTO for pending team leaves
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
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
        private Double days;
        private java.time.LocalDateTime appliedAt;
    }

    /**
     * Nested DTO for team members on leave
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberOnLeaveDTO {
        private Long employeeId;
        private String employeeName;
        private String leaveType;
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
        private Double daysRemaining;
    }
}