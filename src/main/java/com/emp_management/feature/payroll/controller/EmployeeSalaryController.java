//package com.emp_management.feature.payroll.controller;
//
//import com.emp_management.feature.payroll.entity.EmployeeSalary;
//import com.emp_management.feature.payroll.service.EmployeeSalaryService;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/employee-salary")
//public class EmployeeSalaryController {
//
//    private final EmployeeSalaryService service;
//
//    public EmployeeSalaryController(EmployeeSalaryService service) {
//        this.service = service;
//    }
//
//    @PreAuthorize("hasRole('HR')")
//    @PostMapping("/assign")
//    public EmployeeSalary assignSalary(@RequestBody EmployeeSalary salary) {
//        return service.assignSalary(salary);
//    }
//
//    @PreAuthorize("hasRole('HR')")
//    @GetMapping("/history/{employeeId}")
//    public List<EmployeeSalary> getSalaryHistory(@PathVariable Long employeeId) {
//        return service.getSalaryHistory(employeeId);
//    }
//
//    @PreAuthorize("hasRole('HR')")
//    @GetMapping("/current/{employeeId}")
//    public EmployeeSalary getCurrentSalary(@PathVariable Long employeeId) {
//        return service.getCurrentSalary(employeeId);
//    }
//}
