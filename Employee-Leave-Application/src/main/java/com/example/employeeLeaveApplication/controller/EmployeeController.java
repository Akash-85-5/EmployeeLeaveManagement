package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.ProfileResponse;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ==================== CREATE EMPLOYEE ====================
//    @PostMapping
//    public Employee createEmployee(@RequestBody Employee employee) {
//        return employeeService.createEmployee(employee);
//    }

    // ==================== GET ALL EMPLOYEES (WITH PAGINATION & FILTERS) - NEW ====================
    @GetMapping("/all")
    @PreAuthorize("hasRole('HR')")
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

    // ==================== UPDATE EMPLOYEE  ====================
    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return employeeService.updateEmployee(id, employee);
    }

    // ==================== DELETE/DEACTIVATE EMPLOYEE  ====================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }

    // ==================== GET TEAM MEMBERS (REPORTEES)  ====================
    @GetMapping("/manager/{managerId}/team")
    @PreAuthorize("#managerId == authentication.principal.user.id")
    public List<Employee> getTeamMembers(@PathVariable Long managerId) {
        return employeeService.getTeamMembers(managerId);
    }

    // ==================== GET CURRENT EMPLOYEE (SELF)  ====================
    @GetMapping("/profile/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ProfileResponse getCurrentEmployee(@PathVariable Long employeeId) {
        return employeeService.getProfile(employeeId);
    }

    // ==================== UPDATE CURRENT EMPLOYEE (SELF)  ====================
    @PutMapping("/profile/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public Employee updateCurrentEmployee(
            @PathVariable Long employeeId,
            @RequestBody Employee employee
    ) {
        return employeeService.updateEmployee(employeeId, employee);
    }

    // ==================== SEARCH EMPLOYEES BY NAME  ====================
    @GetMapping("/search")
    public List<Employee> searchEmployees(@RequestParam String query) {
        return employeeService.searchEmployees(query);
    }


}