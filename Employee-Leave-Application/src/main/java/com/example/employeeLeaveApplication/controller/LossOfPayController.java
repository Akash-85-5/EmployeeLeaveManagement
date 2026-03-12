package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LossOfPayRecord;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.service.LossOfPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE — view only their own LOP
    // GET /api/lop/my-lop?empId=1&year=2025
    // ═══════════════════════════════════════════════════════════════
    @GetMapping("/my-lop")
    public ResponseEntity<Map<String, Object>> getMyLop(
            @RequestParam Long empId,
            @RequestParam Long requesterId,  // ✅ added
            @RequestParam Integer year) {    // ✅ already existed, just keep it

        // ✅ Ensure caller can only view their own LOP
        if (!requesterId.equals(empId)) {
            throw new BadRequestException("You can only view your own LOP records");
        }

        getEmployee(empId);

        return ResponseEntity.ok(lossOfPayService.getLopSummary(empId, year));
    }

    // ═══════════════════════════════════════════════════════════════
    // MONTHLY — view specific month LOP
    // GET /api/lop/employee/{empId}/year/{year}/month/{month}
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/employee/{empId}/year/{year}/month/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlyLop(
            @PathVariable Long empId,
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestParam Long requesterId) {

        validateAccess(requesterId, empId);

        String lopPercent = lossOfPayService.getMonthlyLossOfPay(empId, year, month);

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

    // ═══════════════════════════════════════════════════════════════
    // YEARLY TOTAL — total LOP% for the year
    // GET /api/lop/employee/{empId}/year/{year}/total
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/employee/{empId}/year/{year}/total")
    public ResponseEntity<Map<String, Object>> getYearlyLop(
            @PathVariable Long empId,
            @PathVariable Integer year,
            @RequestParam Long requesterId) {

        validateAccess(requesterId, empId);

        String yearlyTotal = lossOfPayService.getYearlyLossOfPay(empId, year);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId",  empId);
        response.put("year",        year);
        response.put("yearlyTotal", yearlyTotal);

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════
    // FULL SUMMARY — month by month breakdown for a year
    // GET /api/lop/employee/{empId}/year/{year}/summary
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/employee/{empId}/year/{year}/summary")
    public ResponseEntity<Map<String, Object>> getLopSummary(
            @PathVariable Long empId,
            @PathVariable Integer year,
            @RequestParam Long requesterId) {

        validateAccess(requesterId, empId);

        return ResponseEntity.ok(lossOfPayService.getLopSummary(empId, year));
    }

    // ═══════════════════════════════════════════════════════════════
    // TEAM LEADER — view their team members LOP
    // GET /api/lop/team?teamLeaderId=2&year=2025
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/team")
    public ResponseEntity<Map<String, Object>> getTeamLop(
            @RequestParam Long teamLeaderId,
            @RequestParam Integer year) {

        Employee tl = getEmployee(teamLeaderId);

        if (tl.getRole() != Role.TEAM_LEADER) {
            throw new BadRequestException("Only Team Leader can access this");
        }

        List<Employee> teamMembers = employeeRepository
                .findByTeamLeaderId(teamLeaderId);

        Map<String, Object> teamLop = new LinkedHashMap<>();
        for (Employee member : teamMembers) {
            teamLop.put(
                    member.getName(),
                    lossOfPayService.getLopSummary(member.getId(), year)
            );
        }

        return ResponseEntity.ok(teamLop);
    }

    // ═══════════════════════════════════════════════════════════════
    // MANAGER — view their department employees LOP
    // GET /api/lop/department?managerId=3&year=2025
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/department")
    public ResponseEntity<Map<String, Object>> getDepartmentLop(
            @RequestParam Long managerId,
            @RequestParam Integer year) {

        Employee manager = getEmployee(managerId);

        if (manager.getRole() != Role.MANAGER) {
            throw new BadRequestException("Only Manager can access this");
        }

        List<Employee> deptMembers = employeeRepository
                .findByManagerId(managerId);

        Map<String, Object> deptLop = new LinkedHashMap<>();
        for (Employee member : deptMembers) {
            deptLop.put(
                    member.getName(),
                    lossOfPayService.getLopSummary(member.getId(), year)
            );
        }

        return ResponseEntity.ok(deptLop);
    }

    // ═══════════════════════════════════════════════════════════════
    // HR & ADMIN — view ALL employees LOP
    // GET /api/lop/all?requesterId=4&year=2025
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllEmployeesLop(
            @RequestParam Long requesterId,
            @RequestParam Integer year) {

        Employee requester = getEmployee(requesterId);

        if (requester.getRole() != Role.HR &&
                requester.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only HR or Admin can access this");
        }

        List<Employee> allEmployees = employeeRepository.findAll();

        Map<String, Object> allLop = new LinkedHashMap<>();
        for (Employee emp : allEmployees) {
            allLop.put(
                    emp.getName(),
                    lossOfPayService.getLopSummary(emp.getId(), year)
            );
        }

        return ResponseEntity.ok(allLop);
    }

    // ═══════════════════════════════════════════════════════════════
    // ALL RECORDS — get all LOP records for an employee (all years)
    // GET /api/lop/employee/{employeeId}
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LossOfPayRecord>> listByEmployee(
            @PathVariable Long employeeId,
            @RequestParam Long requesterId) {

        validateAccess(requesterId, employeeId);

        return ResponseEntity.ok(lossOfPayService.getAllForEmployee(employeeId));
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTE: DELETE endpoint has been REMOVED intentionally
    // LOP records are PERMANENT — they can only be reversed
    // internally by the leave cancellation flow
    // No one (including Admin) can manually delete a LOP record
    // ═══════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates that requester has permission to view the employee's LOP.
     * Rules:
     * - Employee can only view their own
     * - Team Leader can view their own team
     * - Manager can view their own department
     * - HR and Admin can view everyone
     */
    private void validateAccess(Long requesterId, Long targetEmpId) {

        Employee requester = getEmployee(requesterId);

        // Same person — always allowed
        if (requesterId.equals(targetEmpId)) return;

        switch (requester.getRole()) {
            case HR, ADMIN -> {
                // HR and Admin can view anyone
            }
            case MANAGER -> {
                // Manager can only view their own department
                Employee target = getEmployee(targetEmpId);
                if (!requesterId.equals(target.getManagerId())) {
                    throw new BadRequestException(
                            "Manager can only view their department employees' LOP");
                }
            }
            case TEAM_LEADER -> {
                // TL can only view their own team
                Employee target = getEmployee(targetEmpId);
                if (!requesterId.equals(target.getTeamLeaderId())) {
                    throw new BadRequestException(
                            "Team Leader can only view their team members' LOP");
                }
            }
            default -> throw new BadRequestException(
                    "Unauthorized: You can only view your own LOP records");
        }
    }

    private Employee getEmployee(Long empId) {
        return employeeRepository.findById(empId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found: " + empId));
    }
}