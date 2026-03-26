package com.example.employeeLeaveApplication.feature.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.example.employeeLeaveApplication.feature.dashboard.dto.*;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.SickLeaveMonthlyBalance;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.AnnualLeaveMonthlyBalanceRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveAllocationRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.SickLeaveMonthlyBalanceRepository;
import com.example.employeeLeaveApplication.feature.leave.carryforward.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.feature.leave.compoff.entity.CompOffBalance;
import com.example.employeeLeaveApplication.feature.leave.compoff.repository.CompOffBalanceRepository;
import com.example.employeeLeaveApplication.feature.leave.lop.repository.LopRecordRepository;
import com.example.employeeLeaveApplication.feature.od.entity.ODRequest;
import com.example.employeeLeaveApplication.feature.od.repository.ODRequestRepository;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import com.example.employeeLeaveApplication.shared.enums.ODStatus;
import com.example.employeeLeaveApplication.shared.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.employeeLeaveApplication.shared.constants.PolicyConstants;
import com.example.employeeLeaveApplication.shared.enums.LeaveStatus;
import com.example.employeeLeaveApplication.shared.enums.Role;
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
    private final LopRecordRepository lopRepository;
    private final ODRequestRepository odRepository;
    private final AnnualLeaveMonthlyBalanceRepository annualLeaveMonthlyBalanceRepository;
    private final SickLeaveMonthlyBalanceRepository sickLeaveMonthlyBalanceRepository;
    private final EmployeePersonalDetailsRepository employeePersonalDetailsRepository;

    public List<TeamMember> getTeamMembers(Long id) {
        return employeeRepository.findActiveTeamMembers(id).stream()
                .map(member -> {
                    TeamMember dto = new TeamMember();
                    dto.setEmployeeId(member.getId());
                    dto.setEmployeeName(member.getName());
                    employeePersonalDetailsRepository.findByEmployeeId(member.getId())
                            .ifPresent(d -> { dto.setDesignation(d.getDesignation()); dto.setSkills(d.getSkillSet()); });
                    return dto;
                }).collect(Collectors.toList());
    }

    public EmployeeDashboardResponse getDashboard(Long employeeId) {
        log.info("DASHBOARD employee={}", employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        int currentYear  = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        EmployeeDashboardResponse response = new EmployeeDashboardResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setCurrentYear(currentYear);
        response.setLastUpdated(LocalDateTime.now());

        // Yearly
        List<LeaveAllocation> allocations = allocationRepository.findByEmployeeIdAndYear(employeeId, currentYear);
        double yearlyAllocated = allocations.stream().mapToDouble(LeaveAllocation::getAllocatedDays).sum();
        Double yearlyUsed = applicationRepository.getTotalUsedDays(employeeId, LeaveStatus.APPROVED, currentYear);
        if (yearlyUsed == null) yearlyUsed = 0.0;

        response.setYearlyAllocated(yearlyAllocated);
        response.setYearlyUsed(yearlyUsed);
        response.setYearlyBalance(yearlyAllocated - yearlyUsed);

        // Monthly ANNUAL_LEAVE cumulative
        AnnualLeaveMonthlyBalance annualMonthly = annualLeaveMonthlyBalanceRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, currentYear, currentMonth).orElse(null);
        double annualAvailable = annualMonthly != null ? annualMonthly.getAvailableDays() : PolicyConstants.ANNUAL_LEAVE_PER_MONTH;
        double annualMonthUsed = annualMonthly != null ? annualMonthly.getUsedDays()      : 0.0;
        double annualRemaining = annualMonthly != null ? annualMonthly.getRemainingDays() : annualAvailable;

        response.setMonthlyAnnualAllocated(annualAvailable);
        response.setMonthlyAnnualUsed(annualMonthUsed);
        response.setMonthlyAnnualBalance(annualRemaining);

        // Monthly SICK cumulative
        SickLeaveMonthlyBalance sickMonthly = sickLeaveMonthlyBalanceRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, currentYear, currentMonth).orElse(null);
        double sickAvailable = sickMonthly != null ? sickMonthly.getRemainingDays() : PolicyConstants.SICK_LEAVE_PER_MONTH;
        double sickMonthUsed = sickMonthly != null ? sickMonthly.getUsedDays()      : 0.0;
        double sickRemaining = sickMonthly != null ? sickMonthly.getRemainingDays() : annualAvailable;

        response.setMonthlySickAllocated(sickAvailable);
        response.setMonthlySickUsed(sickMonthUsed);
        response.setMonthlySickBalance(sickRemaining);

        response.setMonthlyTotalBalance(annualRemaining + sickRemaining);

        // Carry forward
        CarryForwardBalance cfBalance = carryForwardRepository.findByEmployeeIdAndYear(employeeId, currentYear).orElse(null);
        response.setCarryForwardTotal(cfBalance != null ? cfBalance.getTotalCarriedForward() : 0.0);
        response.setCarryForwardUsed(cfBalance != null ? cfBalance.getTotalUsed() : 0.0);
        response.setCarryForwardRemaining(cfBalance != null ? cfBalance.getRemaining() : 0.0);

        // Breakdown
        List<LeaveApplication> approvedLeaves = applicationRepository.findByEmployeeIdAndStatusAndYear(employeeId, LeaveStatus.APPROVED, currentYear);
        List<LeaveApplication> pendingLeaves  = applicationRepository.findByEmployeeIdAndStatusAndYear(employeeId, LeaveStatus.PENDING, currentYear);
        Map<LeaveType, List<LeaveApplication>> byType      = approvedLeaves.stream().collect(Collectors.groupingBy(LeaveApplication::getLeaveType));
        Map<LeaveType, List<LeaveApplication>> pendingByType = pendingLeaves.stream().collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();
        for (LeaveAllocation allocation : allocations) {
            LeaveType type   = allocation.getLeaveCategory();
            double allocated = allocation.getAllocatedDays();
            List<LeaveApplication> typeLeaves = byType.getOrDefault(type, List.of());
            double used = typeLeaves.stream().mapToDouble(l -> l.getDays().doubleValue()).sum();
            double remaining = switch (type) {
                case ANNUAL_LEAVE -> annualRemaining;
                case SICK         -> sickRemaining;
                default           -> allocated - used;
            };
            int halfDays     = (int) typeLeaves.stream().filter(l -> l.getDays().compareTo(new BigDecimal("0.5")) == 0).count();
            long pendingCount = pendingByType.getOrDefault(type, List.of()).size();
            breakdown.add(new LeaveTypeBreakdown(type, (Double) allocated, (Double) used, (Double) remaining, halfDays, pendingCount));
        }

        CompOffBalance compOff = compOffRepository.findByEmployeeIdAndYear(employeeId, currentYear).orElse(null);
        double coEarned = compOff != null ? compOff.getEarned()  : 0.0;
        double coUsed   = compOff != null ? compOff.getUsed()    : 0.0;
        double coBal    = compOff != null ? compOff.getBalance() : 0.0;
        breakdown.add(new LeaveTypeBreakdown(LeaveType.COMP_OFF, (Double) coEarned, (Double) coUsed, (Double) coBal, 0,
                (long) pendingByType.getOrDefault(LeaveType.COMP_OFF, List.of()).size()));
        response.setBreakdown(breakdown);
        response.setCompoffBalance(coBal);

        // LOP
        Double totalLOP = lopRepository.sumLopDaysForYear(employeeId, currentYear);
        response.setLossOfPayPercentage(totalLOP != null ? totalLOP : 0.0);

        // Counts
        Integer approvedCount = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.APPROVED);
        Integer rejectedCount = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.REJECTED);
        Integer pendingCount  = applicationRepository.countByStatus(employeeId, currentYear, LeaveStatus.PENDING);
        response.setApprovedCount(approvedCount != null ? approvedCount : 0);
        response.setRejectedCount(rejectedCount != null ? rejectedCount : 0);
        response.setPendingCount(pendingCount   != null ? pendingCount  : 0);

        log.info("DASHBOARD done: annual={}, sick={}, total={}", annualRemaining, sickRemaining, annualRemaining + sickRemaining);
        return response;
    }

    public MonthlyStatsResponse getMonthlyStats(Long employeeId, Integer year, Integer month) {
        employeeRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));
        MonthlyStatsResponse response = new MonthlyStatsResponse();
        response.setEmployeeId(employeeId); response.setYear(year); response.setMonth(month);
        AnnualLeaveMonthlyBalance balance = annualLeaveMonthlyBalanceRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month).orElse(null);
        double used      = balance != null ? balance.getUsedDays()      : 0.0;
        double available = balance != null ? balance.getAvailableDays() : PolicyConstants.ANNUAL_LEAVE_PER_MONTH;
        response.setTotalApprovedCount((int) used);
        response.setExceededLimit(used > available);
        return response;
    }

    public List<TeamMemberBalance> getTeamBalances(Long managerId, Integer year) {
        return employeeRepository.findActiveTeamMembers(managerId).stream().map(member -> {
            TeamMemberBalance b = new TeamMemberBalance();
            b.setEmployeeId(member.getId()); b.setEmployeeName(member.getName());
            Double alloc = allocationRepository.getTotalAllocatedDays(member.getId(), year);
            Double used  = applicationRepository.getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, year);
            b.setTotalAllocated(alloc != null ? alloc : 0.0);
            b.setTotalUsed(used != null ? used : 0.0);
            b.setTotalRemaining(b.getTotalAllocated() - b.getTotalUsed());
            CompOffBalance co = compOffRepository.findByEmployeeIdAndYear(member.getId(), year).orElse(null);
            b.setCompOffBalance(co != null ? co.getBalance() : 0.0);
            Double lop = lopRepository.sumLopDaysForYear(member.getId(), year);
            b.setLopPercentage(lop != null ? lop : 0.0);
            return b;
        }).collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getTeamMembersOnLeaveToday(Long id) {
        LocalDate today = LocalDate.now();
        return employeeRepository.findActiveTeamMembers(id).stream()
                .filter(m -> applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.APPROVED)
                        .stream().anyMatch(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate())))
                .map(m -> { TeamMemberBalance b = new TeamMemberBalance(); b.setEmployeeId(m.getId()); b.setEmployeeName(m.getName()); return b; })
                .collect(Collectors.toList());
    }

    public Map<String, List<TeamMemberBalance>> getTeamLeaveCalendar(Long id) {
        Map<String, List<TeamMemberBalance>> cal = new TreeMap<>();
        for (Employee m : employeeRepository.findActiveTeamMembers(id)) {
            processLeavesIntoCalendar(cal, m, applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.APPROVED));
            processODsIntoCalendar(cal, m, odRepository.findByEmployeeIdAndStatus(m.getId(), ODStatus.APPROVED));
        }
        return cal;
    }

    public Map<String, List<TeamMemberBalance>> getMyLeaveCalendar(Long employeeId) {
        Employee m = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        Map<String, List<TeamMemberBalance>> cal = new TreeMap<>();
        processLeavesIntoCalendar(cal, m, applicationRepository.findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED));
        processODsIntoCalendar(cal, m, odRepository.findByEmployeeIdAndStatus(employeeId, ODStatus.APPROVED));
        return cal;
    }

    private void processLeavesIntoCalendar(Map<String, List<TeamMemberBalance>> cal, Employee m, List<LeaveApplication> leaves) {
        for (LeaveApplication l : leaves) { LocalDate d = l.getStartDate(); while (!d.isAfter(l.getEndDate())) { addToCalendar(cal, d, m); d = d.plusDays(1); } }
    }
    private void processODsIntoCalendar(Map<String, List<TeamMemberBalance>> cal, Employee m, List<ODRequest> ods) {
        for (ODRequest od : ods) { LocalDate d = od.getStartDate(); while (!d.isAfter(od.getEndDate())) { addToCalendar(cal, d, m); d = d.plusDays(1); } }
    }
    private void addToCalendar(Map<String, List<TeamMemberBalance>> cal, LocalDate date, Employee m) {
        cal.computeIfAbsent(date.toString(), k -> new ArrayList<>());
        TeamMemberBalance e = new TeamMemberBalance(); e.setEmployeeId(m.getId()); e.setEmployeeName(m.getName());
        cal.get(date.toString()).add(e);
    }

    public Integer getPendingTeamRequestsCount(Long managerId) {
        return employeeRepository.findActiveTeamMembers(managerId).stream()
                .mapToInt(m -> applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.PENDING).size()).sum();
    }

    public List<LeaveApplication> getPendingTeamRequests(Long managerId) {
        List<LeaveApplication> all = new ArrayList<>();
        for (Employee m : employeeRepository.findActiveTeamMembers(managerId))
            all.addAll(applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.PENDING));
        all.sort(Comparator.comparing(LeaveApplication::getCreatedAt));
        return all;
    }

    @Transactional(readOnly = true)
    public TeamLeaderDashboardResponse getTeamLeaderDashboard(Long tlId) {
        Employee tl = employeeRepository.findById(tlId).orElseThrow(() -> new RuntimeException("TL not found: " + tlId));
        if (tl.getRole() != Role.TEAM_LEADER) throw new RuntimeException("Not a team leader");
        TeamLeaderDashboardResponse response = new TeamLeaderDashboardResponse();
        response.setPersonalStats(getDashboard(tlId));
        List<Employee> team = employeeRepository.findActiveTeamMembers(tlId);
        response.setTeamSize(team.size());
        List<TeamLeaderDashboardResponse.TeamPendingLeaveDTO> pending = new ArrayList<>();
        for (Employee m : team)
            for (LeaveApplication l : applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.PENDING))
                pending.add(new TeamLeaderDashboardResponse.TeamPendingLeaveDTO(l.getId(), l.getEmployeeId(), m.getName(),
                        l.getLeaveType(), l.getReason(), l.getStatus(), l.getStartDate(), l.getEndDate(), l.getDays().doubleValue(), l.getCreatedAt()));
        pending.sort(Comparator.comparing(TeamLeaderDashboardResponse.TeamPendingLeaveDTO::getAppliedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        response.setPendingTeamRequests(pending); response.setTeamPendingRequestCount(pending.size());
        LocalDate today = LocalDate.now();
        List<TeamLeaderDashboardResponse.TeamMemberOnLeaveDTO> onLeave = new ArrayList<>();
        for (Employee m : team)
            applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.APPROVED).stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate()))
                    .forEach(l -> onLeave.add(new TeamLeaderDashboardResponse.TeamMemberOnLeaveDTO(m.getId(), m.getName(),
                            l.getLeaveType().name(), l.getStartDate(), l.getEndDate(), (double) Math.max(0, ChronoUnit.DAYS.between(today, l.getEndDate())))));
        response.setTeamOnLeaveToday(onLeave); response.setTeamOnLeaveCount(onLeave.size());
        int yr = LocalDate.now().getYear();
        List<TeamLeaderDashboardResponse.TeamMemberBalanceSummaryDTO> summaries = new ArrayList<>();
        for (Employee m : team) {
            Double alloc = allocationRepository.getTotalAllocatedDays(m.getId(), yr);
            Double used  = applicationRepository.getTotalUsedDays(m.getId(), LeaveStatus.APPROVED, yr);
            if (alloc == null) alloc = 0.0; if (used == null) used = 0.0;
            CompOffBalance co = compOffRepository.findByEmployeeIdAndYear(m.getId(), yr).orElse(null);
            Double lop = lopRepository.sumLopDaysForYear(m.getId(), yr);
            summaries.add(new TeamLeaderDashboardResponse.TeamMemberBalanceSummaryDTO(m.getId(), m.getName(), alloc, used, alloc - used,
                    co != null ? co.getBalance() : 0.0, lop != null ? lop : 0.0));
        }
        response.setTeamBalances(summaries); response.setLastUpdated(LocalDateTime.now());
        return response;
    }

    public Map<String, Object> getCompanyWideStats(Integer year) {
        Map<String, Object> stats = new HashMap<>();
        List<Employee> active = employeeRepository.findActiveEmployees();
        stats.put("totalEmployees", active.size());
        double totalDays = 0; int totalApproved = 0;
        for (Employee e : active) { Double u = applicationRepository.getTotalUsedDays(e.getId(), LeaveStatus.APPROVED, year); if (u != null) { totalDays += u; totalApproved++; } }
        stats.put("totalApprovedLeaves", totalApproved); stats.put("totalDaysUsed", totalDays);
        double totalLOP = 0; int lopCount = 0;
        for (Employee e : active) { Double l = lopRepository.sumLopDaysForYear(e.getId(), year); if (l != null && l > 0) { totalLOP += l; lopCount++; } }
        stats.put("totalLopPercentage", lopCount > 0 ? totalLOP / lopCount : 0.0); stats.put("employeesWithLOP", lopCount);
        double cfU = 0, cfT = 0;
        for (Employee e : active) { CarryForwardBalance cf = carryForwardRepository.findByEmployeeIdAndYear(e.getId(), year).orElse(null); if (cf != null) { cfU += cf.getTotalUsed(); cfT += cf.getTotalCarriedForward(); } }
        stats.put("carryForwardUtilization", cfT > 0 ? (cfU / cfT * 100) : 0.0);
        return stats;
    }

    public List<Employee> getEmployeesCurrentlyOnLeave() {
        LocalDate today = LocalDate.now();
        return employeeRepository.findActiveEmployees().stream()
                .filter(e -> applicationRepository.findByEmployeeIdAndStatus(e.getId(), LeaveStatus.APPROVED)
                        .stream().anyMatch(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate())))
                .collect(Collectors.toList());
    }

    public List<Employee> getManagersWithUpcomingLeave() {
        LocalDate today = LocalDate.now(), nw = today.plusDays(7);
        return employeeRepository.findByRole(Role.MANAGER).stream()
                .filter(m -> applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.APPROVED)
                        .stream().anyMatch(la -> !la.getStartDate().isBefore(today) && !la.getStartDate().isAfter(nw)))
                .collect(Collectors.toList());
    }

    public List<Employee> getAdminsWithUpcomingLeave() {
        LocalDate today = LocalDate.now(), nw = today.plusDays(7);
        return employeeRepository.findByRole(Role.ADMIN).stream()
                .filter(a -> applicationRepository.findByEmployeeIdAndStatus(a.getId(), LeaveStatus.APPROVED)
                        .stream().anyMatch(la -> !la.getStartDate().isBefore(today) && !la.getStartDate().isAfter(nw)))
                .collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getEmployeesWithLowBalance(Integer year, Double threshold) {
        return employeeRepository.findActiveEmployees().stream().map(e -> {
            Double a = allocationRepository.getTotalAllocatedDays(e.getId(), year);
            Double u = applicationRepository.getTotalUsedDays(e.getId(), LeaveStatus.APPROVED, year);
            if (a == null) a = 0.0; if (u == null) u = 0.0;
            if (a - u >= threshold) return null;
            TeamMemberBalance b = new TeamMemberBalance(); b.setEmployeeId(e.getId()); b.setEmployeeName(e.getName());
            b.setTotalAllocated(a); b.setTotalUsed(u); b.setTotalRemaining(a - u); return b;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getEmployeesWithHighLOP(Integer year, Double threshold) {
        return employeeRepository.findActiveEmployees().stream().map(e -> {
            Double lop = lopRepository.sumLopDaysForYear(e.getId(), year);
            if (lop == null || lop <= threshold) return null;
            TeamMemberBalance b = new TeamMemberBalance(); b.setEmployeeId(e.getId()); b.setEmployeeName(e.getName()); b.setLopPercentage(lop); return b;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getCarryForwardEligible(Integer year) {
        return employeeRepository.findActiveEmployees().stream().map(e -> {
            Double a = allocationRepository.getTotalAllocatedDays(e.getId(), year);
            Double u = applicationRepository.getTotalUsedDays(e.getId(), LeaveStatus.APPROVED, year);
            if (a == null) a = 0.0; if (u == null) u = 0.0;
            double bal = a - u; if (bal <= 0) return null;
            TeamMemberBalance b = new TeamMemberBalance(); b.setEmployeeId(e.getId()); b.setEmployeeName(e.getName());
            b.setTotalAllocated(a); b.setTotalUsed(u); b.setTotalRemaining(Math.min(bal, PolicyConstants.MAX_CARRY_FORWARD)); return b;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public EmployeeDashboardResponse getEmployeeDashboard(Long employeeId) { return getDashboard(employeeId); }

    public List<TeamMemberBalance> getEmployeesExceedingMonthlyLimit(Integer year, Integer month) {
        return employeeRepository.findActiveEmployees().stream().map(e -> {
            AnnualLeaveMonthlyBalance bal = annualLeaveMonthlyBalanceRepository.findByEmployeeIdAndYearAndMonth(e.getId(), year, month).orElse(null);
            if (bal == null || bal.getUsedDays() <= bal.getAvailableDays()) return null;
            TeamMemberBalance b = new TeamMemberBalance(); b.setEmployeeId(e.getId()); b.setEmployeeName(e.getName()); b.setTotalUsed(bal.getUsedDays()); return b;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(Long managerId) {
        ManagerDashboardResponse response = new ManagerDashboardResponse();
        response.setPersonalStats(getDashboard(managerId));
        List<Employee> team = employeeRepository.findActiveTeamMembers(managerId);
        response.setTeamSize(team.size());
        List<LeaveApplication> pr = applicationRepository.findPendingTeamRequests(managerId);
        response.setTeamPendingRequestCount(pr.size());
        response.setPendingTeamRequests(pr.stream().map(l -> {
            Employee e = employeeRepository.findById(l.getEmployeeId()).orElse(null);
            return new ManagerDashboardResponse.TeamPendingLeaveDTO(l.getId(), l.getEmployeeId(),
                    e != null ? e.getName() : "Unknown", l.getLeaveType(), l.getReason(), l.getStatus(),
                    l.getStartDate(), l.getEndDate(), l.getDays().doubleValue(), l.getCreatedAt());
        }).collect(Collectors.toList()));
        LocalDate today = LocalDate.now();
        List<ManagerDashboardResponse.TeamMemberOnLeaveDTO> ol = new ArrayList<>();
        for (Employee m : team)
            applicationRepository.findByEmployeeIdAndStatus(m.getId(), LeaveStatus.APPROVED).stream()
                    .filter(la -> !today.isBefore(la.getStartDate()) && !today.isAfter(la.getEndDate()))
                    .forEach(l -> ol.add(new ManagerDashboardResponse.TeamMemberOnLeaveDTO(m.getId(), m.getName(),
                            l.getLeaveType().name(), l.getStartDate(), l.getEndDate(), (double) Math.max(0, ChronoUnit.DAYS.between(today, l.getEndDate())))));
        response.setTeamOnLeaveToday(ol); response.setTeamOnLeaveCount(ol.size()); response.setLastUpdated(LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public HRDashboardResponse getHRDashboard() {
        int yr = LocalDate.now().getYear();
        HRDashboardResponse response = new HRDashboardResponse();
        response.setCurrentYear(yr);
        List<Employee> active = employeeRepository.findByActiveTrue();
        response.setTotalEmployees(active.size()); response.setActiveEmployees(active.size());
        LocalDate today = LocalDate.now();
        response.setEmployeesOnLeaveToday(applicationRepository.findEmployeesCurrentlyOnLeave(today).size());
        response.setTotalPendingLeaves(applicationRepository.findByStatus(LeaveStatus.PENDING).size());
        int approvedCnt = 0;
        for (Employee e : active) approvedCnt += applicationRepository.findByEmployeeIdAndStatusAndYear(e.getId(), LeaveStatus.APPROVED, yr).size();
        response.setTotalApprovedLeaves(approvedCnt);
        List<Employee> onboarding = employeeRepository.findOnboardingPending();
        response.setNewEmployeesCount(onboarding.size()); response.setPendingBiometricCount(employeeRepository.countPendingBiometric()); response.setPendingVPNCount(employeeRepository.countPendingVPN());
        List<HRDashboardResponse.OnboardingPendingDTO> oDTOs = new ArrayList<>();
        for (Employee e : onboarding) oDTOs.add(new HRDashboardResponse.OnboardingPendingDTO(e.getId(), e.getName(), e.getEmail(), e.getJoiningDate(), e.getBiometricStatus(), e.getVpnStatus(), (int) ChronoUnit.DAYS.between(e.getJoiningDate(), today)));
        response.setOnboardingPendingList(oDTOs);
        List<HRDashboardResponse.EmployeeOnLeaveDTO> olDTOs = new ArrayList<>();
        for (LeaveApplication l : applicationRepository.findApprovedLeavesInDateRange(today, today)) {
            Employee e = employeeRepository.findById(l.getEmployeeId()).orElse(null); if (e == null) continue;
            Employee mgr = e.getManagerId() != null ? employeeRepository.findById(e.getManagerId()).orElse(null) : null;
            Employee apr = l.getApprovedBy() != null ? employeeRepository.findById(l.getApprovedBy()).orElse(null) : null;
            olDTOs.add(new HRDashboardResponse.EmployeeOnLeaveDTO(e.getId(), e.getName(), mgr != null ? mgr.getName() : "N/A",
                    l.getLeaveType().name(), l.getStartDate(), l.getEndDate(), l.getDays().doubleValue(),
                    l.getApprovedAt() != null ? l.getApprovedAt().toLocalDate() : null, apr != null ? apr.getName() : "N/A"));
        }
        response.setEmployeesOnLeave(olDTOs);
        List<Long> mgrIds = applicationRepository.findManagersWhoApprovedLeaves(yr); response.setTotalManagersWithApprovals(mgrIds.size());
        List<HRDashboardResponse.ManagerApprovalStatsDTO> mStats = new ArrayList<>();
        for (Long mid : mgrIds) {
            Employee mgr = employeeRepository.findById(mid).orElse(null); if (mgr == null) continue;
            List<Employee> t = employeeRepository.findTeamMembersByManager(mid);
            List<LeaveApplication> ap = applicationRepository.findLeavesApprovedByManager(mid, yr);
            int tp = t.stream().mapToInt(tm -> applicationRepository.findByEmployeeIdAndStatus(tm.getId(), LeaveStatus.PENDING).size()).sum();
            LocalDateTime la = ap.isEmpty() ? null : ap.get(0).getApprovedAt();
            mStats.add(new HRDashboardResponse.ManagerApprovalStatsDTO(mid, mgr.getName(), t.size(), ap.size(), tp,
                    !t.isEmpty() ? (int) ((ap.size() / (double) t.size()) * 100) : 0, la != null ? la.toLocalDate() : null));
        }
        response.setManagerApprovalStats(mStats);
        List<HRDashboardResponse.TeamStructureDTO> ts = new ArrayList<>();
        for (Employee mgr : employeeRepository.findAllManagers()) {
            List<Employee> t = employeeRepository.findTeamMembersByManager(mgr.getId());
            List<HRDashboardResponse.TeamMemberDTO> mDTOs = new ArrayList<>();
            for (Employee m : t) {
                Double bal = allocationRepository.getTotalAllocatedDays(m.getId(), yr);
                Double used = applicationRepository.getTotalUsedDays(m.getId(), LeaveStatus.APPROVED, yr);
                CarryForwardBalance cf = carryForwardRepository.findByEmployeeIdAndYear(m.getId(), yr).orElse(null);
                CompOffBalance co = compOffRepository.findByEmployeeIdAndYear(m.getId(), yr).orElse(null);
                mDTOs.add(new HRDashboardResponse.TeamMemberDTO(m.getId(), m.getName(), m.getEmail(),
                        (bal != null ? bal : 0.0) - (used != null ? used : 0.0), cf != null ? cf.getRemaining() : 0.0, co != null ? co.getBalance() : 0.0));
            }
            ts.add(new HRDashboardResponse.TeamStructureDTO(mgr.getId(), mgr.getName(), t.size(), mDTOs));
        }
        response.setTeamStructure(ts); response.setLastUpdated(LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard(Long adminId) {
        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setPersonalStats(getDashboard(adminId));
        response.setCurrentYear(LocalDate.now().getYear()); response.setLastUpdated(LocalDateTime.now());
        List<Employee> all = employeeRepository.findByActiveTrue();
        response.setTotalEmployees(all.size()); response.setTotalManagers(employeeRepository.findByRole(Role.MANAGER).size());
        response.setNewEmployeesPendingOnboarding(employeeRepository.findOnboardingPending().size());
        response.setTotalPendingLeaves(applicationRepository.findByStatus(LeaveStatus.PENDING).size());
        response.setTotalRejectedLeaves(applicationRepository.findByStatus(LeaveStatus.REJECTED).size());
        calculateGlobalMetrics(response, all);
        return response;
    }

    private void calculateGlobalMetrics(AdminDashboardResponse response, List<Employee> all) {
        int yr = response.getCurrentYear(); double ytd = 0, lop = 0; int lopCnt = 0;
        for (Employee e : all) {
            Double u = applicationRepository.getTotalUsedDays(e.getId(), LeaveStatus.APPROVED, yr); if (u != null) ytd += u;
            carryForwardRepository.findByEmployeeIdAndYear(e.getId(), yr).ifPresent(cf -> response.setTotalCarryForwardBalance(response.getTotalCarryForwardBalance() + cf.getRemaining()));
            compOffRepository.findByEmployeeIdAndYear(e.getId(), yr).ifPresent(co -> response.setTotalCompOffBalance(response.getTotalCompOffBalance() + co.getBalance()));
            Double l = lopRepository.sumLopDaysForYear(e.getId(), yr); if (l != null && l > 0) { lop += l; lopCnt++; }
        }
        response.setTotalLeaveDaysUsedYTD(ytd); response.setAverageLossOfPayPercentage(lopCnt > 0 ? lop / lopCnt : 0.0);
    }

    public Map<LeaveStatus, Long> getLeaveCountsByStatus(Long employeeId, Integer year) {
        return applicationRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .collect(Collectors.groupingBy(LeaveApplication::getStatus, Collectors.counting()));
    }
    public List<EmployeeSummaryDTO> getEmployeesCurrentlyOnLeaveDTOs() { return getEmployeesCurrentlyOnLeave().stream().map(EmployeeSummaryDTO::from).toList(); }
    public List<EmployeeSummaryDTO> getManagersWithUpcomingLeaveDTOs() { return getManagersWithUpcomingLeave().stream().map(EmployeeSummaryDTO::from).toList(); }
    public List<EmployeeSummaryDTO> getAdminsWithUpcomingLeaveDTOs() { return getAdminsWithUpcomingLeave().stream().map(EmployeeSummaryDTO::from).toList(); }
    public List<ManagerDashboardResponse.TeamPendingLeaveDTO> getPendingTeamRequestDTOs(Long managerId) {
        return getPendingTeamRequests(managerId).stream().map(l -> {
            ManagerDashboardResponse.TeamPendingLeaveDTO d = new ManagerDashboardResponse.TeamPendingLeaveDTO();
            d.setLeaveId(l.getId()); d.setEmployeeId(l.getEmployeeId()); d.setLeaveType(l.getLeaveType());
            d.setStartDate(l.getStartDate()); d.setEndDate(l.getEndDate()); d.setStatus(l.getStatus()); d.setReason(l.getReason()); return d;
        }).toList();
    }
}