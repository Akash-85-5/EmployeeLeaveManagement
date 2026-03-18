package com.example.employeeLeaveApplication.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.example.employeeLeaveApplication.dto.*;
import com.example.employeeLeaveApplication.entity.*;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.enums.ODStatus;
import com.example.employeeLeaveApplication.exceptions.ResourceNotFoundException;
import com.example.employeeLeaveApplication.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.employeeLeaveApplication.component.PolicyConstants;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final CompOffBalanceRepository compOffRepository;
    private final CarryForwardBalanceRepository carryForwardRepository;
    private final LossOfPayRecordRepository lopRepository;
    private final ODRequestRepository odRepository;
    private final AnnualLeaveMonthlyBalanceRepository annualLeaveMonthlyBalanceRepository;
    private final EmployeePersonalDetailsRepository employeePersonalDetailsRepository;

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    public List<TeamMember> getTeamMembers(Long id) {
        log.info("👥 [DASHBOARD-MANAGER] Getting team members for manager: {}", id);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(id);

        return teamMembers.stream().map(member -> {
            TeamMember dto = new TeamMember();
            dto.setEmployeeId(member.getId());
            dto.setEmployeeName(member.getName());


            employeePersonalDetailsRepository.findByEmployeeId(member.getId())
                    .ifPresent(details -> {
                        dto.setDesignation(details.getDesignation());
                        dto.setSkills(details.getSkillSet());
                    });

            return dto;
        }).collect(Collectors.toList());
    }

    public EmployeeDashboardResponse getDashboard(Long employeeId) {

        log.info("📊 [DASHBOARD] Getting employee dashboard: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        int currentYear  = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        EmployeeDashboardResponse response = new EmployeeDashboardResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setCurrentYear(currentYear);
        response.setLastUpdated(LocalDateTime.now());

        // ── 1. YEARLY STATS ───────────────────────────────────────
        List<LeaveAllocation> allocations = allocationRepository
                .findByEmployeeIdAndYear(employeeId, currentYear);

        double yearlyAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        Double yearlyUsed = applicationRepository
                .getTotalUsedDays(employeeId, LeaveStatus.APPROVED, currentYear);
        if (yearlyUsed == null) yearlyUsed = 0.0;

        response.setYearlyAllocated(yearlyAllocated);
        response.setYearlyUsed(yearlyUsed);
        response.setYearlyBalance(yearlyAllocated - yearlyUsed);

        // ── 2. MONTHLY STATS (cumulative ANNUAL_LEAVE balance) ────
        // How many ANNUAL_LEAVE days the employee has available this month
        // (cumulative: unused days from previous months roll forward)
        AnnualLeaveMonthlyBalance monthlyBalance = annualLeaveMonthlyBalanceRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, currentYear, currentMonth)
                .orElse(null);

        double monthlyAvailable = monthlyBalance != null
                ? monthlyBalance.getAvailableDays() : PolicyConstants.ANNUAL_LEAVE_PER_MONTH;
        double monthlyUsed      = monthlyBalance != null ? monthlyBalance.getUsedDays()      : 0.0;
        double monthlyRemaining = monthlyBalance != null ? monthlyBalance.getRemainingDays()  : monthlyAvailable;

        // Keep field names compatible with existing EmployeeDashboardResponse
        response.setMonthlyAllocated(monthlyAvailable);
        response.setMonthlyUsed(monthlyUsed);
        response.setMonthlyBalance(monthlyRemaining);

        // ── 3. CARRY FORWARD ──────────────────────────────────────
        CarryForwardBalance cfBalance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElse(null);

        response.setCarryForwardTotal(cfBalance != null ? cfBalance.getTotalCarriedForward() : 0.0);
        response.setCarryForwardUsed(cfBalance != null ? cfBalance.getTotalUsed() : 0.0);
        response.setCarryForwardRemaining(cfBalance != null ? cfBalance.getRemaining() : 0.0);

        // ── 4. LEAVE TYPE BREAKDOWN ───────────────────────────────
        List<LeaveApplication> approvedLeaves = applicationRepository
                .findByEmployeeIdAndStatusAndYear(employeeId, LeaveStatus.APPROVED, currentYear);

        Map<LeaveType, List<LeaveApplication>> byType = approvedLeaves.stream()
                .collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();

        for (LeaveAllocation allocation : allocations) {
            LeaveType type     = allocation.getLeaveCategory();
            double allocated   = allocation.getAllocatedDays();
            List<LeaveApplication> typeLeaves = byType.getOrDefault(type, List.of());

            double used = typeLeaves.stream()
                    .mapToDouble(l -> l.getDays().doubleValue())
                    .sum();

            // For ANNUAL_LEAVE, remaining = cumulative monthly remaining (not simple allocated-used)
            double remaining = (type == LeaveType.ANNUAL_LEAVE)
                    ? monthlyRemaining
                    : (allocated - used);

            int halfDays = (int) typeLeaves.stream()
                    .filter(l -> l.getDays().compareTo(new BigDecimal("0.5")) == 0)
                    .count();

            breakdown.add(new LeaveTypeBreakdown(
                    type,
                    (Double) allocated,
                    (Double) used,
                    (Double) remaining,
                    halfDays));
        }

        // CompOff in breakdown
        CompOffBalance compOff = compOffRepository
                .findByEmployeeIdAndYear(employeeId, currentYear).orElse(null);

        double coEarned = compOff != null ? compOff.getEarned()  : 0.0;
        double coUsed   = compOff != null ? compOff.getUsed()    : 0.0;
        double coBal    = compOff != null ? compOff.getBalance() : 0.0;

        breakdown.add(new LeaveTypeBreakdown(
                LeaveType.COMP_OFF,
                (Double) coEarned,
                (Double) coUsed,
                (Double) coBal,
                0));

        response.setBreakdown(breakdown);
        response.setCompoffBalance(coBal);

        // ── 5. LOSS OF PAY (manual — just read stored value) ──────
        Double totalLOP = lopRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, currentYear);
        response.setLossOfPayPercentage(totalLOP != null ? totalLOP : 0.0);

        // ── 6. LEAVE STATUS COUNTS ────────────────────────────────
        Integer approvedCount = applicationRepository
                .countByStatus(employeeId, currentYear, LeaveStatus.APPROVED);
        Integer rejectedCount = applicationRepository
                .countByStatus(employeeId, currentYear, LeaveStatus.REJECTED);
        Integer pendingCount  = applicationRepository
                .countByStatus(employeeId, currentYear, LeaveStatus.PENDING);

        response.setApprovedCount(approvedCount != null ? approvedCount : 0);
        response.setRejectedCount(rejectedCount != null ? rejectedCount : 0);
        response.setPendingCount(pendingCount   != null ? pendingCount  : 0);

        log.info("✅ [DASHBOARD] Employee dashboard complete: allocated={}, used={}", yearlyAllocated, yearlyUsed);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // MONTHLY STATS
    // ═══════════════════════════════════════════════════════════════

    public MonthlyStatsResponse getMonthlyStats(Long employeeId, Integer year, Integer month) {

        log.info("📅 [DASHBOARD] Getting monthly stats: employee={}, year={}, month={}", employeeId, year, month);

        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        MonthlyStatsResponse response = new MonthlyStatsResponse();
        response.setEmployeeId(employeeId);
        response.setYear(year);
        response.setMonth(month);

        // Use cumulative monthly balance for ANNUAL_LEAVE
        AnnualLeaveMonthlyBalance balance = annualLeaveMonthlyBalanceRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElse(null);

        double usedDays      = balance != null ? balance.getUsedDays()      : 0.0;
        double availableDays = balance != null ? balance.getAvailableDays() : PolicyConstants.ANNUAL_LEAVE_PER_MONTH;

        response.setTotalApprovedCount((int) usedDays);
        // Exceeded = used more than what was available this month
        response.setExceededLimit(usedDays > availableDays);

        log.info("✅ [DASHBOARD] Monthly stats: usedDays={}, available={}", usedDays, availableDays);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // MANAGER DASHBOARD - Team View
    // ═══════════════════════════════════════════════════════════════

    public List<TeamMemberBalance> getTeamBalances(Long managerId, Integer year) {

        log.info("👥 [DASHBOARD-MANAGER] Getting team balances for manager: {}", managerId);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        List<TeamMemberBalance> balances = new ArrayList<>();

        for (Employee member : teamMembers) {
            TeamMemberBalance balance = new TeamMemberBalance();
            balance.setEmployeeId(member.getId());
            balance.setEmployeeName(member.getName());

            Double allocated = allocationRepository.getTotalAllocatedDays(member.getId(), year);
            balance.setTotalAllocated(allocated != null ? allocated : 0.0);

            Double used = applicationRepository.getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, year);
            balance.setTotalUsed(used != null ? used : 0.0);
            balance.setTotalRemaining(balance.getTotalAllocated() - balance.getTotalUsed());

            CompOffBalance compOff = compOffRepository
                    .findByEmployeeIdAndYear(member.getId(), year).orElse(null);
            balance.setCompOffBalance(compOff != null ? compOff.getBalance() : 0.0);

            Double lop = lopRepository.getTotalLossPercentageByEmployeeIdAndYear(member.getId(), year);
            balance.setLopPercentage(lop != null ? lop : 0.0);

            balances.add(balance);
        }
        return balances;
    }

    public List<TeamMemberBalance> getTeamMembersOnLeaveToday(Long id) {
        LocalDate today = LocalDate.now();
        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(id);
        List<TeamMemberBalance> onLeave = new ArrayList<>();

        for (Employee member : teamMembers) {
            List<LeaveApplication> todayLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate()))
                    .collect(Collectors.toList());

            if (!todayLeaves.isEmpty()) {
                TeamMemberBalance balance = new TeamMemberBalance();
                balance.setEmployeeId(member.getId());
                balance.setEmployeeName(member.getName());
                balance.setTotalUsed(todayLeaves.stream()
                        .mapToDouble(la -> la.getDays().doubleValue()).sum());
                onLeave.add(balance);
            }
        }
        return onLeave;
    }

    public Map<String, List<TeamMemberBalance>> getTeamLeaveCalendar(Long id) {
        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(id);
        Map<String, List<TeamMemberBalance>> calendar = new TreeMap<>();

        for (Employee member : teamMembers) {
            processLeavesIntoCalendar(calendar, member,
                    applicationRepository.findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED));
            processODsIntoCalendar(calendar, member,
                    odRepository.findByEmployeeIdAndStatus(member.getId(), ODStatus.APPROVED));
        }
        return calendar;
    }

    public Map<String, List<TeamMemberBalance>> getMyLeaveCalendar(Long employeeId) {
        Employee member = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        Map<String, List<TeamMemberBalance>> calendar = new TreeMap<>();
        processLeavesIntoCalendar(calendar, member,
                applicationRepository.findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED));
        processODsIntoCalendar(calendar, member,
                odRepository.findByEmployeeIdAndStatus(employeeId, ODStatus.APPROVED));
        return calendar;
    }

    private void processLeavesIntoCalendar(Map<String, List<TeamMemberBalance>> calendar,
                                           Employee member, List<LeaveApplication> leaves) {
        for (LeaveApplication leave : leaves) {
            LocalDate date = leave.getStartDate();
            while (!date.isAfter(leave.getEndDate())) {
                addToCalendar(calendar, date, member, "LEAVE");
                date = date.plusDays(1);
            }
        }
    }

    private void processODsIntoCalendar(Map<String, List<TeamMemberBalance>> calendar,
                                        Employee member, List<ODRequest> ods) {
        for (ODRequest od : ods) {
            LocalDate date = od.getFromDate();
            while (!date.isAfter(od.getToDate())) {
                addToCalendar(calendar, date, member, "OD");
                date = date.plusDays(1);
            }
        }
    }

    private void addToCalendar(Map<String, List<TeamMemberBalance>> calendar,
                               LocalDate date, Employee member, String type) {
        calendar.computeIfAbsent(date.toString(), k -> new ArrayList<>());
        TeamMemberBalance entry = new TeamMemberBalance();
        entry.setEmployeeId(member.getId());
        entry.setEmployeeName(member.getName());
        calendar.get(date.toString()).add(entry);
    }

    public Integer getPendingTeamRequestsCount(Long managerId) {
        return employeeRepository.findActiveTeamMembers(managerId).stream()
                .mapToInt(m -> applicationRepository
                        .findByEmployeeIdAndStatus(m.getId(), LeaveStatus.PENDING).size())
                .sum();
    }

    public List<LeaveApplication> getPendingTeamRequests(Long managerId) {
        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        List<LeaveApplication> allPending = new ArrayList<>();
        for (Employee member : teamMembers) {
            allPending.addAll(applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.PENDING));
        }
        allPending.sort(Comparator.comparing(LeaveApplication::getCreatedAt));
        return allPending;
    }

    // ═══════════════════════════════════════════════════════════════
    // TEAM LEADER DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public TeamLeaderDashboardResponse getTeamLeaderDashboard(Long teamLeaderId) {

        Employee teamLeader = employeeRepository.findById(teamLeaderId)
                .orElseThrow(() -> new RuntimeException("Team Leader not found: " + teamLeaderId));

        if (teamLeader.getRole() != Role.TEAM_LEADER) {
            throw new RuntimeException("Employee " + teamLeaderId + " is not a Team Leader");
        }

        EmployeeDashboardResponse ownStats = getDashboard(teamLeaderId);
        TeamLeaderDashboardResponse response = new TeamLeaderDashboardResponse();
        response.setPersonalStats(ownStats);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembersByTeamLeader(teamLeaderId);
        response.setTeamSize(teamMembers.size());

        List<TeamLeaderDashboardResponse.TeamPendingLeaveDTO> pendingDTOs = new ArrayList<>();
        for (Employee member : teamMembers) {
            for (LeaveApplication leave : applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.PENDING)) {
                pendingDTOs.add(new TeamLeaderDashboardResponse.TeamPendingLeaveDTO(
                        leave.getId(), leave.getEmployeeId(), member.getName(),
                        leave.getLeaveType(), leave.getReason(), leave.getStatus(),
                        leave.getStartDate(), leave.getEndDate(),
                        leave.getDays().doubleValue(), leave.getCreatedAt()));
            }
        }
        pendingDTOs.sort(Comparator.comparing(
                TeamLeaderDashboardResponse.TeamPendingLeaveDTO::getAppliedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        response.setPendingTeamRequests(pendingDTOs);
        response.setTeamPendingRequestCount(pendingDTOs.size());

        LocalDate today = LocalDate.now();
        List<TeamLeaderDashboardResponse.TeamMemberOnLeaveDTO> onLeaveDTOs = new ArrayList<>();
        for (Employee member : teamMembers) {
            applicationRepository.findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate()))
                    .forEach(leave -> {
                        long daysRemaining = ChronoUnit.DAYS.between(today, leave.getEndDate());
                        onLeaveDTOs.add(new TeamLeaderDashboardResponse.TeamMemberOnLeaveDTO(
                                member.getId(), member.getName(), leave.getLeaveType().name(),
                                leave.getStartDate(), leave.getEndDate(),
                                (double) Math.max(0, daysRemaining)));
                    });
        }
        response.setTeamOnLeaveToday(onLeaveDTOs);
        response.setTeamOnLeaveCount(onLeaveDTOs.size());

        int currentYear = LocalDate.now().getYear();
        List<TeamLeaderDashboardResponse.TeamMemberBalanceSummaryDTO> balanceSummaries = new ArrayList<>();

        for (Employee member : teamMembers) {
            Double allocated = allocationRepository.getTotalAllocatedDays(member.getId(), currentYear);
            Double used      = applicationRepository.getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, currentYear);
            if (allocated == null) allocated = 0.0;
            if (used == null)      used      = 0.0;

            CompOffBalance compOff = compOffRepository
                    .findByEmployeeIdAndYear(member.getId(), currentYear).orElse(null);
            Double lop = lopRepository
                    .getTotalLossPercentageByEmployeeIdAndYear(member.getId(), currentYear);

            balanceSummaries.add(new TeamLeaderDashboardResponse.TeamMemberBalanceSummaryDTO(
                    member.getId(), member.getName(), allocated, used, allocated - used,
                    compOff != null ? compOff.getBalance() : 0.0,
                    lop != null ? lop : 0.0));
        }

        response.setTeamBalances(balanceSummaries);
        response.setLastUpdated(LocalDateTime.now());
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    public Map<String, Object> getCompanyWideStats(Integer year) {
        Map<String, Object> stats = new HashMap<>();
        List<Employee> activeEmployees = employeeRepository.findActiveEmployees();
        stats.put("totalEmployees", activeEmployees.size());

        int totalApproved = 0;
        double totalDaysUsed = 0.0;
        for (Employee emp : activeEmployees) {
            Double used = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, year);
            if (used != null) { totalDaysUsed += used; totalApproved++; }
        }
        stats.put("totalApprovedLeaves", totalApproved);
        stats.put("totalDaysUsed", totalDaysUsed);

        double totalLOP = 0.0;
        int lopCount = 0;
        for (Employee emp : activeEmployees) {
            Double lop = lopRepository.getTotalLossPercentageByEmployeeIdAndYear(emp.getId(), year);
            if (lop != null && lop > 0) { totalLOP += lop; lopCount++; }
        }
        stats.put("totalLopPercentage", lopCount > 0 ? totalLOP / lopCount : 0.0);
        stats.put("employeesWithLOP", lopCount);

        double cfUsedTotal = 0.0, cfTotalTotal = 0.0;
        for (Employee emp : activeEmployees) {
            CarryForwardBalance cf = carryForwardRepository
                    .findByEmployeeIdAndYear(emp.getId(), year).orElse(null);
            if (cf != null) { cfUsedTotal += cf.getTotalUsed(); cfTotalTotal += cf.getTotalCarriedForward(); }
        }
        stats.put("carryForwardUtilization",
                cfTotalTotal > 0 ? (cfUsedTotal / cfTotalTotal * 100) : 0.0);
        return stats;
    }

    public List<Employee> getEmployeesCurrentlyOnLeave() {
        LocalDate today = LocalDate.now();
        return employeeRepository.findActiveEmployees().stream()
                .filter(emp -> applicationRepository
                        .findByEmployeeIdAndStatus(emp.getId(), LeaveStatus.APPROVED)
                        .stream()
                        .anyMatch(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate())))
                .collect(Collectors.toList());
    }

    public List<Employee> getManagersWithUpcomingLeave() {
        LocalDate today = LocalDate.now(), nextWeek = today.plusDays(7);
        return employeeRepository.findByRole(Role.MANAGER).stream()
                .filter(m -> applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.APPROVED)
                        .stream()
                        .anyMatch(la -> !la.getStartDate().isBefore(today) && !la.getStartDate().isAfter(nextWeek)))
                .collect(Collectors.toList());
    }

    public List<Employee> getAdminsWithUpcomingLeave() {
        LocalDate today = LocalDate.now(), nextWeek = today.plusDays(7);
        return employeeRepository.findByRole(Role.ADMIN).stream()
                .filter(a -> applicationRepository.findByEmployeeIdAndStatus(a.getId(), LeaveStatus.APPROVED)
                        .stream()
                        .anyMatch(la -> !la.getStartDate().isBefore(today) && !la.getStartDate().isAfter(nextWeek)))
                .collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getEmployeesWithLowBalance(Integer year, Double threshold) {
        return employeeRepository.findActiveEmployees().stream()
                .map(emp -> {
                    Double allocated = allocationRepository.getTotalAllocatedDays(emp.getId(), year);
                    Double used      = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, year);
                    if (allocated == null) allocated = 0.0;
                    if (used == null)      used      = 0.0;
                    double remaining = allocated - used;
                    if (remaining < threshold) {
                        TeamMemberBalance b = new TeamMemberBalance();
                        b.setEmployeeId(emp.getId()); b.setEmployeeName(emp.getName());
                        b.setTotalAllocated(allocated); b.setTotalUsed(used); b.setTotalRemaining(remaining);
                        return b;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getEmployeesWithHighLOP(Integer year, Double threshold) {
        return employeeRepository.findActiveEmployees().stream()
                .map(emp -> {
                    Double lop = lopRepository.getTotalLossPercentageByEmployeeIdAndYear(emp.getId(), year);
                    if (lop != null && lop > threshold) {
                        TeamMemberBalance b = new TeamMemberBalance();
                        b.setEmployeeId(emp.getId()); b.setEmployeeName(emp.getName());
                        b.setLopPercentage(lop);
                        return b;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getCarryForwardEligible(Integer year) {
        return employeeRepository.findActiveEmployees().stream()
                .map(emp -> {
                    Double allocated = allocationRepository.getTotalAllocatedDays(emp.getId(), year);
                    Double used      = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, year);
                    if (allocated == null) allocated = 0.0;
                    if (used == null)      used      = 0.0;
                    double balance = allocated - used;
                    if (balance > 0) {
                        TeamMemberBalance tmb = new TeamMemberBalance();
                        tmb.setEmployeeId(emp.getId()); tmb.setEmployeeName(emp.getName());
                        tmb.setTotalAllocated(allocated); tmb.setTotalUsed(used);
                        tmb.setTotalRemaining(Math.min(balance, PolicyConstants.MAX_CARRY_FORWARD));
                        return tmb;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public EmployeeDashboardResponse getEmployeeDashboard(Long employeeId) {
        return getDashboard(employeeId);
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN — Employees Exceeding Monthly Limit
    // Now means: used more ANNUAL_LEAVE than their cumulative available for the month
    // ═══════════════════════════════════════════════════════════════

    public List<TeamMemberBalance> getEmployeesExceedingMonthlyLimit(Integer year, Integer month) {
        return employeeRepository.findActiveEmployees().stream()
                .map(emp -> {
                    AnnualLeaveMonthlyBalance bal = annualLeaveMonthlyBalanceRepository
                            .findByEmployeeIdAndYearAndMonth(emp.getId(), year, month)
                            .orElse(null);
                    if (bal != null && bal.getUsedDays() > bal.getAvailableDays()) {
                        TeamMemberBalance b = new TeamMemberBalance();
                        b.setEmployeeId(emp.getId()); b.setEmployeeName(emp.getName());
                        b.setTotalUsed(bal.getUsedDays());
                        return b;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPREHENSIVE MANAGER DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(Long managerId) {

        EmployeeDashboardResponse ownStats = getDashboard(managerId);
        ManagerDashboardResponse response  = new ManagerDashboardResponse();
        response.setPersonalStats(ownStats);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        response.setTeamSize(teamMembers.size());

        List<LeaveApplication> pendingRequests = applicationRepository.findPendingTeamRequests(managerId);
        response.setTeamPendingRequestCount(pendingRequests.size());

        List<ManagerDashboardResponse.TeamPendingLeaveDTO> pendingDTOs = pendingRequests.stream()
                .map(leave -> {
                    Employee emp = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
                    return new ManagerDashboardResponse.TeamPendingLeaveDTO(
                            leave.getId(), leave.getEmployeeId(),
                            emp != null ? emp.getName() : "Unknown",
                            leave.getLeaveType(), leave.getReason(), leave.getStatus(),
                            leave.getStartDate(), leave.getEndDate(),
                            leave.getDays().doubleValue(), leave.getCreatedAt());
                }).collect(Collectors.toList());
        response.setPendingTeamRequests(pendingDTOs);

        LocalDate today = LocalDate.now();
        List<ManagerDashboardResponse.TeamMemberOnLeaveDTO> onLeaveDTOs = new ArrayList<>();
        for (Employee member : teamMembers) {
            applicationRepository.findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate()))
                    .forEach(leave -> {
                        long daysRemaining = ChronoUnit.DAYS.between(today, leave.getEndDate());
                        onLeaveDTOs.add(new ManagerDashboardResponse.TeamMemberOnLeaveDTO(
                                member.getId(), member.getName(), leave.getLeaveType().name(),
                                leave.getStartDate(), leave.getEndDate(),
                                (double) Math.max(0, daysRemaining)));
                    });
        }
        response.setTeamOnLeaveToday(onLeaveDTOs);
        response.setTeamOnLeaveCount(onLeaveDTOs.size());
        response.setLastUpdated(LocalDateTime.now());
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPREHENSIVE HR DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public HRDashboardResponse getHRDashboard() {

        int currentYear = LocalDate.now().getYear();
        HRDashboardResponse response = new HRDashboardResponse();
        response.setCurrentYear(currentYear);

        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();
        response.setTotalEmployees(activeEmployees.size());
        response.setActiveEmployees(activeEmployees.size());

        LocalDate today = LocalDate.now();
        List<Long> onLeaveTodayIds = applicationRepository.findEmployeesCurrentlyOnLeave(today);
        response.setEmployeesOnLeaveToday(onLeaveTodayIds.size());

        List<LeaveApplication> pending = applicationRepository.findByStatus(LeaveStatus.PENDING);
        response.setTotalPendingLeaves(pending.size());

        int approvedCount = 0;
        for (Employee emp : activeEmployees) {
            approvedCount += applicationRepository
                    .findByEmployeeIdAndStatusAndYear(emp.getId(), LeaveStatus.APPROVED, currentYear).size();
        }
        response.setTotalApprovedLeaves(approvedCount);

        List<Employee> onboardingPending = employeeRepository.findOnboardingPending();
        response.setNewEmployeesCount(onboardingPending.size());
        response.setPendingBiometricCount(employeeRepository.countPendingBiometric());
        response.setPendingVPNCount(employeeRepository.countPendingVPN());

        List<HRDashboardResponse.OnboardingPendingDTO> onboardingDTOs = new ArrayList<>();
        for (Employee emp : onboardingPending) {
            int daysInOnboarding = (int) ChronoUnit.DAYS.between(emp.getJoiningDate(), LocalDate.now());
            onboardingDTOs.add(new HRDashboardResponse.OnboardingPendingDTO(
                    emp.getId(), emp.getName(), emp.getEmail(), emp.getJoiningDate(),
                    emp.getBiometricStatus(), emp.getVpnStatus(), daysInOnboarding));
        }
        response.setOnboardingPendingList(onboardingDTOs);

        List<LeaveApplication> leavesInRange = applicationRepository.findApprovedLeavesInDateRange(today, today);
        List<HRDashboardResponse.EmployeeOnLeaveDTO> onLeaveDTOs = new ArrayList<>();
        for (LeaveApplication leave : leavesInRange) {
            Employee emp      = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
            if (emp == null) continue;
            Employee manager  = emp.getManagerId() != null
                    ? employeeRepository.findById(emp.getManagerId()).orElse(null) : null;
            Employee approver = leave.getApprovedBy() != null
                    ? employeeRepository.findById(leave.getApprovedBy()).orElse(null) : null;
            onLeaveDTOs.add(new HRDashboardResponse.EmployeeOnLeaveDTO(
                    emp.getId(), emp.getName(), manager != null ? manager.getName() : "N/A",
                    leave.getLeaveType().name(), leave.getStartDate(), leave.getEndDate(),
                    leave.getDays().doubleValue(),
                    leave.getApprovedAt() != null ? leave.getApprovedAt().toLocalDate() : null,
                    approver != null ? approver.getName() : "N/A"));
        }
        response.setEmployeesOnLeave(onLeaveDTOs);

        List<Long> managerIds = applicationRepository.findManagersWhoApprovedLeaves(currentYear);
        response.setTotalManagersWithApprovals(managerIds.size());

        List<HRDashboardResponse.ManagerApprovalStatsDTO> managerStats = new ArrayList<>();
        for (Long mgrId : managerIds) {
            Employee mgr = employeeRepository.findById(mgrId).orElse(null);
            if (mgr == null) continue;
            List<Employee>         teamMembers      = employeeRepository.findTeamMembersByManager(mgrId);
            List<LeaveApplication> managerApprovals = applicationRepository.findLeavesApprovedByManager(mgrId, currentYear);
            int teamPending = teamMembers.stream()
                    .mapToInt(tm -> applicationRepository.findByEmployeeIdAndStatus(tm.getId(), LeaveStatus.PENDING).size())
                    .sum();
            int approvalRate = !teamMembers.isEmpty()
                    ? (int) ((managerApprovals.size() / (double) teamMembers.size()) * 100) : 0;
            LocalDateTime lastApproval = managerApprovals.isEmpty() ? null : managerApprovals.get(0).getApprovedAt();
            managerStats.add(new HRDashboardResponse.ManagerApprovalStatsDTO(
                    mgrId, mgr.getName(), teamMembers.size(), managerApprovals.size(),
                    teamPending, approvalRate,
                    lastApproval != null ? lastApproval.toLocalDate() : null));
        }
        response.setManagerApprovalStats(managerStats);

        List<Employee> allManagers = employeeRepository.findAllManagers();
        List<HRDashboardResponse.TeamStructureDTO> teamStructure = new ArrayList<>();
        for (Employee manager : allManagers) {
            List<Employee> teamMembers = employeeRepository.findTeamMembersByManager(manager.getId());
            List<HRDashboardResponse.TeamMemberDTO> teamMemberDTOs = new ArrayList<>();
            for (Employee member : teamMembers) {
                Double yearlyBal  = allocationRepository.getTotalAllocatedDays(member.getId(), currentYear);
                Double yearlyUsed = applicationRepository.getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, currentYear);
                CarryForwardBalance cf = carryForwardRepository
                        .findByEmployeeIdAndYear(member.getId(), currentYear).orElse(null);
                CompOffBalance compOff = compOffRepository
                        .findByEmployeeIdAndYear(member.getId(), currentYear).orElse(null);
                teamMemberDTOs.add(new HRDashboardResponse.TeamMemberDTO(
                        member.getId(), member.getName(), member.getEmail(),
                        (yearlyBal  != null ? yearlyBal  : 0.0) - (yearlyUsed != null ? yearlyUsed : 0.0),
                        cf      != null ? cf.getRemaining()     : 0.0,
                        compOff != null ? compOff.getBalance()  : 0.0));
            }
            teamStructure.add(new HRDashboardResponse.TeamStructureDTO(
                    manager.getId(), manager.getName(), teamMembers.size(), teamMemberDTOs));
        }
        response.setTeamStructure(teamStructure);
        response.setLastUpdated(LocalDateTime.now());
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPREHENSIVE ADMIN DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard(Long adminId) {
        // 1. Get Admin's Personal Data (The same way the Manager does)
        EmployeeDashboardResponse ownStats = getDashboard(adminId);

        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setPersonalStats(ownStats); // Use the nested DTO
        response.setCurrentYear(LocalDate.now().getYear());
        response.setLastUpdated(LocalDateTime.now());

        // 2. Global Employee Counts
        List<Employee> allEmployees = employeeRepository.findByActiveTrue();
        response.setTotalEmployees(allEmployees.size());
        response.setTotalManagers(employeeRepository.findByRole(Role.MANAGER).size());
        response.setNewEmployeesPendingOnboarding(employeeRepository.findOnboardingPending().size());

        // 3. Global Leave Counts
        response.setTotalPendingLeaves(applicationRepository.findByStatus(LeaveStatus.PENDING).size());
        response.setTotalRejectedLeaves(applicationRepository.findByStatus(LeaveStatus.REJECTED).size());

        // 4. Global Financial/Compliance Metrics
        // (Your existing loop logic for YTD, CarryForward, and LOP averages)
        calculateGlobalMetrics(response, allEmployees);

        return response;
    }

    private void calculateGlobalMetrics(AdminDashboardResponse response, List<Employee> allEmployees) {
        int currentYear = response.getCurrentYear();
        double totalUsedYTD = 0, totalCFBalance = 0, totalCompOffBalance = 0, totalLOP = 0;
        int lopCount = 0;

        for (Employee emp : allEmployees) {
            // Total Approved Days
            Double used = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, currentYear);
            if (used != null) totalUsedYTD += used;

            // Total Carry Forward Remaining
            carryForwardRepository.findByEmployeeIdAndYear(emp.getId(), currentYear)
                    .ifPresent(cf -> response.setTotalCarryForwardBalance(response.getTotalCarryForwardBalance() + cf.getRemaining()));

            // Total Comp Off Balance
            compOffRepository.findByEmployeeIdAndYear(emp.getId(), currentYear)
                    .ifPresent(co -> response.setTotalCompOffBalance(response.getTotalCompOffBalance() + co.getBalance()));

            // Average LOP calculation
            Double lop = lopRepository.getTotalLossPercentageByEmployeeIdAndYear(emp.getId(), currentYear);
            if (lop != null && lop > 0) {
                totalLOP += lop;
                lopCount++;
            }
        }

        response.setTotalLeaveDaysUsedYTD(totalUsedYTD);
        response.setAverageLossOfPayPercentage(lopCount > 0 ? totalLOP / lopCount : 0.0);
    }

    public Map<LeaveStatus, Long> getLeaveCountsByStatus(Long employeeId, Integer year) {
        return applicationRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .collect(Collectors.groupingBy(LeaveApplication::getStatus, Collectors.counting()));
    }

    public List<EmployeeSummaryDTO> getEmployeesCurrentlyOnLeaveDTOs() {
        return getEmployeesCurrentlyOnLeave().stream().map(EmployeeSummaryDTO::from).toList();
    }

    public List<EmployeeSummaryDTO> getManagersWithUpcomingLeaveDTOs() {
        return getManagersWithUpcomingLeave().stream().map(EmployeeSummaryDTO::from).toList();
    }

    public List<EmployeeSummaryDTO> getAdminsWithUpcomingLeaveDTOs() {
        return getAdminsWithUpcomingLeave().stream().map(EmployeeSummaryDTO::from).toList();
    }

    public List<ManagerDashboardResponse.TeamPendingLeaveDTO> getPendingTeamRequestDTOs(Long managerId) {
        return getPendingTeamRequests(managerId).stream()
                .map(leave -> {
                    ManagerDashboardResponse.TeamPendingLeaveDTO dto = new ManagerDashboardResponse.TeamPendingLeaveDTO();
                    dto.setLeaveId(leave.getId());
                    dto.setEmployeeId(leave.getEmployeeId());
                    dto.setLeaveType(leave.getLeaveType());
                    dto.setStartDate(leave.getStartDate());
                    dto.setEndDate(leave.getEndDate());
                    dto.setStatus(leave.getStatus());
                    dto.setReason(leave.getReason());
                    return dto;
                }).toList();
    }
}