package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.ProfileResponse;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ==================== GET ALL EMPLOYEES (HR & ADMIN) ====================

    @GetMapping("/all")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public Page<Employee> getAllEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return employeeService.getAllEmployees(name, email, role, managerId, active, pageable);
    }

    // ==================== GET PROFILE (EMPLOYEE SELF / ADMIN / HR) ====================

    // ✅ FIXED: Employee sees own, ADMIN and HR see any
    @GetMapping("/profile/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('ADMIN') or hasRole('HR')")
    public ProfileResponse getCurrentEmployee(@PathVariable Long employeeId) {
        return employeeService.getProfile(employeeId);
    }

    // ==================== UPDATE EMPLOYEE (ADMIN ONLY) ====================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Employee updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return employeeService.updateEmployee(id, employee);
    }

    // ==================== DELETE/DEACTIVATE EMPLOYEE (ADMIN ONLY) ====================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }

    // ==================== GET TEAM MEMBERS — MANAGER ====================

    @GetMapping("/manager/{managerId}/team")
    @PreAuthorize("hasRole('MANAGER') and #managerId == authentication.principal.user.id")
    public List<Employee> getTeamMembers(@PathVariable Long managerId) {
        return employeeService.getTeamMembers(managerId);
    }

    // ==================== GET TEAM MEMBERS — TEAM LEADER ====================

    @GetMapping("/teamleader/{teamLeaderId}/team")
    @PreAuthorize("hasRole('TEAM_LEADER') and #teamLeaderId == authentication.principal.user.id")
    public List<Employee> getTeamLeaderMembers(@PathVariable Long teamLeaderId) {
        return employeeService.getTeamLeaderMembers(teamLeaderId);
    }

    // ==================== SEARCH EMPLOYEES ====================

    @GetMapping("/search")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') ")
    public List<Employee> searchEmployees(@RequestParam String query) {
        return employeeService.searchEmployees(query);
    }
}