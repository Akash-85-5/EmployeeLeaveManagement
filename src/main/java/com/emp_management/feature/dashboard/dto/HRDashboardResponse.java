package com.emp_management.feature.dashboard.dto;

import com.emp_management.shared.enums.BiometricVpnStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public class HRDashboardResponse {

    private Integer currentYear;
    private LocalDateTime lastUpdated;
    private Integer totalEmployees;
    private Integer activeEmployees;
    private Integer employeesOnLeaveToday;
    private Integer totalPendingLeaves;
    private Integer totalApprovedLeaves;
    private Integer newEmployeesCount;
    private Integer pendingBiometricCount;
    private Integer pendingVPNCount;
    private List<OnboardingPendingDTO> onboardingPendingList;
    private List<EmployeeOnLeaveDTO> employeesOnLeave;
    private Integer totalManagersWithApprovals;
    private List<ManagerApprovalStatsDTO> managerApprovalStats;
    private List<TeamStructureDTO> teamStructure;

    // ── Getters & Setters ─────────────────────────────────────────

    public Integer getCurrentYear() { return currentYear; }
    public void setCurrentYear(Integer currentYear) { this.currentYear = currentYear; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public Integer getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(Integer totalEmployees) { this.totalEmployees = totalEmployees; }

    public Integer getActiveEmployees() { return activeEmployees; }
    public void setActiveEmployees(Integer activeEmployees) { this.activeEmployees = activeEmployees; }

    public Integer getEmployeesOnLeaveToday() { return employeesOnLeaveToday; }
    public void setEmployeesOnLeaveToday(Integer employeesOnLeaveToday) { this.employeesOnLeaveToday = employeesOnLeaveToday; }

    public Integer getTotalPendingLeaves() { return totalPendingLeaves; }
    public void setTotalPendingLeaves(Integer totalPendingLeaves) { this.totalPendingLeaves = totalPendingLeaves; }

    public Integer getTotalApprovedLeaves() { return totalApprovedLeaves; }
    public void setTotalApprovedLeaves(Integer totalApprovedLeaves) { this.totalApprovedLeaves = totalApprovedLeaves; }

    public Integer getNewEmployeesCount() { return newEmployeesCount; }
    public void setNewEmployeesCount(Integer newEmployeesCount) { this.newEmployeesCount = newEmployeesCount; }

    public Integer getPendingBiometricCount() { return pendingBiometricCount; }
    public void setPendingBiometricCount(Integer pendingBiometricCount) { this.pendingBiometricCount = pendingBiometricCount; }

    public Integer getPendingVPNCount() { return pendingVPNCount; }
    public void setPendingVPNCount(Integer pendingVPNCount) { this.pendingVPNCount = pendingVPNCount; }

    public List<OnboardingPendingDTO> getOnboardingPendingList() { return onboardingPendingList; }
    public void setOnboardingPendingList(List<OnboardingPendingDTO> onboardingPendingList) { this.onboardingPendingList = onboardingPendingList; }

    public List<EmployeeOnLeaveDTO> getEmployeesOnLeave() { return employeesOnLeave; }
    public void setEmployeesOnLeave(List<EmployeeOnLeaveDTO> employeesOnLeave) { this.employeesOnLeave = employeesOnLeave; }

    public Integer getTotalManagersWithApprovals() { return totalManagersWithApprovals; }
    public void setTotalManagersWithApprovals(Integer totalManagersWithApprovals) { this.totalManagersWithApprovals = totalManagersWithApprovals; }

    public List<ManagerApprovalStatsDTO> getManagerApprovalStats() { return managerApprovalStats; }
    public void setManagerApprovalStats(List<ManagerApprovalStatsDTO> managerApprovalStats) { this.managerApprovalStats = managerApprovalStats; }

    public List<TeamStructureDTO> getTeamStructure() { return teamStructure; }
    public void setTeamStructure(List<TeamStructureDTO> teamStructure) { this.teamStructure = teamStructure; }

    // ── Nested DTOs ───────────────────────────────────────────────

    public static class OnboardingPendingDTO {
        private String employeeId;        // was Long
        private String employeeName;
        private String email;
        private LocalDate joiningDate;
        private BiometricVpnStatus biometricStatus;
        private BiometricVpnStatus vpnStatus;
        private Integer daysInOnboarding;

        public OnboardingPendingDTO() {}
        public OnboardingPendingDTO(String employeeId, String employeeName, String email,
                                    LocalDate joiningDate, BiometricVpnStatus biometricStatus,
                                    BiometricVpnStatus vpnStatus, Integer daysInOnboarding) {
            this.employeeId      = employeeId;
            this.employeeName    = employeeName;
            this.email           = email;
            this.joiningDate     = joiningDate;
            this.biometricStatus = biometricStatus;
            this.vpnStatus       = vpnStatus;
            this.daysInOnboarding = daysInOnboarding;
        }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public LocalDate getJoiningDate() { return joiningDate; }
        public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
        public BiometricVpnStatus getBiometricStatus() { return biometricStatus; }
        public void setBiometricStatus(BiometricVpnStatus biometricStatus) { this.biometricStatus = biometricStatus; }
        public BiometricVpnStatus getVpnStatus() { return vpnStatus; }
        public void setVpnStatus(BiometricVpnStatus vpnStatus) { this.vpnStatus = vpnStatus; }
        public Integer getDaysInOnboarding() { return daysInOnboarding; }
        public void setDaysInOnboarding(Integer daysInOnboarding) { this.daysInOnboarding = daysInOnboarding; }
    }

    public static class EmployeeOnLeaveDTO {
        private String employeeId;        // was Long
        private String employeeName;
        private String managerName;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double totalDays;
        private LocalDate approvedAt;
        private String approverName;

        public EmployeeOnLeaveDTO() {}
        public EmployeeOnLeaveDTO(String employeeId, String employeeName, String managerName,
                                  String leaveType, LocalDate startDate, LocalDate endDate,
                                  Double totalDays, LocalDate approvedAt, String approverName) {
            this.employeeId   = employeeId;
            this.employeeName = employeeName;
            this.managerName  = managerName;
            this.leaveType    = leaveType;
            this.startDate    = startDate;
            this.endDate      = endDate;
            this.totalDays    = totalDays;
            this.approvedAt   = approvedAt;
            this.approverName = approverName;
        }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }
        public String getLeaveType() { return leaveType; }
        public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public Double getTotalDays() { return totalDays; }
        public void setTotalDays(Double totalDays) { this.totalDays = totalDays; }
        public LocalDate getApprovedAt() { return approvedAt; }
        public void setApprovedAt(LocalDate approvedAt) { this.approvedAt = approvedAt; }
        public String getApproverName() { return approverName; }
        public void setApproverName(String approverName) { this.approverName = approverName; }
    }

    public static class ManagerApprovalStatsDTO {
        private String managerId;         // was Long
        private String managerName;
        private Integer teamSize;
        private Integer approvalsThisYear;
        private Integer pendingRequests;
        private Integer approvalRate;
        private LocalDate lastApprovalDate;

        public ManagerApprovalStatsDTO() {}
        public ManagerApprovalStatsDTO(String managerId, String managerName, Integer teamSize,
                                       Integer approvalsThisYear, Integer pendingRequests,
                                       Integer approvalRate, LocalDate lastApprovalDate) {
            this.managerId         = managerId;
            this.managerName       = managerName;
            this.teamSize          = teamSize;
            this.approvalsThisYear = approvalsThisYear;
            this.pendingRequests   = pendingRequests;
            this.approvalRate      = approvalRate;
            this.lastApprovalDate  = lastApprovalDate;
        }

        public String getManagerId() { return managerId; }
        public void setManagerId(String managerId) { this.managerId = managerId; }
        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }
        public Integer getTeamSize() { return teamSize; }
        public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }
        public Integer getApprovalsThisYear() { return approvalsThisYear; }
        public void setApprovalsThisYear(Integer approvalsThisYear) { this.approvalsThisYear = approvalsThisYear; }
        public Integer getPendingRequests() { return pendingRequests; }
        public void setPendingRequests(Integer pendingRequests) { this.pendingRequests = pendingRequests; }
        public Integer getApprovalRate() { return approvalRate; }
        public void setApprovalRate(Integer approvalRate) { this.approvalRate = approvalRate; }
        public LocalDate getLastApprovalDate() { return lastApprovalDate; }
        public void setLastApprovalDate(LocalDate lastApprovalDate) { this.lastApprovalDate = lastApprovalDate; }
    }

    public static class TeamStructureDTO {
        private String managerId;         // was Long
        private String managerName;
        private Integer teamMemberCount;
        private List<TeamMemberDTO> teamMembers;

        public TeamStructureDTO() {}
        public TeamStructureDTO(String managerId, String managerName,
                                Integer teamMemberCount, List<TeamMemberDTO> teamMembers) {
            this.managerId       = managerId;
            this.managerName     = managerName;
            this.teamMemberCount = teamMemberCount;
            this.teamMembers     = teamMembers;
        }

        public String getManagerId() { return managerId; }
        public void setManagerId(String managerId) { this.managerId = managerId; }
        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }
        public Integer getTeamMemberCount() { return teamMemberCount; }
        public void setTeamMemberCount(Integer teamMemberCount) { this.teamMemberCount = teamMemberCount; }
        public List<TeamMemberDTO> getTeamMembers() { return teamMembers; }
        public void setTeamMembers(List<TeamMemberDTO> teamMembers) { this.teamMembers = teamMembers; }
    }

    public static class TeamMemberDTO {
        private String employeeId;        // was Long
        private String employeeName;
        private String email;
        private Double yearlyBalance;
        private Double carryForwardBalance;
        private Double compOffBalance;

        public TeamMemberDTO() {}
        public TeamMemberDTO(String employeeId, String employeeName, String email,
                             Double yearlyBalance, Double carryForwardBalance, Double compOffBalance) {
            this.employeeId         = employeeId;
            this.employeeName       = employeeName;
            this.email              = email;
            this.yearlyBalance      = yearlyBalance;
            this.carryForwardBalance = carryForwardBalance;
            this.compOffBalance     = compOffBalance;
        }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Double getYearlyBalance() { return yearlyBalance; }
        public void setYearlyBalance(Double yearlyBalance) { this.yearlyBalance = yearlyBalance; }
        public Double getCarryForwardBalance() { return carryForwardBalance; }
        public void setCarryForwardBalance(Double carryForwardBalance) { this.carryForwardBalance = carryForwardBalance; }
        public Double getCompOffBalance() { return compOffBalance; }
        public void setCompOffBalance(Double compOffBalance) { this.compOffBalance = compOffBalance; }
    }
}