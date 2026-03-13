package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LossOfPayRecord;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.service.LossOfPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/lop")
@RequiredArgsConstructor
public class LossOfPayController {

    private final LossOfPayService lossOfPayService;
    private final EmployeeRepository employeeRepository;

    // ✅ Employee views own LOP summary
    @GetMapping("/my-lop")
    @PreAuthorize("#empId == authentication.principal.user.id")
    public ResponseEntity<Map<String, Object>> getMyLop(
            @RequestParam Long empId,
            @RequestParam Integer year) {
        getEmployee(empId);
        return ResponseEntity.ok(
                lossOfPayService.getLopSummary(empId, year));
    }

    // ✅ Self, HR, ADMIN, MANAGER, TL — monthly LOP for a specific month
    @GetMapping("/employee/{empId}/year/{year}/month/{month}")
    @PreAuthorize("#empId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<Map<String, Object>> getMonthlyLop(
            @PathVariable Long empId,
            @PathVariable Integer year,
            @PathVariable Integer month) {

        getEmployee(empId);

        String lopPercent = lossOfPayService
                .getMonthlyLossOfPay(empId, year, month);

        String monthName = Month.of(month)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId",    empId);
        response.put("year",          year);
        response.put("month",         monthName);
        response.put("lopPercentage", lopPercent);
        response.put("hasLop",        !lopPercent.equals("0.00%"));

        return ResponseEntity.ok(response);
    }

    // ✅ Self, HR, ADMIN, MANAGER, TL — yearly LOP total
    @GetMapping("/employee/{empId}/year/{year}/total")
    @PreAuthorize("#empId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<Map<String, Object>> getYearlyLop(
            @PathVariable Long empId,
            @PathVariable Integer year) {

        getEmployee(empId);

        String yearlyTotal = lossOfPayService
                .getYearlyLossOfPay(empId, year);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId",  empId);
        response.put("year",        year);
        response.put("yearlyTotal", yearlyTotal);

        return ResponseEntity.ok(response);
    }

    // ✅ Self, HR, ADMIN, MANAGER, TL — full month-by-month summary for a year
    @GetMapping("/employee/{empId}/year/{year}/summary")
    @PreAuthorize("#empId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<Map<String, Object>> getLopSummary(
            @PathVariable Long empId,
            @PathVariable Integer year) {

        getEmployee(empId);

        return ResponseEntity.ok(
                lossOfPayService.getLopSummary(empId, year));
    }

    // ✅ Self, HR, ADMIN, MANAGER, TL — all raw LOP records across all years
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<List<LossOfPayRecord>> listByEmployee(
            @PathVariable Long employeeId) {

        getEmployee(employeeId);

        return ResponseEntity.ok(
                lossOfPayService.getAllForEmployee(employeeId));
    }

    // ✅ Self, HR, ADMIN, MANAGER, TL — raw LOP records filtered by year
    @GetMapping("/employee/{empId}/year/{year}/records")
    @PreAuthorize("#empId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<List<LossOfPayRecord>> listByEmployeeAndYear(
            @PathVariable Long empId,
            @PathVariable Integer year) {

        getEmployee(empId);

        return ResponseEntity.ok(
                lossOfPayService.getForEmployeeAndYear(empId, year));
    }

    // ✅ TEAM_LEADER only — LOP summary for all team members
    @GetMapping("/team")
    @PreAuthorize("hasRole('TEAM_LEADER') and " +
            "#teamLeaderId == authentication.principal.user.id")
    public ResponseEntity<Map<String, Object>> getTeamLop(
            @RequestParam Long teamLeaderId,
            @RequestParam Integer year) {

        getEmployee(teamLeaderId);

        List<Employee> teamMembers = employeeRepository
                .findByTeamLeaderId(teamLeaderId);

        Map<String, Object> teamLop = new LinkedHashMap<>();
        for (Employee member : teamMembers) {
            teamLop.put(
                    member.getName(),
                    lossOfPayService.getLopSummary(
                            member.getId(), year));
        }

        return ResponseEntity.ok(teamLop);
    }

    // ✅ MANAGER only — LOP summary for all department members
    @GetMapping("/department")
    @PreAuthorize("hasRole('MANAGER') and " +
            "#managerId == authentication.principal.user.id")
    public ResponseEntity<Map<String, Object>> getDepartmentLop(
            @RequestParam Long managerId,
            @RequestParam Integer year) {

        getEmployee(managerId);

        List<Employee> deptMembers = employeeRepository
                .findByManagerId(managerId);

        Map<String, Object> deptLop = new LinkedHashMap<>();
        for (Employee member : deptMembers) {
            deptLop.put(
                    member.getName(),
                    lossOfPayService.getLopSummary(
                            member.getId(), year));
        }

        return ResponseEntity.ok(deptLop);
    }

    // ✅ HR and ADMIN only — LOP summary for all employees
    @GetMapping("/all")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllEmployeesLop(
            @RequestParam Integer year) {

        List<Employee> allEmployees = employeeRepository.findAll();

        Map<String, Object> allLop = new LinkedHashMap<>();
        for (Employee emp : allEmployees) {
            allLop.put(
                    emp.getName(),
                    lossOfPayService.getLopSummary(
                            emp.getId(), year));
        }

        return ResponseEntity.ok(allLop);
    }

    // ─── Private Helpers ────────────────────────────────────────────

    private Employee getEmployee(Long empId) {
        return employeeRepository.findById(empId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found: " + empId));
    }
}