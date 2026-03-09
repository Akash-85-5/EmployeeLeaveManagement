package com.example.employeeLeaveApplication.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.employeeLeaveApplication.dto.*;
import com.example.employeeLeaveApplication.enums.LeaveType;
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

    /**
     * Get dashboard for any employee
     * Reusable for: Employee, Manager (own stats), Admin (own stats)
     */
    public EmployeeDashboardResponse getDashboard(Long employeeId) {

        log.info("📊 [DASHBOARD] Getting employee dashboard: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        EmployeeDashboardResponse response = new EmployeeDashboardResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setCurrentYear(currentYear);
        response.setLastUpdated(LocalDateTime.now());

        // ═══════════════════════════════════════════════════════════
        // 1. YEARLY STATS
        // ═══════════════════════════════════════════════════════════

        List<LeaveAllocation> allocations = allocationRepository
                .findByEmployeeIdAndYear(employeeId, currentYear);
        List<LeaveApplication> approvedLeaves = applicationRepository
                .findByEmployeeIdAndStatusAndYear(employeeId, LeaveStatus.APPROVED, currentYear);

        // Grouping by LeaveType to calculate breakdown logic
        Map<LeaveType, List<LeaveApplication>> byType = approvedLeaves.stream()
                .collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        double yearlyAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        Double yearlyUsed = applicationRepository.getTotalUsedDays(
                employeeId, LeaveStatus.APPROVED, currentYear);
        if (yearlyUsed == null) yearlyUsed = 0.0;

        double yearlyBalance = yearlyAllocated - yearlyUsed;

        response.setYearlyAllocated(yearlyAllocated);
        response.setYearlyUsed(yearlyUsed);
        response.setYearlyBalance(yearlyBalance);

        log.info("   Yearly: Allocated={}, Used={}, Balance={}",
                yearlyAllocated, yearlyUsed, yearlyBalance);

        // ═══════════════════════════════════════════════════════════
        // 2. MONTHLY STATS
        // ═══════════════════════════════════════════════════════════

        response.setMonthlyAllocated(PolicyConstants.MONTHLY_LIMIT);

        Integer approvedThisMonth = applicationRepository
                .countApprovedInMonth(employeeId, currentYear, currentMonth);

        double monthlyUsed = approvedThisMonth != null ? approvedThisMonth.doubleValue() : 0.0;
        response.setMonthlyUsed(monthlyUsed);
        response.setMonthlyBalance(PolicyConstants.MONTHLY_LIMIT - monthlyUsed);

        log.info("   Monthly: Allocated={}, Used={}, Balance={}",
                PolicyConstants.MONTHLY_LIMIT, monthlyUsed,
                PolicyConstants.MONTHLY_LIMIT - monthlyUsed);

        // ═══════════════════════════════════════════════════════════
        // 3. CARRY FORWARD
        // ═══════════════════════════════════════════════════════════

        CarryForwardBalance cfBalance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElse(null);

        if (cfBalance != null) {
            response.setCarryForwardTotal(cfBalance.getTotalCarriedForward());
            response.setCarryForwardUsed(cfBalance.getTotalUsed());
            response.setCarryForwardRemaining(cfBalance.getRemaining());
        } else {
            response.setCarryForwardTotal(0.0);
            response.setCarryForwardUsed(0.0);
            response.setCarryForwardRemaining(0.0);
        }


        // ═══════════════════════════════════════════════════════════
        // 3. BUILD LEAVE TYPE BREAKDOWN (For Donut Chart)
        // ═══════════════════════════════════════════════════════════
        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();

        // Standard Allocations (Vacation, Sick, etc.)
        for (LeaveAllocation alloc : allocations) {
            LeaveType type = alloc.getLeaveCategory();
            double allocated = alloc.getAllocatedDays();

            double used = byType.getOrDefault(type, List.of()).stream()
                    .mapToDouble(l -> l.getDays().doubleValue()).sum();

            long halfDays = byType.getOrDefault(type, List.of()).stream()
                    .filter(l -> l.getDays().compareTo(new BigDecimal("0.5")) == 0).count();

            breakdown.add(new LeaveTypeBreakdown(type, allocated, used, allocated - used, (int) halfDays));
        }

        // Include Comp-Off in Breakdown
        CompOffBalance compOff = compOffRepository
                .findByEmployeeIdAndYear(employeeId, currentYear).orElse(null);

        double coEarned = (compOff != null) ? compOff.getEarned() : 0.0;
        double coUsed = (compOff != null) ? compOff.getUsed() : 0.0;
        double coBal = (compOff != null) ? compOff.getBalance() : 0.0;

        breakdown.add(new LeaveTypeBreakdown(LeaveType.COMP_OFF, coEarned, coUsed, coBal, 0));
        response.setBreakdown(breakdown);
        response.setCompoffBalance(coBal);

        // ═══════════════════════════════════════════════════════════
        // 5. LOSS OF PAY
        // ═══════════════════════════════════════════════════════════

        Double totalLOP = lopRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, currentYear);
        response.setLossOfPayPercentage(totalLOP != null ? totalLOP : 0.0);


        // ═══════════════════════════════════════════════════════════
        // 6. LEAVE STATUS COUNTS
        // ═══════════════════════════════════════════════════════════

        Integer approvedCountVal = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.APPROVED);
        Integer rejectedCountVal = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.REJECTED);
        Integer pendingCountVal = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.PENDING);

        response.setApprovedCount(approvedCountVal != null ? approvedCountVal : 0);
        response.setRejectedCount(rejectedCountVal != null ? rejectedCountVal : 0);
        response.setPendingCount(pendingCountVal != null ? pendingCountVal : 0);

        log.info("✅ [DASHBOARD] Employee dashboard complete");

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

        // Count approved leaves in this month
        Integer approvedCount = applicationRepository
                .countApprovedInMonth(employeeId, year, month);

        response.setTotalApprovedCount(approvedCount != null ? approvedCount : 0);
        response.setExceededLimit(response.getTotalApprovedCount() > PolicyConstants.MONTHLY_LIMIT);

        log.info("✅ [DASHBOARD] Monthly stats: approved={}, exceeded={}",
                response.getTotalApprovedCount(), response.getExceededLimit());

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // MANAGER DASHBOARD - Team View
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get team members' leave balance summary
     */
    public List<TeamMemberBalance> getTeamBalances(Long managerId, Integer year) {

        log.info("👥 [DASHBOARD-MANAGER] Getting team balances for manager: {}", managerId);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        List<TeamMemberBalance> balances = new ArrayList<>();

        for (Employee member : teamMembers) {

            TeamMemberBalance balance = new TeamMemberBalance();
            balance.setEmployeeId(member.getId());
            balance.setEmployeeName(member.getName());

            // Get allocations
            Double allocated = allocationRepository
                    .getTotalAllocatedDays(member.getId(), year);
            balance.setTotalAllocated(allocated != null ? allocated : 0.0);

            // Get used
            Double used = applicationRepository
                    .getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, year);
            balance.setTotalUsed(used != null ? used : 0.0);

            // Calculate remaining
            double remaining = balance.getTotalAllocated() - balance.getTotalUsed();
            balance.setTotalRemaining(remaining);

            // Get comp-off
            CompOffBalance compOff = compOffRepository
                    .findByEmployeeIdAndYear(member.getId(), year)
                    .orElse(null);
            Double compOffBal = compOff != null ? compOff.getBalance() : null;
            balance.setCompOffBalance(compOffBal != null ? compOffBal : 0.0);

            // Get LOP
            Double lop = lopRepository
                    .getTotalLossPercentageByEmployeeIdAndYear(member.getId(), year);
            balance.setLopPercentage(lop != null ? lop : 0.0);

            balances.add(balance);
        }

        log.info("✅ [DASHBOARD-MANAGER] Retrieved {} team member balances", balances.size());

        return balances;
    }

    /**
     * Get team members currently on leave TODAY
     */
    public List<TeamMemberBalance> getTeamMembersOnLeaveToday(Long managerId) {

        log.info("🏖️ [DASHBOARD-MANAGER] Getting team members on leave today for manager: {}", managerId);

        LocalDate today = LocalDate.now();
        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        List<TeamMemberBalance> onLeave = new ArrayList<>();

        for (Employee member : teamMembers) {
            // Check if employee has approved leave for today
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

                // Calculate total days on leave
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

    /**
     * Get team leave calendar for next 7 days
     */
    public Map<String, List<TeamMemberBalance>> getTeamLeaveCalendar(Long managerId) {

        log.info("📅 [DASHBOARD-MANAGER] Getting team leave calendar for manager: {}", managerId);

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(7);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        Map<String, List<TeamMemberBalance>> calendar = new LinkedHashMap<>();

        // Initialize dates
        for (LocalDate date = today; !date.isAfter(endDate); date = date.plusDays(1)) {
            calendar.put(date.toString(), new ArrayList<>());
        }

        // Fill calendar
        for (Employee member : teamMembers) {
            List<LeaveApplication> approvedLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !la.getEndDate().isBefore(today) &&
                            !la.getStartDate().isAfter(endDate))
                    .collect(Collectors.toList());

            for (LeaveApplication leave : approvedLeaves) {
                LocalDate leaveDate = leave.getStartDate();
                while (!leaveDate.isAfter(leave.getEndDate()) && !leaveDate.isAfter(endDate)) {
                    if (!leaveDate.isBefore(today)) {
                        TeamMemberBalance balance = new TeamMemberBalance();
                        balance.setEmployeeId(member.getId());
                        balance.setEmployeeName(member.getName());

                        String dateKey = leaveDate.toString();
                        calendar.get(dateKey).add(balance);
                    }
                    leaveDate = leaveDate.plusDays(1);
                }
            }
        }

        log.info("✅ [DASHBOARD-MANAGER] Team leave calendar prepared for 7 days");

        return calendar;
    }

    /**
     * Get pending team leave requests count
     */
    public Integer getPendingTeamRequestsCount(Long managerId) {

        log.info("🔔 [DASHBOARD-MANAGER] Getting pending team requests count for manager: {}", managerId);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        int totalPending = 0;

        for (Employee member : teamMembers) {
            List<LeaveApplication> pending = applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.PENDING);
            totalPending += pending.size();
        }

        log.info("✅ [DASHBOARD-MANAGER] {} pending team requests", totalPending);

        return totalPending;
    }

    /**
     * Get pending team leave requests (detailed)
     */
    public List<LeaveApplication> getPendingTeamRequests(Long managerId) {

        log.info("📋 [DASHBOARD-MANAGER] Getting pending team requests for manager: {}", managerId);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        List<LeaveApplication> allPending = new ArrayList<>();

        for (Employee member : teamMembers) {
            List<LeaveApplication> pending = applicationRepository
                    .findByEmployeeIdAndStatus(member.getId(), LeaveStatus.PENDING);
            allPending.addAll(pending);
        }

        // Sort by created date (oldest first)
        allPending.sort(Comparator.comparing(LeaveApplication::getCreatedAt));

        log.info("✅ [DASHBOARD-MANAGER] Retrieved {} pending team requests", allPending.size());

        return allPending;
    }

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD - Company-wide View
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get company-wide statistics
     */
    public Map<String, Object> getCompanyWideStats(Integer year) {

        log.info("📊 [DASHBOARD-HR] Getting company-wide stats for year: {}", year);

        Map<String, Object> stats = new HashMap<>();

        // Total active employees
        List<Employee> activeEmployees = employeeRepository.findActiveEmployees();
        stats.put("totalEmployees", activeEmployees.size());

        // Total approved leaves this year
        int totalApproved = 0;
        double totalDaysUsed = 0.0;

        for (Employee emp : activeEmployees) {
            Double used = applicationRepository.getTotalUsedDays(
                    emp.getId(), LeaveStatus.APPROVED, year);
            if (used != null) {
                totalDaysUsed += used;
                totalApproved++;
            }
        }

        stats.put("totalApprovedLeaves", totalApproved);
        stats.put("totalDaysUsed", totalDaysUsed);

        // Total LOP percentage
        double totalLOP = 0.0;
        int lopCount = 0;

        for (Employee emp : activeEmployees) {
            Double lop = lopRepository.getTotalLossPercentageByEmployeeIdAndYear(
                    emp.getId(), year);
            if (lop != null && lop > 0) {
                totalLOP += lop;
                lopCount++;
            }
        }

        stats.put("totalLopPercentage", lopCount > 0 ? totalLOP / lopCount : 0.0);
        stats.put("employeesWithLOP", lopCount);

        // Carry forward utilization
        double cfUsedTotal = 0.0;
        double cfTotalTotal = 0.0;

        for (Employee emp : activeEmployees) {
            CarryForwardBalance cf = carryForwardRepository
                    .findByEmployeeIdAndYear(emp.getId(), year)
                    .orElse(null);

            if (cf != null) {
                cfUsedTotal += cf.getTotalUsed();
                cfTotalTotal += cf.getTotalCarriedForward();
            }
        }

        double cfUtilization = (cfTotalTotal > 0) ? (cfUsedTotal / cfTotalTotal * 100) : 0.0;
        stats.put("carryForwardUtilization", cfUtilization);

        log.info("✅ [DASHBOARD-HR] Company-wide stats calculated");

        return stats;
    }

    /**
     * Get employees currently on leave (today)
     */
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

    /**
     * Get managers with upcoming leave (next 7 days)
     */
    public List<Employee> getManagersWithUpcomingLeave() {

        log.info("👔 [DASHBOARD-HR] Getting managers with upcoming leave");

        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Employee> managers = employeeRepository.findByRole(Role.MANAGER);
        List<Employee> managersWithLeave = new ArrayList<>();

        for (Employee manager : managers) {
            List<LeaveApplication> upcomingLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(manager.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !la.getStartDate().isBefore(today) &&
                            !la.getStartDate().isAfter(nextWeek))
                    .collect(Collectors.toList());

            if (!upcomingLeaves.isEmpty()) {
                managersWithLeave.add(manager);
            }
        }

        log.info("✅ [DASHBOARD-HR] {} managers with upcoming leave", managersWithLeave.size());

        return managersWithLeave;
    }

    /**
     * Get admins with upcoming leave (next 7 days)
     */
    public List<Employee> getAdminsWithUpcomingLeave() {

        log.info("⚙️ [DASHBOARD-HR] Getting admins with upcoming leave");

        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Employee> admins = employeeRepository.findByRole(Role.ADMIN);
        List<Employee> adminsWithLeave = new ArrayList<>();

        for (Employee admin : admins) {
            List<LeaveApplication> upcomingLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(admin.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !la.getStartDate().isBefore(today) &&
                            !la.getStartDate().isAfter(nextWeek))
                    .collect(Collectors.toList());

            if (!upcomingLeaves.isEmpty()) {
                adminsWithLeave.add(admin);
            }
        }

        log.info("✅ [DASHBOARD-HR] {} admins with upcoming leave", adminsWithLeave.size());

        return adminsWithLeave;
    }

    /**
     * Get employees with low leave balance
     */
    public List<TeamMemberBalance> getEmployeesWithLowBalance(Integer year, Double threshold) {

        log.info("⚠️ [DASHBOARD-HR] Getting employees with balance < {}", threshold);

        List<Employee> allEmployees = employeeRepository.findActiveEmployees();
        List<TeamMemberBalance> lowBalance = new ArrayList<>();

        for (Employee emp : allEmployees) {
            Double allocated = allocationRepository.getTotalAllocatedDays(emp.getId(), year);
            Double used = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, year);

            if (allocated == null) allocated = 0.0;
            if (used == null) used = 0.0;

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

    /**
     * Get employees with high LOP
     */
    public List<TeamMemberBalance> getEmployeesWithHighLOP(Integer year, Double threshold) {

        log.info("⚠️ [DASHBOARD-HR] Getting employees with LOP > {}", threshold);

        List<Employee> allEmployees = employeeRepository.findActiveEmployees();
        List<TeamMemberBalance> highLOP = new ArrayList<>();

        for (Employee emp : allEmployees) {
            Double lop = lopRepository.getTotalLossPercentageByEmployeeIdAndYear(emp.getId(), year);

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

    /**
     * Get employees eligible for carry forward
     */
    public List<TeamMemberBalance> getCarryForwardEligible(Integer year) {

        log.info("📋 [DASHBOARD-HR] Getting carry forward eligible employees for year: {}", year);

        List<Employee> allEmployees = employeeRepository.findActiveEmployees();
        List<TeamMemberBalance> eligible = new ArrayList<>();

        for (Employee emp : allEmployees) {
            Double allocated = allocationRepository.getTotalAllocatedDays(emp.getId(), year);
            Double used = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, year);

            if (allocated == null) allocated = 0.0;
            if (used == null) used = 0.0;

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

    /**
     * Alias for getDashboard() - used by controller
     */
    public EmployeeDashboardResponse getEmployeeDashboard(Long employeeId) {
        return getDashboard(employeeId);
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN DASHBOARD - System Health & Policy Compliance
    // ═══════════════════════════════════════════════════════════════

    /**
     * Admin uses same dashboard as employee for their own stats
     * @return AdminDashboardResponse for admin with comprehensive metrics
     */


//    /**
//     * Get total pending approvals across all managers
//     */
//    public Integer getTotalPendingApprovals() {
//
//        log.info("🔔 [DASHBOARD-ADMIN] Getting total pending approvals");
//
//        List<LeaveApplication> pending = applicationRepository
//                .findByStatus(LeaveStatus.PENDING);
//
//        log.info("✅ [DASHBOARD-ADMIN] {} total pending approvals", pending.size());
//
//        return pending.size();
//    }

    /**
     * Get employees exceeding monthly limit
     */
    public List<TeamMemberBalance> getEmployeesExceedingMonthlyLimit(Integer year, Integer month) {

        log.info("⚠️ [DASHBOARD-ADMIN] Getting employees exceeding monthly limit for {}/{}", year, month);

        List<Employee> allEmployees = employeeRepository.findActiveEmployees();
        List<TeamMemberBalance> exceeding = new ArrayList<>();

        for (Employee emp : allEmployees) {
            Integer approvedCount = applicationRepository
                    .countApprovedInMonth(emp.getId(), year, month);

            if (approvedCount != null && approvedCount > PolicyConstants.MONTHLY_LIMIT) {
                TeamMemberBalance balance = new TeamMemberBalance();
                balance.setEmployeeId(emp.getId());
                balance.setEmployeeName(emp.getName());
                balance.setTotalUsed((double) approvedCount);
                exceeding.add(balance);
            }
        }

        log.info("✅ [DASHBOARD-ADMIN] {} employees exceeding monthly limit", exceeding.size());

        return exceeding;
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY: Get Leave Counts by Status
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get leave application counts by status for employee
     */
    public Map<LeaveStatus, Long> getLeaveCountsByStatus(Long employeeId, Integer year) {

        log.info("📊 [DASHBOARD] Getting leave counts by status: employee={}, year={}",
                employeeId, year);

        List<LeaveApplication> allLeaves = applicationRepository
                .findByEmployeeIdAndYear(employeeId, year);

        Map<LeaveStatus, Long> counts = allLeaves.stream()
                .collect(Collectors.groupingBy(
                        LeaveApplication::getStatus,
                        Collectors.counting()
                ));

        log.info("✅ [DASHBOARD] Leave counts: {}", counts);

        return counts;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPREHENSIVE MANAGER DASHBOARD (NEW)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get complete manager dashboard with all stats and team info
     */
    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(Long managerId) {

        log.info("👔 [DASHBOARD-MANAGER] Building optimized dashboard for manager: {}", managerId);

        // 1. Get Manager's personal stats using the existing method
        EmployeeDashboardResponse ownStats = getDashboard(managerId);

        ManagerDashboardResponse response = new ManagerDashboardResponse();
        response.setPersonalStats(ownStats); // Use composition here!

        // 2. Fetch Team Members
        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        response.setTeamSize(teamMembers.size());

        // 3. Team Pending Requests
        List<LeaveApplication> pendingRequests = applicationRepository.findPendingTeamRequests(managerId);
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

        // 4. Team Members on Leave Today
        LocalDate today = LocalDate.now();
        List<ManagerDashboardResponse.TeamMemberOnLeaveDTO> onLeaveDTOs = new ArrayList<>();

        for (Employee member : teamMembers) {
            applicationRepository.findByEmployeeIdAndStatus(member.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate()))
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
    // COMPREHENSIVE HR DASHBOARD (NEW)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get complete HR dashboard with company-wide metrics
     */
    @Transactional(readOnly = true)
    public HRDashboardResponse getHRDashboard() {

        log.info("🏢 [DASHBOARD-HR] Building comprehensive HR dashboard");

        int currentYear = LocalDate.now().getYear();
        com.example.employeeLeaveApplication.dto.HRDashboardResponse response =
                new com.example.employeeLeaveApplication.dto.HRDashboardResponse();

        response.setCurrentYear(currentYear);

        // ═══════════════════════════════════════════════════════════
        // COMPANY-WIDE METRICS
        // ═══════════════════════════════════════════════════════════

        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();
        response.setTotalEmployees(activeEmployees.size());
        response.setActiveEmployees(activeEmployees.size());

        // Employees on leave today
        LocalDate today = LocalDate.now();
        List<Long> onLeaveTodayIds = applicationRepository.findEmployeesCurrentlyOnLeave(today);
        response.setEmployeesOnLeaveToday(onLeaveTodayIds.size());

        // Total pending and approved
        List<LeaveApplication> pending = applicationRepository.findByStatus(LeaveStatus.PENDING);
        response.setTotalPendingLeaves(pending.size());

        // Total approved (all employees, this year)
        int approvedCount = 0;
        for (Employee emp : activeEmployees) {
            List<LeaveApplication> approved = applicationRepository
                    .findByEmployeeIdAndStatusAndYear(emp.getId(), LeaveStatus.APPROVED, currentYear);
            approvedCount += approved.size();
        }
        response.setTotalApprovedLeaves(approvedCount);

        // ═══════════════════════════════════════════════════════════
        // ONBOARDING STATUS
        // ═══════════════════════════════════════════════════════════

        List<Employee> onboardingPending = employeeRepository.findOnboardingPending();
        response.setNewEmployeesCount(onboardingPending.size());
        response.setPendingBiometricCount(employeeRepository.countPendingBiometric());
        response.setPendingVPNCount(employeeRepository.countPendingVPN());

        // Prepare onboarding pending DTOs
        List<HRDashboardResponse.OnboardingPendingDTO> onboardingDTOs
                = new ArrayList<>();
        for (Employee emp : onboardingPending) {
            int daysInOnboarding = (int) ChronoUnit.DAYS.between(emp.getJoiningDate(), LocalDate.now());
            var dto = new HRDashboardResponse.OnboardingPendingDTO(
                    emp.getId(),
                    emp.getName(),
                    emp.getEmail(),
                    emp.getJoiningDate(),
                    emp.getBiometricStatus(),
                    emp.getVpnStatus(),
                    daysInOnboarding
            );
            onboardingDTOs.add(dto);
        }
        response.setOnboardingPendingList(onboardingDTOs);

        // ═══════════════════════════════════════════════════════════
        // EMPLOYEES ON LEAVE
        // ═══════════════════════════════════════════════════════════

        List<LeaveApplication> leavesInRange = applicationRepository
                .findApprovedLeavesInDateRange(today, today);

        List<com.example.employeeLeaveApplication.dto.HRDashboardResponse.EmployeeOnLeaveDTO> onLeaveDTOs
                = new ArrayList<>();

        for (LeaveApplication leave : leavesInRange) {
            Employee emp = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
            if (emp != null) {
                Employee manager = emp.getManagerId() != null ?
                        employeeRepository.findById(emp.getManagerId()).orElse(null) : null;

                Employee approver = leave.getApprovedBy() != null ?
                        employeeRepository.findById(leave.getApprovedBy()).orElse(null) : null;

                var dto = new com.example.employeeLeaveApplication.dto.HRDashboardResponse.EmployeeOnLeaveDTO(
                        emp.getId(),
                        emp.getName(),
                        manager != null ? manager.getName() : "N/A",
                        leave.getLeaveType().name(),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        leave.getDays().doubleValue(),
                        leave.getApprovedAt() != null ? leave.getApprovedAt().toLocalDate() : null,
                        approver != null ? approver.getName() : "N/A"
                );
                onLeaveDTOs.add(dto);
            }
        }
        response.setEmployeesOnLeave(onLeaveDTOs);

        // ═══════════════════════════════════════════════════════════
        // MANAGER INSIGHTS
        // ═══════════════════════════════════════════════════════════

        List<Long> managerIds = applicationRepository.findManagersWhoApprovedLeaves(currentYear);
        response.setTotalManagersWithApprovals(managerIds.size());

        List<com.example.employeeLeaveApplication.dto.HRDashboardResponse.ManagerApprovalStatsDTO> managerStats
                = new ArrayList<>();

        for (Long mgrId : managerIds) {
            Employee mgr = employeeRepository.findById(mgrId).orElse(null);
            if (mgr != null) {
                List<Employee> teamMembers = employeeRepository.findTeamMembersByManager(mgrId);
                List<LeaveApplication> managerApprovals = applicationRepository
                        .findLeavesApprovedByManager(mgrId, currentYear);

                // Count pending from his team
                int teamPending = 0;
                for (Employee tm : teamMembers) {
                    List<LeaveApplication> tmPending = applicationRepository
                            .findByEmployeeIdAndStatus(tm.getId(), LeaveStatus.PENDING);
                    teamPending += tmPending.size();
                }

                int approvalRate = !teamMembers.isEmpty() ?
                        (int) ((managerApprovals.size() / (double) teamMembers.size()) * 100) : 0;

                LocalDateTime lastApproval = managerApprovals.isEmpty() ? null :
                        managerApprovals.get(0).getApprovedAt();

                var stats = new com.example.employeeLeaveApplication.dto.HRDashboardResponse.ManagerApprovalStatsDTO(
                        mgrId,
                        mgr.getName(),
                        teamMembers.size(),
                        managerApprovals.size(),
                        teamPending,
                        approvalRate,
                        lastApproval != null ? lastApproval.toLocalDate() : null
                );
                managerStats.add(stats);
            }
        }
        response.setManagerApprovalStats(managerStats);

        // ═══════════════════════════════════════════════════════════
        // TEAM STRUCTURE
        // ═══════════════════════════════════════════════════════════

        List<Employee> allManagers = employeeRepository.findAllManagers();
        List<com.example.employeeLeaveApplication.dto.HRDashboardResponse.TeamStructureDTO> teamStructure
                = new ArrayList<>();

        for (Employee manager : allManagers) {
            List<Employee> teamMembers = employeeRepository.findTeamMembersByManager(manager.getId());

            List<com.example.employeeLeaveApplication.dto.HRDashboardResponse.TeamMemberDTO> teamMemberDTOs
                    = new ArrayList<>();

            for (Employee member : teamMembers) {
                Double yearlyBal = allocationRepository.getTotalAllocatedDays(member.getId(), currentYear);
                Double yearlyUsed = applicationRepository.getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, currentYear);

                CarryForwardBalance cf = carryForwardRepository
                        .findByEmployeeIdAndYear(member.getId(), currentYear).orElse(null);
                Double cfBalVal = cf != null ? cf.getRemaining() : null;
                Double cfBal = cfBalVal != null ? cfBalVal : 0.0;

                CompOffBalance compOff = compOffRepository
                        .findByEmployeeIdAndYear(member.getId(), currentYear).orElse(null);
                Double compOffBalVal = compOff != null ? compOff.getBalance() : null;
                Double compOffBal = compOffBalVal != null ? compOffBalVal : 0.0;

                var memberDTO = new com.example.employeeLeaveApplication.dto.HRDashboardResponse.TeamMemberDTO(
                        member.getId(),
                        member.getName(),
                        member.getEmail(),
                        (yearlyBal != null ? yearlyBal : 0.0) - (yearlyUsed != null ? yearlyUsed : 0.0),
                        cfBal,
                        compOffBal
                );
                teamMemberDTOs.add(memberDTO);
            }

            var teamStruct = new com.example.employeeLeaveApplication.dto.HRDashboardResponse.TeamStructureDTO(
                    manager.getId(),
                    manager.getName(),
                    teamMembers.size(),
                    teamMemberDTOs
            );
            teamStructure.add(teamStruct);
        }
        response.setTeamStructure(teamStructure);
        response.setLastUpdated(LocalDateTime.now());

        log.info("✅ [DASHBOARD-HR] HR dashboard complete");
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPREHENSIVE ADMIN DASHBOARD (NEW)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get complete admin dashboard with compliance metrics
     */

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

        // ═══════════════════════════════════════════════════════════
        // ADMIN'S OWN STATS
        // ═══════════════════════════════════════════════════════════

        EmployeeDashboardResponse ownStats = getDashboard(adminId);
        response.setYearlyBalance(ownStats.getYearlyBalance());
        response.setCarryForwardBalance(ownStats.getCarryForwardRemaining());
        response.setCompOffBalance(ownStats.getCompoffBalance());
        response.setApprovedLeaveCount(ownStats.getApprovedCount());
        response.setPendingLeaveCount(ownStats.getPendingCount());

        // ═══════════════════════════════════════════════════════════
        // COMPLIANCE METRICS
        // ═══════════════════════════════════════════════════════════

        List<Employee> allEmployees = employeeRepository.findByActiveTrue();
        response.setTotalEmployees(allEmployees.size());

        List<Employee> managers = employeeRepository.findByRole(Role.MANAGER);
        response.setTotalManagers(managers.size());

        // Onboarding pending
        List<Employee> onboardingPending = employeeRepository.findOnboardingPending();
        response.setNewEmployeesPendingOnboarding(onboardingPending.size());

        // Pending leaves
        List<LeaveApplication> pending = applicationRepository.findByStatus(LeaveStatus.PENDING);
        response.setTotalPendingLeaves(pending.size());

        // Rejected leaves
        List<LeaveApplication> rejected = applicationRepository.findByStatus(LeaveStatus.REJECTED);
        response.setTotalRejectedLeaves(rejected.size());

        // ═══════════════════════════════════════════════════════════
        // LEAVE STATISTICS
        // ═══════════════════════════════════════════════════════════

        double totalUsedYTD = 0.0;
        double totalCFBalance = 0.0;
        double totalCompOffBalance = 0.0;
        double totalLOP = 0.0;
        int lopCount = 0;

        for (Employee emp : allEmployees) {
            Double used = applicationRepository.getTotalUsedDays(emp.getId(), LeaveStatus.APPROVED, currentYear);
            if (used != null) totalUsedYTD += used;

            CarryForwardBalance cf = carryForwardRepository
                    .findByEmployeeIdAndYear(emp.getId(), currentYear).orElse(null);
            if (cf != null) totalCFBalance += cf.getRemaining();

            CompOffBalance compOff = compOffRepository
                    .findByEmployeeIdAndYear(emp.getId(), currentYear).orElse(null);
            if (compOff != null) totalCompOffBalance += compOff.getBalance();

            Double lop = lopRepository.getTotalLossPercentageByEmployeeIdAndYear(emp.getId(), currentYear);
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

    public List<ManagerDashboardResponse.TeamPendingLeaveDTO> getPendingTeamRequestDTOs(
            Long managerId) {
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