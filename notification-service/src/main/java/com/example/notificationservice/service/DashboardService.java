package com.example.notificationservice.service;

import com.example.notificationservice.dto.EmployeeDashboardResponse;

import com.example.notificationservice.dto.LeaveBalanceResponse;
import com.example.notificationservice.dto.MonthlyStatsResponse;
import com.example.notificationservice.dto.TeamMemberBalance;
import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.entity.LeaveApplication;

import com.example.notificationservice.enums.LeaveStatus;

import com.example.notificationservice.repository.EmployeeRepository;
import com.example.notificationservice.repository.LeaveApplicationRepository;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final LeaveApplicationRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceService leaveBalanceService;

    public DashboardService(LeaveBalanceService leaveBalanceService,
                            LeaveApplicationRepository leaveRepository,
                            EmployeeRepository employeeRepository){
        this.leaveRepository=leaveRepository;
        this.employeeRepository=employeeRepository;
        this.leaveBalanceService=leaveBalanceService;
    }


    public EmployeeDashboardResponse getDashboard(Long employeeId) {

        LeaveBalanceResponse balance =
                leaveBalanceService.getBalance(
                        employeeId,
                        java.time.Year.now().getValue()
                );

        Map<LeaveStatus, Long> statusCount =
                leaveRepository.findByEmployeeId(employeeId)
                        .stream()
                        .collect(Collectors.groupingBy(
                                LeaveApplication::getStatus,
                                Collectors.counting()
                        ));

        EmployeeDashboardResponse response =
                new EmployeeDashboardResponse();

        response.setLeaveBalances(balance.getBreakdown());
        response.setLeaveStatusCount(statusCount);

        return response;
    }


    /* ==============================
       EMPLOYEE: MONTHLY STATS
       ============================== */
    public MonthlyStatsResponse getMonthlyStats(Long employeeId, Integer year, Integer month) {

        List<Object[]> stats = leaveRepository.getMonthlyStats(employeeId, year, month);

        List<MonthlyStatsResponse.LeaveTypeStat> statsList = new ArrayList<>();
        double totalDays = 0;

        for (Object[] row : stats) {
            // 🔥 FIX IS HERE
            String leaveType = row[0].toString();

            long count = ((Number) row[1]).longValue();
            double days = ((Number) row[2]).doubleValue();

            MonthlyStatsResponse.LeaveTypeStat stat =
                    new MonthlyStatsResponse.LeaveTypeStat(
                            leaveType,
                            (int) count,
                            days
                    );

            stat.setLeaveType(leaveType);
            stat.setCount((int) count);
            stat.setTotalDays(days);

            statsList.add(stat);
            totalDays += days;
        }

        long approvedCount = leaveRepository.countApprovedInMonth(employeeId, year, month);

        MonthlyStatsResponse response =
                new MonthlyStatsResponse(
                        employeeId,
                        year,
                        month,
                        (int)  approvedCount,
                        totalDays,
                        approvedCount > 2,
                        statsList
                );

        response.setEmployeeId(employeeId);
        response.setYear(year);
        response.setMonth(month);
        response.setTotalApprovedCount((int)approvedCount);
        response.setTotalDays(totalDays);
        response.setExceededLimit(approvedCount > 2);
        response.setBreakdown(statsList);

        return response;
    }


    /* ==============================
       MANAGER: TEAM BALANCES
       ============================== */
    public List<TeamMemberBalance> getTeamBalances(
            Long managerId,
            Integer year
    ) {

        List<Employee> team =
                employeeRepository.findByManagerId(managerId);

        List<TeamMemberBalance> result = new ArrayList<>();

        for (Employee emp : team) {

            LeaveBalanceResponse balance =
                    leaveBalanceService.getBalance(emp.getId(), year);

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


    /* ==============================
       MANAGER: PENDING COUNT
       ============================== */
    public int getPendingCount(Long managerId) {

        List<Long> teamIds =
                employeeRepository
                        .findByManagerId(managerId)
                        .stream()
                        .map(Employee::getId)
                        .toList();

        return leaveRepository
                .countByEmployeeIdInAndStatus(
                        teamIds,
                        LeaveStatus.PENDING
                );
    }
}

