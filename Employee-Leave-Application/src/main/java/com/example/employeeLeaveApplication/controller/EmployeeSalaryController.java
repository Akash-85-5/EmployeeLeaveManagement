package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.EmployeeSalary;
import com.example.employeeLeaveApplication.repository.EmployeeSalaryRepository;
import com.example.employeeLeaveApplication.service.EmployeeSalaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee-salary")
public class EmployeeSalaryController {

    private final EmployeeSalaryService service;
    private final EmployeeSalaryRepository repository;

    public EmployeeSalaryController(EmployeeSalaryService service, EmployeeSalaryRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    // HR assigns salary
    @PreAuthorize("hasRole('HR')")
    @PostMapping("/assign")
    public EmployeeSalary assignSalary(@RequestBody EmployeeSalary salary) {

        return service.assignSalary(salary);
    }

    // Get salary history
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/history/{employeeId}")
    public List<EmployeeSalary> getSalaryHistory(
            @PathVariable Long employeeId) {

        return service.getSalaryHistory(employeeId);
    }

    // Get current salary
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/current/{employeeId}")
    public EmployeeSalary getCurrentSalary(
            @PathVariable Long employeeId) {

        return service.getCurrentSalary(employeeId);
    }
    @PreAuthorize("hasRole('HR')")
    @DeleteMapping("/{id}")
    public String deleteEmployeeSalary(@PathVariable Long id) {

        repository.deleteById(id);

        return "Employee salary deleted successfully";
    }
}