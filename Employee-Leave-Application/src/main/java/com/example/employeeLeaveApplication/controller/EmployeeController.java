package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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

    // ==================== GET EMPLOYEE BY ID ====================
    @GetMapping("/{id}")
    public Employee getEmployee(@PathVariable Long id) {
        return employeeService.getEmployee(id);
    }

    // ==================== GET ALL EMPLOYEES (WITH PAGINATION & FILTERS) - NEW ====================
    @GetMapping
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
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }

    // ==================== GET TEAM MEMBERS (REPORTEES)  ====================
    @GetMapping("/manager/{managerId}/team")
    public List<Employee> getTeamMembers(@PathVariable Long managerId) {
        return employeeService.getTeamMembers(managerId);
    }

    // ==================== GET CURRENT EMPLOYEE (SELF)  ====================
    @GetMapping("/profile")
    public Employee getCurrentEmployee(@RequestParam Long employeeId) {
        return employeeService.getEmployee(employeeId);
    }

    // ==================== UPDATE CURRENT EMPLOYEE (SELF)  ====================
    @PutMapping("/profile")
    public Employee updateCurrentEmployee(
            @RequestParam Long employeeId,
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