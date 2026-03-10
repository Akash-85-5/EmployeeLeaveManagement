package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.EmployeeSalary;
import com.example.employeeLeaveApplication.repository.EmployeeSalaryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee-salary")
public class EmployeeSalaryController {

    private final EmployeeSalaryRepository repository;

    public EmployeeSalaryController(EmployeeSalaryRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public EmployeeSalary assignSalary(@RequestBody EmployeeSalary salary) {
        return repository.save(salary);
    }

    @GetMapping("/{employeeId}")
    public List<EmployeeSalary> getSalary(@PathVariable Long employeeId) {
        return repository.findByEmployeeId(employeeId);
    }
}
