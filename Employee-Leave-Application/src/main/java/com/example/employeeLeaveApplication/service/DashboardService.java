package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.EmployeeDashboardResponse;
import com.example.employeeLeaveApplication.dto.LeaveBalanceResponse;
import com.example.employeeLeaveApplication.dto.MonthlyStatsResponse;
import com.example.employeeLeaveApplication.dto.TeamMemberBalance;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final LeaveApplicationRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceService leaveBalanceService;

    public DashboardService(LeaveBalanceService leaveBalanceService,
                            LeaveApplicationRepository leaveRepository,
                            EmployeeRepository employeeRepository) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.leaveBalanceService = leaveBalanceService;
    }

    // ==================== EXISTING METHODS ====================

    public EmployeeDashboardResponse getDashboard(Long employeeId) {
        LeaveBalanceResponse balance = leaveBalanceService.getBalance(
                employeeId,
                java.time.Year.now().getValue()
        );

        Map<LeaveStatus, Long> statusCount = leaveRepository.findByEmployeeId(employeeId)
                .stream()
                .collect(Collectors.groupingBy(
                        LeaveApplication::getStatus,
                        Collectors.counting()
                ));

        EmployeeDashboardResponse response = new EmployeeDashboardResponse();
        response.setLeaveBalances(balance.getBreakdown());
        response.setLeaveStatusCount(statusCount);

        return response;
    }

    public MonthlyStatsResponse getMonthlyStats(Long employeeId, Integer year, Integer month) {
        List<Object[]> stats = leaveRepository.getMonthlyStats(employeeId, year, month);

        List<MonthlyStatsResponse.LeaveTypeStat> statsList = new ArrayList<>();
        double totalDays = 0;

        for (Object[] row : stats) {
            String leaveType = row[0].toString();
            long count = ((Number) row[1]).longValue();
            double days = ((Number) row[2]).doubleValue();

            MonthlyStatsResponse.LeaveTypeStat stat = new MonthlyStatsResponse.LeaveTypeStat(
                    leaveType,
                    (int) count,
                    days
            );

            statsList.add(stat);
            totalDays += days;
        }

        long approvedCount = leaveRepository.countApprovedInMonth(employeeId, year, month);

        return new MonthlyStatsResponse(
                employeeId,
                year,
                month,
                (int) approvedCount,
                totalDays,
                approvedCount > 2,
                statsList
        );
    }

    public List<TeamMemberBalance> getTeamBalances(Long managerId, Integer year) {
        List<Employee> team = employeeRepository.findByManagerId(managerId);
        List<TeamMemberBalance> result = new ArrayList<>();

        for (Employee emp : team) {
            LeaveBalanceResponse balance = leaveBalanceService.getBalance(emp.getId(), year);

            TeamMemberBalance dto = new TeamMemberBalance();
            dto.setEmployeeId(emp.getId());
            dto.setEmployeeName(emp.getName());
            dto.setTotalAllocated(balance.getTotalAllocated());
            dto.setTotalUsed(balance.getTotalUsed());
            dto.setTotalRemaining(balance.getTotalRemaining());
            dto.setCompOffBalance(balance.getCompOffBalance());
            dto.setLopPercentage(balance.getLopPercentage());
            dto.setTotalWorkingDays(balance.getTotalWorkingDays());

            result.add(dto);
        }

        return result;
    }

    public int getPendingCount(Long managerId) {
        List<Long> teamIds = employeeRepository.findByManagerId(managerId)
                .stream()
                .map(Employee::getId)
                .toList();

        return leaveRepository.countByEmployeeIdInAndStatus(teamIds, LeaveStatus.PENDING);
    }

    // ==================== NEW METHODS ====================

    /**
     * Get upcoming approved leaves for employee
     */
    public List<LeaveApplication> getUpcomingLeaves(Long employeeId) {
        LocalDate today = LocalDate.now();
        return leaveRepository.findUpcomingLeaves(employeeId, today);
    }

    /**
     * Get recent leaves for employee (approved/rejected)
     */
    public List<LeaveApplication> getRecentLeaves(Long employeeId, int limit) {
        return leaveRepository.findRecentLeaves(employeeId, PageRequest.of(0, limit));
    }

    /**
     * Get admin statistics for entire company
     */
    public Map<String, Object> getAdminStatistics(Integer year) {
        if (year == null) {
            year = java.time.Year.now().getValue();
        }

        Map<String, Object> stats = new HashMap<>();

        // Total employees
        long totalEmployees = employeeRepository.count();
        stats.put("totalEmployees", totalEmployees);

        // Total leaves by status
        long totalApproved = leaveRepository.findByStatus(LeaveStatus.APPROVED).size();
        long totalPending = leaveRepository.findByStatus(LeaveStatus.PENDING).size();
        long totalRejected = leaveRepository.findByStatus(LeaveStatus.REJECTED).size();

        stats.put("totalLeavesApproved", totalApproved);
        stats.put("totalLeavesPending", totalPending);
        stats.put("totalLeavesRejected", totalRejected);

        // Average leaves per employee (approximate)
        if (totalEmployees > 0) {
            stats.put("averageLeavesPerEmployee", totalApproved / totalEmployees);
        } else {
            stats.put("averageLeavesPerEmployee", 0);
        }

        return stats;
    }

    /**
     * Get admin dashboard summary
     */
    public Map<String, Object> getAdminDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.countByActive(true);
        long pendingApprovals = leaveRepository.findByStatus(LeaveStatus.PENDING).size();

        summary.put("totalEmployees", totalEmployees);
        summary.put("activeEmployees", activeEmployees);
        summary.put("inactiveEmployees", totalEmployees - activeEmployees);
        summary.put("pendingApprovals", pendingApprovals);

        return summary;
    }

    /**
     * Get leave calendar for employee (specific month)
     */
    public Map<String, Object> getLeaveCalendar(Long employeeId, Integer year, Integer month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<LeaveApplication> leaves = leaveRepository.findByEmployeeId(employeeId)
                .stream()
                .filter(leave ->
                        leave.getStatus() == LeaveStatus.APPROVED &&
                                !leave.getEndDate().isBefore(startDate) &&
                                !leave.getStartDate().isAfter(endDate)
                )
                .toList();

        Map<String, Object> calendar = new HashMap<>();
        calendar.put("year", year);
        calendar.put("month", month);
        calendar.put("leaves", leaves);

        return calendar;
    }

    /**
     * Get team leave calendar (date range)
     */
    public Map<String, Object> getTeamLeaveCalendar(Long managerId, LocalDate startDate, LocalDate endDate) {
        List<Employee> team = employeeRepository.findByManagerId(managerId);
        List<Long> teamIds = team.stream().map(Employee::getId).toList();

        List<LeaveApplication> teamLeaves = leaveRepository.findApprovedLeavesInRange(startDate, endDate)
                .stream()
                .filter(leave -> teamIds.contains(leave.getEmployeeId()))
                .toList();

        Map<String, Object> calendar = new HashMap<>();
        calendar.put("startDate", startDate);
        calendar.put("endDate", endDate);
        calendar.put("teamSize", team.size());
        calendar.put("leaves", teamLeaves);

        return calendar;
    }

    /**
     * Get who's out today across company
     */
    public Map<String, Object> getWhosOutToday() {
        LocalDate today = LocalDate.now();
        List<LeaveApplication> todaysLeaves = leaveRepository.findApprovedLeavesOnDate(today);

        List<Map<String, Object>> employeesOut = new ArrayList<>();
        for (LeaveApplication leave : todaysLeaves) {
            Employee employee = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
            if (employee != null) {
                Map<String, Object> empInfo = new HashMap<>();
                empInfo.put("employeeId", employee.getId());
                empInfo.put("employeeName", employee.getName());
                empInfo.put("leaveType", leave.getLeaveType());
                empInfo.put("startDate", leave.getStartDate());
                empInfo.put("endDate", leave.getEndDate());
                employeesOut.add(empInfo);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("count", employeesOut.size());
        result.put("employees", employeesOut);

        return result;
    }

    /**
     * Get who's out by date range
     */
    public Map<String, Object> getWhosOut(LocalDate startDate, LocalDate endDate) {
        List<LeaveApplication> leaves = leaveRepository.findApprovedLeavesInRange(startDate, endDate);

        List<Map<String, Object>> employeesOut = new ArrayList<>();
        for (LeaveApplication leave : leaves) {
            Employee employee = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
            if (employee != null) {
                Map<String, Object> empInfo = new HashMap<>();
                empInfo.put("employeeId", employee.getId());
                empInfo.put("employeeName", employee.getName());
                empInfo.put("leaveType", leave.getLeaveType());
                empInfo.put("startDate", leave.getStartDate());
                empInfo.put("endDate", leave.getEndDate());
                employeesOut.add(empInfo);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("count", employeesOut.size());
        result.put("employees", employeesOut);

        return result;
    }
}