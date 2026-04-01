package com.emp_management.feature.dashboard.dto;

import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.shared.enums.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ManagerDashboardResponse {

    private EmployeeDashboardResponse personalStats;
    private Integer teamSize;
    private Integer teamPendingRequestCount;
    private Integer teamOnLeaveCount;
    private List<TeamPendingLeaveDTO> pendingTeamRequests;
    private List<TeamMemberOnLeaveDTO> teamOnLeaveToday;
    private LocalDateTime lastUpdated;

    // ── Getters & Setters ─────────────────────────────────────────

    public EmployeeDashboardResponse getPersonalStats() { return personalStats; }
    public void setPersonalStats(EmployeeDashboardResponse personalStats) { this.personalStats = personalStats; }

    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }

    public Integer getTeamPendingRequestCount() { return teamPendingRequestCount; }
    public void setTeamPendingRequestCount(Integer teamPendingRequestCount) { this.teamPendingRequestCount = teamPendingRequestCount; }

    public Integer getTeamOnLeaveCount() { return teamOnLeaveCount; }
    public void setTeamOnLeaveCount(Integer teamOnLeaveCount) { this.teamOnLeaveCount = teamOnLeaveCount; }

    public List<TeamPendingLeaveDTO> getPendingTeamRequests() { return pendingTeamRequests; }
    public void setPendingTeamRequests(List<TeamPendingLeaveDTO> pendingTeamRequests) { this.pendingTeamRequests = pendingTeamRequests; }

    public List<TeamMemberOnLeaveDTO> getTeamOnLeaveToday() { return teamOnLeaveToday; }
    public void setTeamOnLeaveToday(List<TeamMemberOnLeaveDTO> teamOnLeaveToday) { this.teamOnLeaveToday = teamOnLeaveToday; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    // ── Nested DTOs ───────────────────────────────────────────────

    public static class TeamPendingLeaveDTO {
        private Long leaveId;
        private String employeeId;        // was Long
        private String employeeName;
        private String leaveType;         // was LeaveType enum — now String name
        private String reason;
        private RequestStatus status;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double days;
        private LocalDateTime appliedAt;

        public TeamPendingLeaveDTO() {}

        public TeamPendingLeaveDTO(Long leaveId, String employeeId, String employeeName,
                                   String leaveType, String reason, RequestStatus status,
                                   LocalDate startDate, LocalDate endDate,
                                   Double days, LocalDateTime appliedAt) {
            this.leaveId      = leaveId;
            this.employeeId   = employeeId;
            this.employeeName = employeeName;
            this.leaveType    = leaveType;
            this.reason       = reason;
            this.status       = status;
            this.startDate    = startDate;
            this.endDate      = endDate;
            this.days         = days;
            this.appliedAt    = appliedAt;
        }

        public Long getLeaveId() { return leaveId; }
        public void setLeaveId(Long leaveId) { this.leaveId = leaveId; }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

        public String getLeaveType() { return leaveType; }
        public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public RequestStatus getStatus() { return status; }
        public void setStatus(RequestStatus status) { this.status = status; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public Double getDays() { return days; }
        public void setDays(Double days) { this.days = days; }

        public LocalDateTime getAppliedAt() { return appliedAt; }
        public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    }

    public static class TeamMemberOnLeaveDTO {
        private String employeeId;        // was Long
        private String employeeName;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double daysRemaining;

        public TeamMemberOnLeaveDTO() {}

        public TeamMemberOnLeaveDTO(String employeeId, String employeeName,
                                    String leaveType, LocalDate startDate,
                                    LocalDate endDate, Double daysRemaining) {
            this.employeeId   = employeeId;
            this.employeeName = employeeName;
            this.leaveType    = leaveType;
            this.startDate    = startDate;
            this.endDate      = endDate;
            this.daysRemaining = daysRemaining;
        }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

        public String getLeaveType() { return leaveType; }
        public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public Double getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(Double daysRemaining) { this.daysRemaining = daysRemaining; }
    }
}