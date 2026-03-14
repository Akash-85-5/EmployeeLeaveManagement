package com.example.employeeLeaveApplication.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.example.employeeLeaveApplication.dto.*;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.employeeLeaveApplication.component.PolicyConstants;
import com.example.employeeLeaveApplication.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.entity.CompOffBalance;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.repository.CompOffBalanceRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveAllocationRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.repository.LossOfPayRecordRepository;

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

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE DASHBOARD (Reusable for Employee/Manager/Admin own stats)
    // ═══════════════════════════════════════════════════════════════

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

        // ═══════════════════════════════════════════════════════════════
        // 1. YEARLY STATS
        // ═══════════════════════════════════════════════════════════════

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

        log.info("   Yearly: Allocated={}, Used={}, Balance={}",
                yearlyAllocated, yearlyUsed, yearlyAllocated - yearlyUsed);

        // ═══════════════════════════════════════════════════════════════
        // 2. MONTHLY STATS
        // ═══════════════════════════════════════════════════════════════

        response.setMonthlyAllocated(PolicyConstants.MONTHLY_LIMIT);

        Double monthlyUsed = applicationRepository
                .getTotalApprovedDaysInMonth(employeeId, currentYear, currentMonth);
        if (monthlyUsed == null) monthlyUsed = 0.0;

        response.setMonthlyUsed(monthlyUsed);
        response.setMonthlyBalance(PolicyConstants.MONTHLY_LIMIT - monthlyUsed);

        // ═══════════════════════════════════════════════════════════════
        // 3. CARRY FORWARD
        // ═══════════════════════════════════════════════════════════════

        CarryForwardBalance cfBalance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElse(null);

        response.setCarryForwardTotal(cfBalance != null ? cfBalance.getTotalCarriedForward() : 0.0);
        response.setCarryForwardUsed(cfBalance != null ? cfBalance.getTotalUsed() : 0.0);
        response.setCarryForwardRemaining(cfBalance != null ? cfBalance.getRemaining() : 0.0);


        List<LeaveApplication> approvedLeaves = applicationRepository
                .findByEmployeeIdAndStatusAndYear(employeeId, LeaveStatus.APPROVED, currentYear);

        // Group approved leaves by LeaveType for O(1) lookup inside the loop
        Map<LeaveType, List<LeaveApplication>> byType = approvedLeaves.stream()
                .collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();

        for (LeaveAllocation allocation : allocations) {
            LeaveType type = allocation.getLeaveCategory();
            double allocated = allocation.getAllocatedDays();

            List<LeaveApplication> typeLeaves = byType.getOrDefault(type, List.of());

            double used = typeLeaves.stream()
                    .mapToDouble(l -> l.getDays().doubleValue())
                    .sum();

            int halfDays = (int) typeLeaves.stream()
                    .filter(l -> l.getDays().compareTo(new BigDecimal("0.5")) == 0)
                    .count();

            breakdown.add(new LeaveTypeBreakdown(
                    type,
                    allocated,
                    used,
                    allocated - used,   // per-type remaining, not grand-total
                    halfDays
            ));
        }

        // ─── Comp-Off in breakdown ────────────────────────────────────
        CompOffBalance compOff = compOffRepository
                .findByEmployeeIdAndYear(employeeId, currentYear).orElse(null);

        double coEarned = compOff != null ? compOff.getEarned()   : 0.0;
        double coUsed   = compOff != null ? compOff.getUsed()     : 0.0;
        double coBal    = compOff != null ? compOff.getBalance()  : 0.0;

        breakdown.add(new LeaveTypeBreakdown(LeaveType.COMP_OFF, coEarned, coUsed, coBal, 0));

        response.setBreakdown(breakdown);
        response.setCompoffBalance(coBal);

        // ═══════════════════════════════════════════════════════════════
        // 5. LOSS OF PAY
        // ═══════════════════════════════════════════════════════════════

        Double totalLOP = lopRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, currentYear);
        response.setLossOfPayPercentage(totalLOP != null ? totalLOP : 0.0);

        // ═══════════════════════════════════════════════════════════════
        // 6. LEAVE STATUS COUNTS
        // ═══════════════════════════════════════════════════════════════

        Integer approvedCount = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.APPROVED);
        Integer rejectedCount = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.REJECTED);
        Integer pendingCount  = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.PENDING);

        response.setApprovedCount(approvedCount != null ? approvedCount : 0);
        response.setRejectedCount(rejectedCount != null ? rejectedCount : 0);
        response.setPendingCount(pendingCount   != null ? pendingCount  : 0);

        log.info("✅ [DASHBOARD] Employee dashboard complete: allocated={}, used={}, breakdown={}",
                yearlyAllocated, yearlyUsed, breakdown.size());

        return response;
    }

    /**
     * Get monthly statistics for an employee
     */
    public MonthlyStatsResponse getMonthlyStats(Long employeeId, Integer year, Integer month) {

        log.info("📅 [DASHBOARD] Getting monthly stats: employee={}, year={}, month={}",
                employeeId, year, month);

        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        MonthlyStatsResponse response = new MonthlyStatsResponse();
        response.setEmployeeId(employeeId);
        response.setYear(year);
        response.setMonth(month);

        Double approvedDays = applicationRepository
                .getTotalApprovedDaysInMonth(employeeId, year, month);
        if (approvedDays == null) approvedDays = 0.0;

        response.setTotalApprovedCount(approvedDays.intValue());
        response.setExceededLimit(approvedDays > PolicyConstants.MONTHLY_LIMIT);

        log.info("✅ [DASHBOARD] Monthly stats: approvedDays={}, exceeded={}",
                approvedDays, response.getExceededLimit());

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

            Double allocated = allocationRepository
                    .getTotalAllocatedDays(member.getId(), year);
            balance.setTotalAllocated(allocated != null ? allocated : 0.0);

            Double used = applicationRepository
                    .getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, year);
            balance.setTotalUsed(used != null ? used : 0.0);

            double remaining = balance.getTotalAllocated() - balance.getTotalUsed();
            balance.setTotalRemaining(remaining);

            CompOffBalance compOff = compOffRepository
                    .findByEmployeeIdAndYear(member.getId(), year)
                    .orElse(null);
            balance.setCompOffBalance(compOff != null ? compOff.getBalance() : 0.0);

            Double lop = lopRepository
                    .getTotalLossPercentageByEmployeeIdAndYear(member.getId(), year);
            balance.setLopPercentage(lop != null ? lop : 0.0);

            balances.add(balance);
        }

        log.info("✅ [DASHBOARD-MANAGER] Retrieved {} team member balances", balances.size());

        return balances;
    }

    public List<TeamMemberBalance> getTeamMembersOnLeaveToday(Long managerId) {

        log.info("🏖️ [DASHBOARD-MANAGER] Getting team members on leave today for manager: {}", managerId);

        LocalDate today = LocalDate.now();
        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        List<TeamMemberBalance> onLeave = new ArrayList<>();

        for (Employee member : teamMembers) {
            List<LeaveApplication> todayLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) &&
                            !today.isAfter(la.getEndDate()))
                    .collect(Collectors.toList());

            if (!todayLeaves.isEmpty()) {
                TeamMemberBalance balance = new TeamMemberBalance();
                balance.setEmployeeId(member.getId());
                balance.setEmployeeName(member.getName());

                double totalDays = todayLeaves.stream()
                        .mapToDouble(la -> la.getDays().doubleValue())
                        .sum();

                balance.setTotalUsed(totalDays);
                onLeave.add(balance);
            }
        }

        log.info("✅ [DASHBOARD-MANAGER] {} team members on leave today", onLeave.size());

        return onLeave;
    }

    public Map<String, List<TeamMemberBalance>> getTeamLeaveCalendar(Long managerId) {
        log.info("📅 [DASHBOARD-MANAGER] Getting full team leave calendar for manager: {}", managerId);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        Map<String, List<TeamMemberBalance>> calendar = new TreeMap<>(); // TreeMap keeps dates sorted

        for (Employee member : teamMembers) {
            List<LeaveApplication> approvedLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED);

            for (LeaveApplication leave : approvedLeaves) {
                LocalDate leaveDate = leave.getStartDate();
                // Loop through the entire duration of each leave
                while (!leaveDate.isAfter(leave.getEndDate())) {
                    String dateKey = leaveDate.toString();

                    calendar.computeIfAbsent(dateKey, k -> new ArrayList<>());

                    TeamMemberBalance balance = new TeamMemberBalance();
                    balance.setEmployeeId(member.getId());
                    balance.setEmployeeName(member.getName());
                    calendar.get(dateKey).add(balance);

                    leaveDate = leaveDate.plusDays(1);
                }
            }
        }
        return calendar;
    }

    public Map<String, List<TeamMemberBalance>> getMyLeaveCalendar(Long employeeId) {
        log.info("📅 [DASHBOARD-EMPLOYEE] Getting personal leave calendar for employee: {}", employeeId);

        Employee member = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Map<String, List<TeamMemberBalance>> calendar = new TreeMap<>();
        List<LeaveApplication> approvedLeaves = applicationRepository
                .findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED);

        for (LeaveApplication leave : approvedLeaves) {
            LocalDate leaveDate = leave.getStartDate();
            while (!leaveDate.isAfter(leave.getEndDate())) {
                String dateKey = leaveDate.toString();

                calendar.computeIfAbsent(dateKey, k -> new ArrayList<>());

                TeamMemberBalance balance = new TeamMemberBalance();
                balance.setEmployeeId(member.getId());
                balance.setEmployeeName(member.getName());
                // Add leave type or status here if your TeamMemberBalance supports it
                calendar.get(dateKey).add(balance);

                leaveDate = leaveDate.plusDays(1);
            }
        }
        return calendar;
    }


    public Integer getPendingTeamRequestsCount(Long managerId) {

        log.info("🔔 [DASHBOARD-MANAGER] Getting pending team requests count for manager: {}", managerId);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        int totalPending = 0;

        for (Employee member : teamMembers) {
            totalPending += applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.PENDING).size();
        }

        log.info("✅ [DASHBOARD-MANAGER] {} pending team requests", totalPending);

        return totalPending;
    }

    public List<LeaveApplication> getPendingTeamRequests(Long managerId) {

        log.info("📋 [DASHBOARD-MANAGER] Getting pending team requests for manager: {}", managerId);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        List<LeaveApplication> allPending = new ArrayList<>();

        for (Employee member : teamMembers) {
            allPending.addAll(applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.PENDING));
        }

        allPending.sort(Comparator.comparing(LeaveApplication::getCreatedAt));

        log.info("✅ [DASHBOARD-MANAGER] Retrieved {} pending team requests", allPending.size());

        return allPending;
    }
    @Transactional(readOnly = true)
    public TeamLeaderDashboardResponse getTeamLeaderDashboard(Long teamLeaderId) {

        log.info("🧑‍💼 [DASHBOARD-TEAMLEADER] Building dashboard for team leader: {}", teamLeaderId);

        // Validate team leader exists and has TEAM_LEADER role
        Employee teamLeader = employeeRepository.findById(teamLeaderId)
                .orElseThrow(() -> new RuntimeException("Team Leader not found: " + teamLeaderId));

        if (teamLeader.getRole() != Role.TEAM_LEADER) {
            throw new RuntimeException("Employee " + teamLeaderId + " is not a Team Leader");
        }

        // ── 1. Team Leader's own personal leave stats ────────────────
        EmployeeDashboardResponse ownStats = getDashboard(teamLeaderId);

        TeamLeaderDashboardResponse response = new TeamLeaderDashboardResponse();
        response.setPersonalStats(ownStats);

        // ── 2. Fetch team members assigned to this team leader ───────
        List<Employee> teamMembers = employeeRepository
                .findActiveTeamMembersByTeamLeader(teamLeaderId);
        response.setTeamSize(teamMembers.size());

        log.info("   [TEAMLEADER] Team size: {}", teamMembers.size());

        // ── 3. Pending leave requests (waiting for TL first-level approval) ──
        List<TeamLeaderDashboardResponse.TeamPendingLeaveDTO> pendingDTOs = new ArrayList<>();

        for (Employee member : teamMembers) {
            List<LeaveApplication> memberPending = applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.PENDING);

            for (LeaveApplication leave : memberPending) {
                pendingDTOs.add(new TeamLeaderDashboardResponse.TeamPendingLeaveDTO(
                        leave.getId(),
                        leave.getEmployeeId(),
                        member.getName(),
                        leave.getLeaveType(),
                        leave.getReason(),
                        leave.getStatus(),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        leave.getDays().doubleValue(),
                        leave.getCreatedAt()
                ));
            }
        }

        // Sort oldest first so urgent requests appear at the top
        pendingDTOs.sort(Comparator.comparing(
                TeamLeaderDashboardResponse.TeamPendingLeaveDTO::getAppliedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        response.setPendingTeamRequests(pendingDTOs);
        response.setTeamPendingRequestCount(pendingDTOs.size());

        log.info("   [TEAMLEADER] Pending requests: {}", pendingDTOs.size());

        // ── 4. Team members on leave today ───────────────────────────
        LocalDate today = LocalDate.now();
        List<TeamLeaderDashboardResponse.TeamMemberOnLeaveDTO> onLeaveDTOs = new ArrayList<>();

        for (Employee member : teamMembers) {
            applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) &&
                            !today.isAfter(la.getEndDate()))
                    .forEach(leave -> {
                        long daysRemaining = ChronoUnit.DAYS.between(today, leave.getEndDate());
                        onLeaveDTOs.add(new TeamLeaderDashboardResponse.TeamMemberOnLeaveDTO(
                                member.getId(),
                                member.getName(),
                                leave.getLeaveType().name(),
                                leave.getStartDate(),
                                leave.getEndDate(),
                                (double) Math.max(0, daysRemaining)
                        ));
                    });
        }

        response.setTeamOnLeaveToday(onLeaveDTOs);
        response.setTeamOnLeaveCount(onLeaveDTOs.size());

        log.info("   [TEAMLEADER] On leave today: {}", onLeaveDTOs.size());

        // ── 5. Team leave balance summary (per member) ───────────────
        int currentYear = LocalDate.now().getYear();
        List<TeamLeaderDashboardResponse.TeamMemberBalanceSummaryDTO> balanceSummaries = new ArrayList<>();

        for (Employee member : teamMembers) {

            Double allocated = allocationRepository
                    .getTotalAllocatedDays(member.getId(), currentYear);
            Double used = applicationRepository
                    .getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, currentYear);

            if (allocated == null) allocated = 0.0;
            if (used == null)      used      = 0.0;

            CompOffBalance compOff = compOffRepository
                    .findByEmployeeIdAndYear(member.getId(), currentYear).orElse(null);
            double compOffBal = (compOff != null) ? compOff.getBalance() : 0.0;

            Double lop = lopRepository
                    .getTotalLossPercentageByEmployeeIdAndYear(member.getId(), currentYear);

            balanceSummaries.add(new TeamLeaderDashboardResponse.TeamMemberBalanceSummaryDTO(
                    member.getId(),
                    member.getName(),
                    allocated,
                    used,
                    allocated - used,
                    compOffBal,
                    lop != null ? lop : 0.0
            ));
        }

        response.setTeamBalances(balanceSummaries);
        response.setLastUpdated(LocalDateTime.now());

        log.info("✅ [DASHBOARD-TEAMLEADER] Dashboard complete for team leader: {}", teamLeaderId);

        return response;
    }



    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD - Company-wide View
    // ═══════════════════════════════════════════════════════════════

    public Map<String, Object> getCompanyWideStats(Integer year) {

        log.info("📊 [DASHBOARD-HR] Getting company-wide stats for year: {}", year);

        Map<String, Object> stats = new HashMap<>();

        List<Employee> activeEmployees = employeeRepository.findActiveEmployees();
        stats.put("totalEmployees", activeEmployees.size());

        int    totalApproved = 0;
        double totalDaysUsed = 0.0;

        for (Employee emp : activeEmployees) {
            Double used = applicationRepository
                    .getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, year);
            if (used != null) {
                totalDaysUsed += used;
                totalApproved++;
            }
        }

        stats.put("totalApprovedLeaves", totalApproved);
        stats.put("totalDaysUsed", totalDaysUsed);

        double totalLOP = 0.0;
        int    lopCount = 0;

        for (Employee emp : activeEmployees) {
            Double lop = lopRepository
                    .getTotalLossPercentageByEmployeeIdAndYear(emp.getId(), year);
            if (lop != null && lop > 0) {
                totalLOP += lop;
                lopCount++;
            }
        }

        stats.put("totalLopPercentage", lopCount > 0 ? totalLOP / lopCount : 0.0);
        stats.put("employeesWithLOP",   lopCount);

        double cfUsedTotal  = 0.0;
        double cfTotalTotal = 0.0;

        for (Employee emp : activeEmployees) {
            CarryForwardBalance cf = carryForwardRepository
                    .findByEmployeeIdAndYear(emp.getId(), year).orElse(null);
            if (cf != null) {
                cfUsedTotal  += cf.getTotalUsed();
                cfTotalTotal += cf.getTotalCarriedForward();
            }
        }

        double cfUtilization = (cfTotalTotal > 0) ? (cfUsedTotal / cfTotalTotal * 100) : 0.0;
        stats.put("carryForwardUtilization", cfUtilization);

        log.info("✅ [DASHBOARD-HR] Company-wide stats calculated");

        return stats;
    }

    public List<Employee> getEmployeesCurrentlyOnLeave() {

        log.info("🏖️ [DASHBOARD-HR] Getting employees currently on leave");

        LocalDate today = LocalDate.now();
        List<Employee> activeEmployees = employeeRepository.findActiveEmployees();
        List<Employee> onLeave = new ArrayList<>();

        for (Employee emp : activeEmployees) {
            List<LeaveApplication> todayLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(emp.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) &&
                            !today.isAfter(la.getEndDate()))
                    .collect(Collectors.toList());

            if (!todayLeaves.isEmpty()) {
                onLeave.add(emp);
            }
        }

        log.info("✅ [DASHBOARD-HR] {} employees currently on leave", onLeave.size());

        return onLeave;
    }

    public List<Employee> getManagersWithUpcomingLeave() {

        log.info("👔 [DASHBOARD-HR] Getting managers with upcoming leave");

        LocalDate today    = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Employee> managers = employeeRepository.findByRole(Role.MANAGER);
        List<Employee> managersWithLeave = new ArrayList<>();

        for (Employee manager : managers) {
            boolean hasUpcoming = applicationRepository
                    .findByEmployeeIdAndStatus(manager.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .anyMatch(la -> !la.getStartDate().isBefore(today) &&
                            !la.getStartDate().isAfter(nextWeek));

            if (hasUpcoming) managersWithLeave.add(manager);
        }

        log.info("✅ [DASHBOARD-HR] {} managers with upcoming leave", managersWithLeave.size());

        return managersWithLeave;
    }

    public List<Employee> getAdminsWithUpcomingLeave() {

        log.info("⚙️ [DASHBOARD-HR] Getting admins with upcoming leave");

        LocalDate today    = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Employee> admins = employeeRepository.findByRole(Role.ADMIN);
        List<Employee> adminsWithLeave = new ArrayList<>();

        for (Employee admin : admins) {
            boolean hasUpcoming = applicationRepository
                    .findByEmployeeIdAndStatus(admin.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .anyMatch(la -> !la.getStartDate().isBefore(today) &&
                            !la.getStartDate().isAfter(nextWeek));

            if (hasUpcoming) adminsWithLeave.add(admin);
        }

        log.info("✅ [DASHBOARD-HR] {} admins with upcoming leave", adminsWithLeave.size());

        return adminsWithLeave;
    }

    public List<TeamMemberBalance> getEmployeesWithLowBalance(Integer year, Double threshold) {

        log.info("⚠️ [DASHBOARD-HR] Getting employees with balance < {}", threshold);

        List<Employee> allEmployees = employeeRepository.findActiveEmployees();
        List<TeamMemberBalance> lowBalance = new ArrayList<>();

        for (Employee emp : allEmployees) {
            Double allocated = allocationRepository.getTotalAllocatedDays(emp.getId(), year);
            Double used      = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, year);

            if (allocated == null) allocated = 0.0;
            if (used == null)      used      = 0.0;

            Double remaining = allocated - used;

            if (remaining < threshold) {
                TeamMemberBalance balance = new TeamMemberBalance();
                balance.setEmployeeId(emp.getId());
                balance.setEmployeeName(emp.getName());
                balance.setTotalAllocated(allocated);
                balance.setTotalUsed(used);
                balance.setTotalRemaining(remaining);
                lowBalance.add(balance);
            }
        }

        log.info("✅ [DASHBOARD-HR] {} employees with low balance", lowBalance.size());

        return lowBalance;
    }

    public List<TeamMemberBalance> getEmployeesWithHighLOP(Integer year, Double threshold) {

        log.info("⚠️ [DASHBOARD-HR] Getting employees with LOP > {}", threshold);

        List<Employee> allEmployees = employeeRepository.findActiveEmployees();
        List<TeamMemberBalance> highLOP = new ArrayList<>();

        for (Employee emp : allEmployees) {
            Double lop = lopRepository
                    .getTotalLossPercentageByEmployeeIdAndYear(emp.getId(), year);

            if (lop != null && lop > threshold) {
                TeamMemberBalance balance = new TeamMemberBalance();
                balance.setEmployeeId(emp.getId());
                balance.setEmployeeName(emp.getName());
                balance.setLopPercentage(lop);
                highLOP.add(balance);
            }
        }

        log.info("✅ [DASHBOARD-HR] {} employees with high LOP", highLOP.size());

        return highLOP;
    }

    public List<TeamMemberBalance> getCarryForwardEligible(Integer year) {

        log.info("📋 [DASHBOARD-HR] Getting carry forward eligible employees for year: {}", year);

        List<Employee> allEmployees = employeeRepository.findActiveEmployees();
        List<TeamMemberBalance> eligible = new ArrayList<>();

        for (Employee emp : allEmployees) {
            Double allocated = allocationRepository.getTotalAllocatedDays(emp.getId(), year);
            Double used      = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, year);

            if (allocated == null) allocated = 0.0;
            if (used == null)      used      = 0.0;

            Double balance = allocated - used;

            if (balance > 0) {
                double eligibleAmount = Math.min(balance, PolicyConstants.MAX_CARRY_FORWARD);

                TeamMemberBalance tmb = new TeamMemberBalance();
                tmb.setEmployeeId(emp.getId());
                tmb.setEmployeeName(emp.getName());
                tmb.setTotalAllocated(allocated);
                tmb.setTotalUsed(used);
                tmb.setTotalRemaining(eligibleAmount);
                eligible.add(tmb);
            }
        }

        log.info("✅ [DASHBOARD-HR] {} employees eligible for carry forward", eligible.size());

        return eligible;
    }

    public EmployeeDashboardResponse getEmployeeDashboard(Long employeeId) {
        return getDashboard(employeeId);
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN — Employees Exceeding Monthly Limit
    // ═══════════════════════════════════════════════════════════════

    public List<TeamMemberBalance> getEmployeesExceedingMonthlyLimit(Integer year, Integer month) {

        log.info("⚠️ [DASHBOARD-ADMIN] Getting employees exceeding monthly limit for {}/{}", year, month);

        List<Employee> allEmployees = employeeRepository.findActiveEmployees();
        List<TeamMemberBalance> exceeding = new ArrayList<>();

        for (Employee emp : allEmployees) {
            Double approvedDays = applicationRepository
                    .getTotalApprovedDaysInMonth(emp.getId(), year, month);

            if (approvedDays != null && approvedDays > PolicyConstants.MONTHLY_LIMIT) {
                TeamMemberBalance balance = new TeamMemberBalance();
                balance.setEmployeeId(emp.getId());
                balance.setEmployeeName(emp.getName());
                balance.setTotalUsed(approvedDays);
                exceeding.add(balance);
            }
        }

        log.info("✅ [DASHBOARD-ADMIN] {} employees exceeding monthly limit", exceeding.size());

        return exceeding;
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    public Map<LeaveStatus, Long> getLeaveCountsByStatus(Long employeeId, Integer year) {

        log.info("📊 [DASHBOARD] Getting leave counts by status: employee={}, year={}",
                employeeId, year);

        List<LeaveApplication> allLeaves = applicationRepository
                .findByEmployeeIdAndYear(employeeId, year);

        Map<LeaveStatus, Long> counts = allLeaves.stream()
                .collect(Collectors.groupingBy(LeaveApplication::getStatus, Collectors.counting()));

        log.info("✅ [DASHBOARD] Leave counts: {}", counts);

        return counts;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPREHENSIVE MANAGER DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(Long managerId) {

        log.info("👔 [DASHBOARD-MANAGER] Building optimized dashboard for manager: {}", managerId);

        EmployeeDashboardResponse ownStats = getDashboard(managerId);

        ManagerDashboardResponse response = new ManagerDashboardResponse();
        response.setPersonalStats(ownStats);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        response.setTeamSize(teamMembers.size());

        List<LeaveApplication> pendingRequests = applicationRepository
                .findPendingTeamRequests(managerId);
        response.setTeamPendingRequestCount(pendingRequests.size());

        List<ManagerDashboardResponse.TeamPendingLeaveDTO> pendingDTOs = pendingRequests.stream()
                .map(leave -> {
                    Employee emp = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
                    return new ManagerDashboardResponse.TeamPendingLeaveDTO(
                            leave.getId(),
                            leave.getEmployeeId(),
                            emp != null ? emp.getName() : "Unknown",
                            leave.getLeaveType(),
                            leave.getReason(),
                            leave.getStatus(),
                            leave.getStartDate(),
                            leave.getEndDate(),
                            leave.getDays().doubleValue(),
                            leave.getCreatedAt()
                    );
                }).collect(Collectors.toList());

        response.setPendingTeamRequests(pendingDTOs);

        LocalDate today = LocalDate.now();
        List<ManagerDashboardResponse.TeamMemberOnLeaveDTO> onLeaveDTOs = new ArrayList<>();

        for (Employee member : teamMembers) {
            applicationRepository.findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) &&
                            !today.isAfter(la.getEndDate()))
                    .forEach(leave -> {
                        long daysRemaining = ChronoUnit.DAYS.between(today, leave.getEndDate());
                        onLeaveDTOs.add(new ManagerDashboardResponse.TeamMemberOnLeaveDTO(
                                member.getId(),
                                member.getName(),
                                leave.getLeaveType().name(),
                                leave.getStartDate(),
                                leave.getEndDate(),
                                (double) Math.max(0, daysRemaining)
                        ));
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

        log.info("🏢 [DASHBOARD-HR] Building comprehensive HR dashboard");

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
                    .findByEmployeeIdAndStatusAndYear(emp.getId(), LeaveStatus.APPROVED, currentYear)
                    .size();
        }
        response.setTotalApprovedLeaves(approvedCount);

        // Onboarding
        List<Employee> onboardingPending = employeeRepository.findOnboardingPending();
        response.setNewEmployeesCount(onboardingPending.size());
        response.setPendingBiometricCount(employeeRepository.countPendingBiometric());
        response.setPendingVPNCount(employeeRepository.countPendingVPN());

        List<HRDashboardResponse.OnboardingPendingDTO> onboardingDTOs = new ArrayList<>();
        for (Employee emp : onboardingPending) {
            int daysInOnboarding = (int) ChronoUnit.DAYS.between(emp.getJoiningDate(), LocalDate.now());
            onboardingDTOs.add(new HRDashboardResponse.OnboardingPendingDTO(
                    emp.getId(), emp.getName(), emp.getEmail(),
                    emp.getJoiningDate(), emp.getBiometricStatus(),
                    emp.getVpnStatus(), daysInOnboarding
            ));
        }
        response.setOnboardingPendingList(onboardingDTOs);

        // Employees on leave
        List<LeaveApplication> leavesInRange = applicationRepository
                .findApprovedLeavesInDateRange(today, today);

        List<HRDashboardResponse.EmployeeOnLeaveDTO> onLeaveDTOs = new ArrayList<>();
        for (LeaveApplication leave : leavesInRange) {
            Employee emp = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
            if (emp != null) {
                Employee manager  = emp.getManagerId() != null ?
                        employeeRepository.findById(emp.getManagerId()).orElse(null) : null;
                Employee approver = leave.getApprovedBy() != null ?
                        employeeRepository.findById(leave.getApprovedBy()).orElse(null) : null;

                onLeaveDTOs.add(new HRDashboardResponse.EmployeeOnLeaveDTO(
                        emp.getId(), emp.getName(),
                        manager  != null ? manager.getName()  : "N/A",
                        leave.getLeaveType().name(),
                        leave.getStartDate(), leave.getEndDate(),
                        leave.getDays().doubleValue(),
                        leave.getApprovedAt() != null ? leave.getApprovedAt().toLocalDate() : null,
                        approver != null ? approver.getName() : "N/A"
                ));
            }
        }
        response.setEmployeesOnLeave(onLeaveDTOs);

        // Manager insights
        List<Long> managerIds = applicationRepository.findManagersWhoApprovedLeaves(currentYear);
        response.setTotalManagersWithApprovals(managerIds.size());

        List<HRDashboardResponse.ManagerApprovalStatsDTO> managerStats = new ArrayList<>();
        for (Long mgrId : managerIds) {
            Employee mgr = employeeRepository.findById(mgrId).orElse(null);
            if (mgr != null) {
                List<Employee>         teamMembers      = employeeRepository.findTeamMembersByManager(mgrId);
                List<LeaveApplication> managerApprovals = applicationRepository.findLeavesApprovedByManager(mgrId, currentYear);

                int teamPending = 0;
                for (Employee tm : teamMembers) {
                    teamPending += applicationRepository
                            .findByEmployeeIdAndStatus(tm.getId(), LeaveStatus.PENDING).size();
                }

                int approvalRate = !teamMembers.isEmpty() ?
                        (int) ((managerApprovals.size() / (double) teamMembers.size()) * 100) : 0;

                LocalDateTime lastApproval = managerApprovals.isEmpty() ? null :
                        managerApprovals.get(0).getApprovedAt();

                managerStats.add(new HRDashboardResponse.ManagerApprovalStatsDTO(
                        mgrId, mgr.getName(), teamMembers.size(),
                        managerApprovals.size(), teamPending, approvalRate,
                        lastApproval != null ? lastApproval.toLocalDate() : null
                ));
            }
        }
        response.setManagerApprovalStats(managerStats);

        // Team structure
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
                double cfBal = cf != null ? cf.getRemaining() : 0.0;

                CompOffBalance compOff = compOffRepository
                        .findByEmployeeIdAndYear(member.getId(), currentYear).orElse(null);
                double compOffBal = compOff != null ? compOff.getBalance() : 0.0;

                teamMemberDTOs.add(new HRDashboardResponse.TeamMemberDTO(
                        member.getId(), member.getName(), member.getEmail(),
                        (yearlyBal  != null ? yearlyBal  : 0.0) -
                                (yearlyUsed != null ? yearlyUsed : 0.0),
                        cfBal, compOffBal
                ));
            }

            teamStructure.add(new HRDashboardResponse.TeamStructureDTO(
                    manager.getId(), manager.getName(),
                    teamMembers.size(), teamMemberDTOs
            ));
        }

        response.setTeamStructure(teamStructure);
        response.setLastUpdated(LocalDateTime.now());

        log.info("✅ [DASHBOARD-HR] HR dashboard complete");

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPREHENSIVE ADMIN DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard(Long adminId) {

        log.info("⚙️ [DASHBOARD-ADMIN] Building comprehensive admin dashboard for: {}", adminId);

        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminId));

        int currentYear = LocalDate.now().getYear();
        AdminDashboardResponse response = new AdminDashboardResponse();

        response.setCurrentYear(currentYear);
        response.setAdminId(adminId);
        response.setAdminName(admin.getName());

        // Admin's own stats
        EmployeeDashboardResponse ownStats = getDashboard(adminId);
        response.setYearlyBalance(ownStats.getYearlyBalance());
        response.setCarryForwardBalance(ownStats.getCarryForwardRemaining());
        response.setCompOffBalance(ownStats.getCompoffBalance());
        response.setApprovedLeaveCount(ownStats.getApprovedCount());
        response.setPendingLeaveCount(ownStats.getPendingCount());

        // Compliance metrics
        List<Employee> allEmployees = employeeRepository.findByActiveTrue();
        response.setTotalEmployees(allEmployees.size());

        List<Employee> managers = employeeRepository.findByRole(Role.MANAGER);
        response.setTotalManagers(managers.size());

        List<Employee> onboardingPending = employeeRepository.findOnboardingPending();
        response.setNewEmployeesPendingOnboarding(onboardingPending.size());

        List<LeaveApplication> pendingLeaves  = applicationRepository.findByStatus(LeaveStatus.PENDING);
        List<LeaveApplication> rejectedLeaves = applicationRepository.findByStatus(LeaveStatus.REJECTED);
        response.setTotalPendingLeaves(pendingLeaves.size());
        response.setTotalRejectedLeaves(rejectedLeaves.size());

        // Leave statistics
        double totalUsedYTD        = 0.0;
        double totalCFBalance      = 0.0;
        double totalCompOffBalance = 0.0;
        double totalLOP            = 0.0;
        int    lopCount            = 0;

        for (Employee emp : allEmployees) {
            Double used = applicationRepository
                    .getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, currentYear);
            if (used != null) totalUsedYTD += used;

            CarryForwardBalance cf = carryForwardRepository
                    .findByEmployeeIdAndYear(emp.getId(), currentYear).orElse(null);
            if (cf != null) totalCFBalance += cf.getRemaining();

            CompOffBalance compOff = compOffRepository
                    .findByEmployeeIdAndYear(emp.getId(), currentYear).orElse(null);
            if (compOff != null) totalCompOffBalance += compOff.getBalance();

            Double lop = lopRepository
                    .getTotalLossPercentageByEmployeeIdAndYear(emp.getId(), currentYear);
            if (lop != null && lop > 0) {
                totalLOP += lop;
                lopCount++;
            }
        }

        response.setTotalLeaveDaysUsedYTD(totalUsedYTD);
        response.setTotalCarryForwardBalance(totalCFBalance);
        response.setTotalCompOffBalance(totalCompOffBalance);
        response.setAverageLossOfPayPercentage(lopCount > 0 ? totalLOP / lopCount : 0.0);
        response.setLastUpdated(LocalDateTime.now());

        log.info("✅ [DASHBOARD-ADMIN] AdminDashboard complete");

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // DTO HELPERS
    // ═══════════════════════════════════════════════════════════════

    public List<EmployeeSummaryDTO> getEmployeesCurrentlyOnLeaveDTOs() {
        return getEmployeesCurrentlyOnLeave().stream()
                .map(EmployeeSummaryDTO::from)
                .toList();
    }

    public List<EmployeeSummaryDTO> getManagersWithUpcomingLeaveDTOs() {
        return getManagersWithUpcomingLeave().stream()
                .map(EmployeeSummaryDTO::from)
                .toList();
    }

    public List<EmployeeSummaryDTO> getAdminsWithUpcomingLeaveDTOs() {
        return getAdminsWithUpcomingLeave().stream()
                .map(EmployeeSummaryDTO::from)
                .toList();
    }

    public List<ManagerDashboardResponse.TeamPendingLeaveDTO> getPendingTeamRequestDTOs(Long managerId) {
        return getPendingTeamRequests(managerId).stream()
                .map(leave -> {
                    ManagerDashboardResponse.TeamPendingLeaveDTO dto =
                            new ManagerDashboardResponse.TeamPendingLeaveDTO();
                    dto.setLeaveId(leave.getId());
                    dto.setEmployeeId(leave.getEmployeeId());
                    dto.setLeaveType(leave.getLeaveType());
                    dto.setStartDate(leave.getStartDate());
                    dto.setEndDate(leave.getEndDate());
                    dto.setStatus(leave.getStatus());
                    dto.setReason(leave.getReason());
                    return dto;
                })
                .toList();
    }
}