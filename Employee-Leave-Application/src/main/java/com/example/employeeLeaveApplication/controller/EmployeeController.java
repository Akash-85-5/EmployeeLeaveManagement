package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService){
        this.employeeService=employeeService;
    }



    @PostMapping
    public Employee create(@RequestBody Employee employee) {
        return employeeService.createEmployee(employee);
    }

    @GetMapping("/{id}")
    public Employee get(@PathVariable Long id) {
        return employeeService.getEmployee(id);
    }
}
