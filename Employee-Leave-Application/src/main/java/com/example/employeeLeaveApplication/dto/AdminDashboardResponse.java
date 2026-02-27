package com.example.employeeLeaveApplication.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.employeeLeaveApplication.enums.BiometricVpnStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin Dashboard Response
 * System-wide compliance and audit view
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private Integer currentYear;
    private LocalDateTime lastUpdated;

    // ═══════════════════════════════════════════════════════════════
    // ADMIN'S OWN STATS
    // ═══════════════════════════════════════════════════════════════

    private Long adminId;
    private String adminName;
    private Double yearlyBalance;
    private Double carryForwardBalance;
    private Double compOffBalance;
    private Integer approvedLeaveCount;
    private Integer pendingLeaveCount;

    // ═══════════════════════════════════════════════════════════════
    // COMPLIANCE & AUDIT METRICS
    // ═══════════════════════════════════════════════════════════════

    private Integer totalEmployees;
    private Integer totalManagers;
    private Integer newEmployeesPendingOnboarding;
    private Integer totalPendingLeaves;
    private Integer totalRejectedLeaves;

    // ═══════════════════════════════════════════════════════════════
    // LEAVE STATISTICS
    // ═══════════════════════════════════════════════════════════════

    private Double totalLeaveDaysUsedYTD;
    private Double totalCarryForwardBalance;
    private Double totalCompOffBalance;
    private Double averageLossOfPayPercentage;

    // ═══════════════════════════════════════════════════════════════
    // LEAVE TYPE BREAKDOWN
    // ═══════════════════════════════════════════════════════════════

    private List<LeaveTypeUsageDTO> leaveTypeUsage;

    // ═══════════════════════════════════════════════════════════════
    // COMPLIANCE REPORTS
    // ═══════════════════════════════════════════════════════════════

    private List<RejectedLeaveAuditDTO> recentRejections;
    private List<EmployeeComplianceDTO> complianceIssues;
    private List<OnboardingStatusDTO> newEmployeesStatus;

    /**
     * Nested DTO for leave type usage breakdown
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveTypeUsageDTO {
        private String leaveType; // VACATION, SICK, CASUAL, PERSONAL, COMP_OFF
        private Double totalAllocated;
        private Double totalUsed;
        private Double totalBalance;
        private Integer countOfApplications;
        private Double averagePerEmployee;
    }

    /**
     * Nested DTO for rejected leave audit trail
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedLeaveAuditDTO {
        private Long leaveId;
        private Long employeeId;
        private String employeeName;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
        private Long rejectedBy;
        private String rejectedByName;
        private LocalDateTime rejectedAt;
    }

    /**
     * Nested DTO for employee compliance issues
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeComplianceDTO {
        private Long employeeId;
        private String employeeName;
        private String issue; // e.g., "Loss of Pay Exceeded 5%", "Negative Balance Detected"
        private String severity; // LOW, MEDIUM, HIGH
        private LocalDate detectedDate;
        private String recommendation;
    }

    /**
     * Nested DTO for new employees onboarding status
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingStatusDTO {
        private Long employeeId;
        private String employeeName;
        private String email;
        private LocalDate joiningDate;
        private Integer daysInCompany;
        private BiometricVpnStatus biometricStatus;
        private BiometricVpnStatus vpnStatus;
        private Boolean onboardingComplete;
        private LocalDateTime completionDate;
    }
}