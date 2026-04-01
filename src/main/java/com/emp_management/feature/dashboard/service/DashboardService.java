package com.emp_management.feature.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.emp_management.feature.dashboard.dto.*;
import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.entity.EmployeeOnboarding;
import com.emp_management.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.entity.LeaveAllocation;
import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.entity.SickLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.repository.AnnualLeaveMonthlyBalanceRepository;
import com.emp_management.feature.leave.annual.repository.LeaveAllocationRepository;
import com.emp_management.feature.leave.annual.repository.LeaveApplicationRepository;
import com.emp_management.feature.leave.annual.repository.SickLeaveMonthlyBalanceRepository;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.emp_management.feature.leave.compoff.entity.CompOffBalance;
import com.emp_management.feature.leave.compoff.repository.CompOffBalanceRepository;
import com.emp_management.shared.enums.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final CompOffBalanceRepository compOffRepository;
    private final CarryForwardBalanceRepository carryForwardRepository;
//    private final LopRecordRepository                   lopRepository;
//    private final ODRequestRepository                   odRepository;
    private final AnnualLeaveMonthlyBalanceRepository annualLeaveMonthlyBalanceRepository;
    private final SickLeaveMonthlyBalanceRepository     sickLeaveMonthlyBalanceRepository;
    private final EmployeePersonalDetailsRepository employeePersonalDetailsRepository;

    public DashboardService(EmployeeRepository employeeRepository,
                            LeaveAllocationRepository allocationRepository,
                            LeaveApplicationRepository applicationRepository,
                            CompOffBalanceRepository compOffRepository,
                            CarryForwardBalanceRepository carryForwardRepository,
//                            LopRecordRepository lopRepository,
//                            ODRequestRepository odRepository,
                            AnnualLeaveMonthlyBalanceRepository annualLeaveMonthlyBalanceRepository,
                            SickLeaveMonthlyBalanceRepository sickLeaveMonthlyBalanceRepository,
                            EmployeePersonalDetailsRepository employeePersonalDetailsRepository) {
        this.employeeRepository                 = employeeRepository;
        this.allocationRepository               = allocationRepository;
        this.applicationRepository              = applicationRepository;
        this.compOffRepository                  = compOffRepository;
        this.carryForwardRepository             = carryForwardRepository;
//        this.lopRepository                      = lopRepository;
//        this.odRepository                       = odRepository;
        this.annualLeaveMonthlyBalanceRepository = annualLeaveMonthlyBalanceRepository;
        this.sickLeaveMonthlyBalanceRepository  = sickLeaveMonthlyBalanceRepository;
        this.employeePersonalDetailsRepository  = employeePersonalDetailsRepository;
    }

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    public EmployeeDashboardResponse getDashboard(String employeeId) {
        log.info("DASHBOARD employee={}", employeeId);

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        int currentYear  = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        EmployeeDashboardResponse response = new EmployeeDashboardResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setCurrentYear(currentYear);
        response.setLastUpdated(LocalDateTime.now());

        // ── Yearly totals ──────────────────────────────────────────
        List<LeaveAllocation> allocations =
                allocationRepository.findByEmployee_EmpIdAndYear(employeeId, currentYear);

        double yearlyAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays).sum();

        Double yearlyUsed = applicationRepository
                .getTotalUsedDays(employeeId, RequestStatus.APPROVED, currentYear);
        if (yearlyUsed == null) yearlyUsed = 0.0;

        response.setYearlyAllocated(yearlyAllocated);
        response.setYearlyUsed(yearlyUsed);
        response.setYearlyBalance(yearlyAllocated - yearlyUsed);

        // ── Monthly ANNUAL_LEAVE balance ───────────────────────────
        AnnualLeaveMonthlyBalance annualMonthly = annualLeaveMonthlyBalanceRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, currentYear, currentMonth)
                .orElse(null);

        double annualAvailable = annualMonthly != null ? annualMonthly.getAvailableDays() : 0.0;
        double annualUsed      = annualMonthly != null ? annualMonthly.getUsedDays()      : 0.0;
        double annualRemaining = annualMonthly != null ? annualMonthly.getRemainingDays() : 0.0;

        response.setMonthlyAnnualAllocated(annualAvailable);
        response.setMonthlyAnnualUsed(annualUsed);
        response.setMonthlyAnnualBalance(annualRemaining);

        // ── Monthly SICK balance ───────────────────────────────────
        SickLeaveMonthlyBalance sickMonthly = sickLeaveMonthlyBalanceRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, currentYear, currentMonth)
                .orElse(null);

        double sickAvailable = sickMonthly != null ? sickMonthly.getAvailableDays() : 0.0;
        double sickUsed      = sickMonthly != null ? sickMonthly.getUsedDays()      : 0.0;
        double sickRemaining = sickMonthly != null ? sickMonthly.getRemainingDays() : 0.0;

        response.setMonthlySickAllocated(sickAvailable);
        response.setMonthlySickUsed(sickUsed);
        response.setMonthlySickBalance(sickRemaining);
        response.setMonthlyTotalBalance(annualRemaining + sickRemaining);

        // ── Carry forward ──────────────────────────────────────────
        CarryForwardBalance cf = carryForwardRepository
                .findByEmployee_EmpIdAndYear(employeeId, currentYear).orElse(null);
        response.setCarryForwardTotal(cf != null ? cf.getTotalCarriedForward() : 0.0);
        response.setCarryForwardUsed(cf != null ? cf.getTotalUsed()            : 0.0);
        response.setCarryForwardRemaining(cf != null ? cf.getRemaining()       : 0.0);

        // ── Comp-off ───────────────────────────────────────────────
        CompOffBalance compOff = compOffRepository
                .findByEmployeeIdAndYear(employeeId, currentYear).orElse(null);
        response.setCompoffBalance(compOff != null ? compOff.getBalance() : 0.0);

        // ── LOP ───────────────────────────────────────────────────
//        Double totalLOP = lopRepository.sumLopDaysForYear(employeeId, currentYear);
//        response.setLossOfPayPercentage(totalLOP != null ? totalLOP : 0.0);

        // ── Leave counts ───────────────────────────────────────────
        response.setApprovedCount(safeCount(applicationRepository
                .countByStatus(employeeId, currentYear, RequestStatus.APPROVED)));
        response.setRejectedCount(safeCount(applicationRepository
                .countByStatus(employeeId, currentYear, RequestStatus.REJECTED)));
        response.setPendingCount(safeCount(applicationRepository
                .countByStatus(employeeId, currentYear, RequestStatus.PENDING)));

        // ── Breakdown by leave type ────────────────────────────────
        List<LeaveApplication> approvedLeaves = applicationRepository
                .findByEmployee_EmpIdAndStatusAndYear(employeeId, RequestStatus.APPROVED, currentYear);
        List<LeaveApplication> pendingLeaves = applicationRepository
                .findByEmployee_EmpIdAndStatusAndYear(employeeId, RequestStatus.PENDING, currentYear);

        // Group by leaveType name (String) — LeaveType is now an entity
        Map<String, List<LeaveApplication>> byType =
                approvedLeaves.stream().collect(Collectors.groupingBy(
                        l -> l.getLeaveType().getLeaveType()));
        Map<String, List<LeaveApplication>> pendingByType =
                pendingLeaves.stream().collect(Collectors.groupingBy(
                        l -> l.getLeaveType().getLeaveType()));

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();
        for (LeaveAllocation allocation : allocations) {
            String typeName = allocation.getLeaveCategory().getLeaveType();
            double allocated = allocation.getAllocatedDays();

            List<LeaveApplication> typeLeaves = byType.getOrDefault(typeName, List.of());
            double used = typeLeaves.stream()
                    .mapToDouble(l -> l.getDays().doubleValue()).sum();

            // Use monthly balance for ANNUAL/SICK, else simple calc
            double remaining = switch (typeName.toUpperCase()) {
                case "ANNUAL_LEAVE" -> annualRemaining;
                case "SICK"         -> sickRemaining;
                default             -> allocated - used;
            };

            int halfDays = (int) typeLeaves.stream()
                    .filter(l -> l.getDays().compareTo(new BigDecimal("0.5")) == 0)
                    .count();
            long pendingCount = pendingByType.getOrDefault(typeName, List.of()).size();

            breakdown.add(new LeaveTypeBreakdown(
                    typeName, allocated, used, remaining, halfDays, pendingCount));
        }

        // Add COMP_OFF row
        double coEarned = compOff != null ? compOff.getEarned()  : 0.0;
        double coUsed   = compOff != null ? compOff.getUsed()    : 0.0;
        double coBal    = compOff != null ? compOff.getBalance() : 0.0;
        long coPending  = pendingByType.getOrDefault("COMP_OFF", List.of()).size();
        breakdown.add(new LeaveTypeBreakdown("COMP_OFF", coEarned, coUsed, coBal, 0, coPending));

        response.setBreakdown(breakdown);
        response.setCompoffBalance(coBal);

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // MONTHLY STATS
    // ═══════════════════════════════════════════════════════════════

    public MonthlyStatsResponse getMonthlyStats(String employeeId, Integer year, Integer month) {
        employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        MonthlyStatsResponse response = new MonthlyStatsResponse();
        response.setEmployeeId(employeeId);
        response.setYear(year);
        response.setMonth(month);

        AnnualLeaveMonthlyBalance balance = annualLeaveMonthlyBalanceRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month).orElse(null);

        double used      = balance != null ? balance.getUsedDays()      : 0.0;
        double available = balance != null ? balance.getAvailableDays() : 0.0;

        response.setTotalApprovedCount((int) used);
        response.setExceededLimit(used > available);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // TEAM MEMBERS
    // ═══════════════════════════════════════════════════════════════

    public List<TeamMember> getTeamMembers(String managerId) {
        return employeeRepository.findActiveTeamMembers(managerId).stream()
                .map(member -> {
                    TeamMember dto = new TeamMember();
                    dto.setEmployeeId(member.getEmpId());
                    dto.setEmployeeName(member.getName());
                    employeePersonalDetailsRepository
                            .findByEmployee_EmpId(member.getEmpId())
                            .ifPresent(d -> {
                                dto.setDesignation(d.getDesignation());
                                dto.setSkills(d.getSkillSet());
                            });
                    return dto;
                }).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // TEAM BALANCES
    // ═══════════════════════════════════════════════════════════════

    public List<TeamMemberBalance> getTeamBalances(String managerId, Integer year) {
        return employeeRepository.findActiveTeamMembers(managerId).stream()
                .map(member -> {
                    String empId = member.getEmpId();
                    TeamMemberBalance b = new TeamMemberBalance();
                    b.setEmployeeId(empId);
                    b.setEmployeeName(member.getName());

                    Double alloc = allocationRepository.getTotalAllocatedDays(empId, year);
                    Double used  = applicationRepository.getTotalUsedDays(
                            empId, RequestStatus.APPROVED, year);
                    b.setTotalAllocated(alloc != null ? alloc : 0.0);
                    b.setTotalUsed(used != null ? used : 0.0);
                    b.setTotalRemaining(b.getTotalAllocated() - b.getTotalUsed());

                    compOffRepository.findByEmployeeIdAndYear(empId, year)
                            .ifPresent(co -> b.setCompOffBalance(co.getBalance()));

//                    Double lop = lopRepository.sumLopDaysForYear(empId, year);
//                    b.setLopPercentage(lop != null ? lop : 0.0);
                    return b;
                }).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // MANAGER DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(String managerId) {
        ManagerDashboardResponse response = new ManagerDashboardResponse();
        response.setPersonalStats(getDashboard(managerId));

        List<Employee> team = employeeRepository.findActiveTeamMembers(managerId);
        response.setTeamSize(team.size());

        // Pending team requests
        List<LeaveApplication> pendingRequests =
                applicationRepository.findPendingTeamRequests(managerId);
        response.setTeamPendingRequestCount(pendingRequests.size());

        List<ManagerDashboardResponse.TeamPendingLeaveDTO> pendingDTOs =
                pendingRequests.stream().map(l -> new ManagerDashboardResponse.TeamPendingLeaveDTO(
                        l.getId(),
                        l.getEmployeeId(),
                        l.getEmployeeName(),
                        l.getLeaveType().getLeaveType(),   // entity → String name
                        l.getReason(),
                        l.getStatus(),
                        l.getStartDate(),
                        l.getEndDate(),
                        l.getDays().doubleValue(),
                        l.getCreatedAt()
                )).collect(Collectors.toList());
        response.setPendingTeamRequests(pendingDTOs);

        // Team on leave today
        LocalDate today = LocalDate.now();
        List<ManagerDashboardResponse.TeamMemberOnLeaveDTO> onLeave = new ArrayList<>();
        for (Employee m : team) {
            applicationRepository.findByEmployee_EmpIdAndStatus(m.getEmpId(), RequestStatus.APPROVED)
                    .stream()
                    .filter(la -> !today.isBefore(la.getStartDate())
                            && !today.isAfter(la.getEndDate()))
                    .forEach(l -> onLeave.add(
                            new ManagerDashboardResponse.TeamMemberOnLeaveDTO(
                                    m.getEmpId(),
                                    m.getName(),
                                    l.getLeaveType().getLeaveType(),
                                    l.getStartDate(),
                                    l.getEndDate(),
                                    (double) Math.max(0,
                                            ChronoUnit.DAYS.between(today, l.getEndDate()))
                            )));
        }
        response.setTeamOnLeaveToday(onLeave);
        response.setTeamOnLeaveCount(onLeave.size());
        response.setLastUpdated(LocalDateTime.now());
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD
    // ═══════════════════════════════════════════════════════════════
//
//    @Transactional(readOnly = true)
//    public HRDashboardResponse getHRDashboard() {
//        int yr = LocalDate.now().getYear();
//        HRDashboardResponse response = new HRDashboardResponse();
//        response.setCurrentYear(yr);
//
//        List<Employee> active = employeeRepository.findActiveEmployees();
//        response.setTotalEmployees(active.size());
//        response.setActiveEmployees(active.size());
//
//        LocalDate today = LocalDate.now();
//        response.setEmployeesOnLeaveToday(
//                applicationRepository.findEmployeesCurrentlyOnLeave(today).size());
//        response.setTotalPendingLeaves(
//                applicationRepository.findByStatus(RequestStatus.PENDING).size());
//
//        int approvedCnt = 0;
//        for (Employee e : active) {
//            approvedCnt += applicationRepository
//                    .findByEmployee_EmpIdAndStatusAndYear(
//                            e.getEmpId(), RequestStatus.APPROVED, yr).size();
//        }
//        response.setTotalApprovedLeaves(approvedCnt);
//
//        List<HRDashboardResponse.OnboardingPendingDTO> oDTOs = onboarding.stream()
//                .map(e -> {
//                    EmployeeOnboarding ob = e.getOnboarding();  // from the OneToOne
//                    return new HRDashboardResponse.OnboardingPendingDTO(
//                            e.getEmpId(),
//                            e.getName(),
//                            e.getEmail(),
//                            ob != null ? ob.getJoiningDate()     : null,
//                            ob != null ? ob.getBiometricStatus() : null,
//                            ob != null ? ob.getVpnStatus()       : null,
//                            ob != null && ob.getJoiningDate() != null
//                                    ? (int) ChronoUnit.DAYS.between(ob.getJoiningDate(), today)
//                                    : 0);
//                }).collect(Collectors.toList());
//        response.setOnboardingPendingList(oDTOs);
//
//        // Employees on leave today
//        List<HRDashboardResponse.EmployeeOnLeaveDTO> olDTOs = new ArrayList<>();
//        for (LeaveApplication l : applicationRepository
//                .findApprovedLeavesInDateRange(today, today)) {
//
//            Employee emp = employeeRepository.findByEmpId(l.getEmployeeId()).orElse(null);
//            if (emp == null) continue;
//
//            String managerName = emp.getReportingId() != null
//                    ? employeeRepository.findByEmpId(emp.getReportingId())
//                    .map(Employee::getName).orElse("N/A")
//                    : "N/A";
//
//            String approverName = l.getApprovedBy() != null
//                    ? employeeRepository.findByEmpId(l.getApprovedBy())
//                    .map(Employee::getName).orElse("N/A")
//                    : "N/A";
//
//            olDTOs.add(new HRDashboardResponse.EmployeeOnLeaveDTO(
//                    emp.getEmpId(),
//                    emp.getName(),
//                    managerName,
//                    l.getLeaveType().getLeaveType(),    // entity → String
//                    l.getStartDate(),
//                    l.getEndDate(),
//                    l.getDays().doubleValue(),
//                    l.getApprovedAt() != null ? l.getApprovedAt().toLocalDate() : null,
//                    approverName));
//        }
//        response.setEmployeesOnLeave(olDTOs);
//
//        // Manager approval stats
//        List<String> mgrIds = applicationRepository.findManagersWhoApprovedLeaves(yr);
//        response.setTotalManagersWithApprovals(mgrIds.size());
//
//        List<HRDashboardResponse.ManagerApprovalStatsDTO> mStats = new ArrayList<>();
//        for (String mid : mgrIds) {
//            Employee mgr = employeeRepository.findByEmpId(mid).orElse(null);
//            if (mgr == null) continue;
//
//            List<Employee> t = employeeRepository.findTeamMembersByManager(mid);
//            List<LeaveApplication> ap = applicationRepository
//                    .findLeavesApprovedByManager(mid, yr);
//
//            int tp = t.stream().mapToInt(tm -> applicationRepository
//                    .findByEmployee_EmpIdAndStatus(tm.getEmpId(), RequestStatus.PENDING)
//                    .size()).sum();
//
//            LocalDate lastApproval = ap.isEmpty() ? null
//                    : ap.get(0).getApprovedAt().toLocalDate();
//
//            mStats.add(new HRDashboardResponse.ManagerApprovalStatsDTO(
//                    mid,
//                    mgr.getName(),
//                    t.size(),
//                    ap.size(),
//                    tp,
//                    !t.isEmpty() ? (int) ((ap.size() / (double) t.size()) * 100) : 0,
//                    lastApproval));
//        }
//        response.setManagerApprovalStats(mStats);
//
//        // Team structure
//        List<HRDashboardResponse.TeamStructureDTO> ts = new ArrayList<>();
//        for (Employee mgr : employeeRepository.findAllManagers()) {
//            List<Employee> t = employeeRepository.findTeamMembersByManager(mgr.getEmpId());
//            List<HRDashboardResponse.TeamMemberDTO> mDTOs = t.stream().map(m -> {
//                String empId = m.getEmpId();
//                Double alloc = allocationRepository.getTotalAllocatedDays(empId, yr);
//                Double used  = applicationRepository.getTotalUsedDays(
//                        empId, RequestStatus.APPROVED, yr);
//                CarryForwardBalance cfb = carryForwardRepository
//                        .findByEmployee_EmpIdAndYear(empId, yr).orElse(null);
//                CompOffBalance co = compOffRepository
//                        .findByEmployee_EmpIdAndYear(empId, yr).orElse(null);
//                return new HRDashboardResponse.TeamMemberDTO(
//                        empId, m.getName(), m.getEmail(),
//                        (alloc != null ? alloc : 0.0) - (used != null ? used : 0.0),
//                        cfb != null ? cfb.getRemaining()  : 0.0,
//                        co  != null ? co.getBalance()     : 0.0);
//            }).collect(Collectors.toList());
//
//            ts.add(new HRDashboardResponse.TeamStructureDTO(
//                    mgr.getEmpId(), mgr.getName(), t.size(), mDTOs));
//        }
//        response.setTeamStructure(ts);
//        response.setLastUpdated(LocalDateTime.now());
//        return response;
//    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN DASHBOARD
    // ═══════════════════════════════════════════════════════════════

//    @Transactional(readOnly = true)
//    public AdminDashboardResponse getAdminDashboard(String adminId) {
//        AdminDashboardResponse response = new AdminDashboardResponse();
//        response.setPersonalStats(getDashboard(adminId));
//        response.setCurrentYear(LocalDate.now().getYear());
//        response.setLastUpdated(LocalDateTime.now());
//
//        List<Employee> all = employeeRepository.findActiveEmployees();
//        response.setTotalEmployees(all.size());
//        response.setTotalManagers(employeeRepository.findAllManagers().size());
//        response.setNewEmployeesPendingOnboarding(
//                employeeRepository.findOnboardingPending().size());
//        response.setTotalPendingLeaves(
//                applicationRepository.findByStatus(RequestStatus.PENDING).size());
//        response.setTotalRejectedLeaves(
//                applicationRepository.findByStatus(RequestStatus.REJECTED).size());
//
//        calculateGlobalMetrics(response, all);
//        return response;
//    }
//
//    private void calculateGlobalMetrics(AdminDashboardResponse response, List<Employee> all) {
//        int yr = response.getCurrentYear();
//        double ytd = 0, lop = 0; int lopCnt = 0;
//        double totalCF = 0, totalCO = 0;
//
//        for (Employee e : all) {
//            String empId = e.getEmpId();
//            Double u = applicationRepository.getTotalUsedDays(
//                    empId, RequestStatus.APPROVED, yr);
//            if (u != null) ytd += u;
//
//            CarryForwardBalance cf = carryForwardRepository
//                    .findByEmployee_EmpIdAndYear(empId, yr).orElse(null);
//            if (cf != null) totalCF += cf.getRemaining();
//
//            CompOffBalance co = compOffRepository
//                    .findByEmployee_EmpIdAndYear(empId, yr).orElse(null);
//            if (co != null) totalCO += co.getBalance();
//
//            Double l = lopRepository.sumLopDaysForYear(empId, yr);
//            if (l != null && l > 0) { lop += l; lopCnt++; }
//        }
//
//        response.setTotalLeaveDaysUsedYTD(ytd);
//        response.setTotalCarryForwardBalance(totalCF);
//        response.setTotalCompOffBalance(totalCO);
//        response.setAverageLossOfPayPercentage(lopCnt > 0 ? lop / lopCnt : 0.0);
//    }

    // ═══════════════════════════════════════════════════════════════
    // TEAM ON LEAVE / CALENDAR
    // ═══════════════════════════════════════════════════════════════

    public List<TeamMemberBalance> getTeamMembersOnLeaveToday(String managerId) {
        LocalDate today = LocalDate.now();
        return employeeRepository.findActiveTeamMembers(managerId).stream()
                .filter(m -> applicationRepository
                        .findByEmployee_EmpIdAndStatus(m.getEmpId(), RequestStatus.APPROVED)
                        .stream()
                        .anyMatch(la -> !today.isBefore(la.getStartDate())
                                && !today.isAfter(la.getEndDate())))
                .map(m -> {
                    TeamMemberBalance b = new TeamMemberBalance();
                    b.setEmployeeId(m.getEmpId());
                    b.setEmployeeName(m.getName());
                    return b;
                }).collect(Collectors.toList());
    }

    public Map<String, List<TeamMemberBalance>> getTeamLeaveCalendar(String managerId) {
        Map<String, List<TeamMemberBalance>> cal = new TreeMap<>();
        for (Employee m : employeeRepository.findActiveTeamMembers(managerId)) {
            processLeavesIntoCalendar(cal, m,
                    applicationRepository.findByEmployee_EmpIdAndStatus(
                            m.getEmpId(), RequestStatus.APPROVED));
//            processODsIntoCalendar(cal, m,
//                    odRepository.findByEmployee_EmpIdAndStatus(
//                            m.getEmpId(), RequestStatus.APPROVED));
        }
        return cal;
    }

    public Map<String, List<TeamMemberBalance>> getMyLeaveCalendar(String employeeId) {
        Employee m = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        Map<String, List<TeamMemberBalance>> cal = new TreeMap<>();
        processLeavesIntoCalendar(cal, m,
                applicationRepository.findByEmployee_EmpIdAndStatus(
                        employeeId, RequestStatus.APPROVED));
//        processODsIntoCalendar(cal, m,
//                odRepository.findByEmployee_EmpIdAndStatus(
//                        employeeId, RequestStatus.APPROVED));
        return cal;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPANY-WIDE STATS
    // ═══════════════════════════════════════════════════════════════

    public Map<String, Object> getCompanyWideStats(Integer year) {
        Map<String, Object> stats = new HashMap<>();
        List<Employee> active = employeeRepository.findActiveEmployees();
        stats.put("totalEmployees", active.size());

        double totalDays = 0; int totalApproved = 0;
        for (Employee e : active) {
            Double u = applicationRepository.getTotalUsedDays(
                    e.getEmpId(), RequestStatus.APPROVED, year);
            if (u != null) { totalDays += u; totalApproved++; }
        }
        stats.put("totalApprovedLeaves", totalApproved);
        stats.put("totalDaysUsed", totalDays);

//        double totalLOP = 0; int lopCount = 0;
//        for (Employee e : active) {
//            Double l = lopRepository.sumLopDaysForYear(e.getEmpId(), year);
//            if (l != null && l > 0) { totalLOP += l; lopCount++; }
//        }
//        stats.put("totalLopPercentage", lopCount > 0 ? totalLOP / lopCount : 0.0);
//        stats.put("employeesWithLOP", lopCount);

        double cfU = 0, cfT = 0;
        for (Employee e : active) {
            CarryForwardBalance cf = carryForwardRepository
                    .findByEmployee_EmpIdAndYear(e.getEmpId(), year).orElse(null);
            if (cf != null) { cfU += cf.getTotalUsed(); cfT += cf.getTotalCarriedForward(); }
        }
        stats.put("carryForwardUtilization", cfT > 0 ? (cfU / cfT * 100) : 0.0);
        return stats;
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY QUERY METHODS
    // ═══════════════════════════════════════════════════════════════

    public Integer getPendingTeamRequestsCount(String managerId) {
        return employeeRepository.findActiveTeamMembers(managerId).stream()
                .mapToInt(m -> applicationRepository
                        .findByEmployee_EmpIdAndStatus(m.getEmpId(), RequestStatus.PENDING)
                        .size())
                .sum();
    }

    public List<LeaveApplication> getPendingTeamRequests(String managerId) {
        List<LeaveApplication> all = new ArrayList<>();
        for (Employee m : employeeRepository.findActiveTeamMembers(managerId)) {
            all.addAll(applicationRepository
                    .findByEmployee_EmpIdAndStatus(m.getEmpId(), RequestStatus.PENDING));
        }
        all.sort(Comparator.comparing(LeaveApplication::getCreatedAt));
        return all;
    }

    public Map<RequestStatus, Long> getLeaveCountsByStatus(String employeeId, Integer year) {
        return applicationRepository.findByEmployee_EmpIdAndYear(employeeId, year).stream()
                .collect(Collectors.groupingBy(LeaveApplication::getStatus, Collectors.counting()));
    }

    public List<Employee> getEmployeesCurrentlyOnLeave() {
        LocalDate today = LocalDate.now();
        return employeeRepository.findActiveEmployees().stream()
                .filter(e -> applicationRepository
                        .findByEmployee_EmpIdAndStatus(e.getEmpId(), RequestStatus.APPROVED)
                        .stream()
                        .anyMatch(la -> !today.isBefore(la.getStartDate())
                                && !today.isAfter(la.getEndDate())))
                .collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getEmployeesWithLowBalance(Integer year, Double threshold) {
        return employeeRepository.findActiveEmployees().stream().map(e -> {
            String empId = e.getEmpId();
            Double a = allocationRepository.getTotalAllocatedDays(empId, year);
            Double u = applicationRepository.getTotalUsedDays(empId, RequestStatus.APPROVED, year);
            if (a == null) a = 0.0; if (u == null) u = 0.0;
            if (a - u >= threshold) return null;
            TeamMemberBalance b = new TeamMemberBalance();
            b.setEmployeeId(empId); b.setEmployeeName(e.getName());
            b.setTotalAllocated(a); b.setTotalUsed(u); b.setTotalRemaining(a - u);
            return b;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getEmployeesWithHighLOP(Integer year, Double threshold) {
        return employeeRepository.findActiveEmployees().stream().map(e -> {
//            Double lop = lopRepository.sumLopDaysForYear(e.getEmpId(), year);
//            if (lop == null || lop <= threshold) return null;
            TeamMemberBalance b = new TeamMemberBalance();
            b.setEmployeeId(e.getEmpId()); b.setEmployeeName(e.getName());
//            b.setLopPercentage(lop);
            return b;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getCarryForwardEligible(Integer year) {
        return employeeRepository.findActiveEmployees().stream().map(e -> {
            String empId = e.getEmpId();
            Double a = allocationRepository.getTotalAllocatedDays(empId, year);
            Double u = applicationRepository.getTotalUsedDays(empId, RequestStatus.APPROVED, year);
            if (a == null) a = 0.0; if (u == null) u = 0.0;
            double bal = a - u; if (bal <= 0) return null;
            TeamMemberBalance b = new TeamMemberBalance();
            b.setEmployeeId(empId); b.setEmployeeName(e.getName());
            b.setTotalAllocated(a); b.setTotalUsed(u);
            b.setTotalRemaining(Math.min(bal, 10.0)); // max carry forward cap
            return b;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<TeamMemberBalance> getEmployeesExceedingMonthlyLimit(Integer year, Integer month) {
        return employeeRepository.findActiveEmployees().stream().map(e -> {
            AnnualLeaveMonthlyBalance bal = annualLeaveMonthlyBalanceRepository
                    .findByEmployeeIdAndYearAndMonth(e.getEmpId(), year, month).orElse(null);
            if (bal == null || bal.getUsedDays() <= bal.getAvailableDays()) return null;
            TeamMemberBalance b = new TeamMemberBalance();
            b.setEmployeeId(e.getEmpId()); b.setEmployeeName(e.getName());
            b.setTotalUsed(bal.getUsedDays());
            return b;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public EmployeeDashboardResponse getEmployeeDashboard(String employeeId) {
        return getDashboard(employeeId);
    }

    // ── DTO conversion helpers ─────────────────────────────────────

    public List<EmployeeSummaryDTO> getEmployeesCurrentlyOnLeaveDTOs() {
        return getEmployeesCurrentlyOnLeave().stream().map(EmployeeSummaryDTO::from).toList();
    }

    public List<ManagerDashboardResponse.TeamPendingLeaveDTO> getPendingTeamRequestDTOs(
            String managerId) {
        return getPendingTeamRequests(managerId).stream().map(l ->
                new ManagerDashboardResponse.TeamPendingLeaveDTO(
                        l.getId(),
                        l.getEmployeeId(),
                        l.getEmployeeName(),
                        l.getLeaveType().getLeaveType(),
                        l.getReason(),
                        l.getStatus(),
                        l.getStartDate(),
                        l.getEndDate(),
                        l.getDays().doubleValue(),
                        l.getCreatedAt())
        ).toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void processLeavesIntoCalendar(Map<String, List<TeamMemberBalance>> cal,
                                           Employee m, List<LeaveApplication> leaves) {
        for (LeaveApplication l : leaves) {
            LocalDate d = l.getStartDate();
            while (!d.isAfter(l.getEndDate())) {
                addToCalendar(cal, d, m);
                d = d.plusDays(1);
            }
        }
    }

//    private void processODsIntoCalendar(Map<String, List<TeamMemberBalance>> cal,
//                                        Employee m, List<ODRequest> ods) {
//        for (ODRequest od : ods) {
//            LocalDate d = od.getStartDate();
//            while (!d.isAfter(od.getEndDate())) {
//                addToCalendar(cal, d, m);
//                d = d.plusDays(1);
//            }
//        }
//    }

    private void addToCalendar(Map<String, List<TeamMemberBalance>> cal,
                               LocalDate date, Employee m) {
        TeamMemberBalance e = new TeamMemberBalance();
        e.setEmployeeId(m.getEmpId());
        e.setEmployeeName(m.getName());
        cal.computeIfAbsent(date.toString(), k -> new ArrayList<>()).add(e);
    }

    private int safeCount(Integer val) {
        return val != null ? val : 0;
    }
}