package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.PersonalDetailsRequest;
import com.example.employeeLeaveApplication.dto.ProfileResponse;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.EmployeePersonalDetails;
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

    // ── View own profile (READ ONLY for employee) ─────────────────
    @GetMapping("/profile/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ResponseEntity<ProfileResponse> getProfile(
            @PathVariable Long employeeId) {
        System.out.println(employeeId);
        return ResponseEntity.ok(employeeService.getProfile(employeeId));
    }

    // ── Employee submits personal details ONCE ────────────────────
    // After submit → locked → this endpoint will throw error if called again
    @PostMapping("/personal-details/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ResponseEntity<EmployeePersonalDetails> submitPersonalDetails(
            @PathVariable Long employeeId,
            @RequestBody PersonalDetailsRequest request) {
        return ResponseEntity.ok(
                employeeService.submitPersonalDetails(employeeId, request));
    }

    // ── HR/Admin views full personal details ──────────────────────
    @GetMapping("/personal-details/{employeeId}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> getPersonalDetails(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getPersonalDetails(employeeId));
    }

    // ── Existing endpoints ────────────────────────────────────────

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR','CFO')")
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
        return employeeService.getAllEmployees(
                name, email, role, managerId, active, pageable);
    }

    // ==================== GET TEAM MEMBERS (REPORTEES)  ====================
    @GetMapping("/manager/{managerId}/team")
    @PreAuthorize("hasRole('MANAGER') and #managerId == authentication.principal.user.id")
    public List<Employee> getTeamMembers(@PathVariable Long managerId) {
        return employeeService.getTeamMembers(managerId);
    }


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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }
}