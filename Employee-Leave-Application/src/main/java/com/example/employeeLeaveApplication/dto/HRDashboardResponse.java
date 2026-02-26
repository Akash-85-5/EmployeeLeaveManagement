package com.example.employeeLeaveApplication.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.employeeLeaveApplication.enums.BiometricVpnStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HR Dashboard Response
 * Company-wide leave management overview
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRDashboardResponse {

    private Integer currentYear;
    private LocalDateTime lastUpdated;

    // ═══════════════════════════════════════════════════════════════
    // COMPANY-WIDE METRICS
    // ═══════════════════════════════════════════════════════════════

    private Integer totalEmployees;
    private Integer activeEmployees;
    private Integer employeesOnLeaveToday;
    private Integer totalPendingLeaves;
    private Integer totalApprovedLeaves;

    // ═══════════════════════════════════════════════════════════════
    // ONBOARDING STATUS (For HR compliance)
    // ═══════════════════════════════════════════════════════════════

    private Integer newEmployeesCount;
    private Integer pendingBiometricCount;
    private Integer pendingVPNCount;
    private List<OnboardingPendingDTO> onboardingPendingList;

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEES CURRENTLY ON LEAVE
    // ═══════════════════════════════════════════════════════════════

    private List<EmployeeOnLeaveDTO> employeesOnLeave;

    // ═══════════════════════════════════════════════════════════════
    // MANAGER INSIGHTS (Leaves approved by managers)
    // ═══════════════════════════════════════════════════════════════

    private Integer totalManagersWithApprovals;
    private List<ManagerApprovalStatsDTO> managerApprovalStats;

    // ═══════════════════════════════════════════════════════════════
    // TEAM STRUCTURE (For onboarding & compliance)
    // ═══════════════════════════════════════════════════════════════

    private List<TeamStructureDTO> teamStructure;

    /**
     * Nested DTO for onboarding pending employees
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingPendingDTO {
        private Long employeeId;
        private String employeeName;
        private String email;
        private LocalDate joiningDate;
        private BiometricVpnStatus biometricStatus; // PENDING, COMPLETED
        private BiometricVpnStatus vpnStatus;       // PENDING, COMPLETED
        private Integer daysInOnboarding; // How many days since joining
    }

    /**
     * Nested DTO for employees currently on leave
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeOnLeaveDTO {
        private Long employeeId;
        private String employeeName;
        private String managerName;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double totalDays;
        private LocalDate approvedAt;
        private String approverName; // Manager or HR who approved
    }

    /**
     * Nested DTO for manager approval statistics
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerApprovalStatsDTO {
        private Long managerId;
        private String managerName;
        private Integer teamSize;
        private Integer approvalsThisYear;
        private Integer pendingRequests;
        private Integer approvalRate; // percentage
        private LocalDate lastApprovalDate;
    }

    /**
     * Nested DTO for team structure (Manager + team members)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamStructureDTO {
        private Long managerId;
        private String managerName;
        private Integer teamMemberCount;
        private List<TeamMemberDTO> teamMembers; // List of team members under this manager
    }

    /**
     * Nested DTO for individual team member
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberDTO {
        private Long employeeId;
        private String employeeName;
        private String email;
        private Double yearlyBalance;
        private Double carryForwardBalance;
        private Double compOffBalance;
    }
}