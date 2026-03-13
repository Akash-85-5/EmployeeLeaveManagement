package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.service.PayrollService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService){
        this.payrollService = payrollService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('HR')")
    public String generatePayroll(
            @RequestParam Integer year,
            @RequestParam Integer month) {

        payrollService.generatePayroll(year, month);

        return "Payroll generated successfully";
    }

    @PutMapping("/mark-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public String markPaid(
            @RequestParam Integer year,
            @RequestParam Integer month) {

        payrollService.markPayrollPaid(year, month);

        return "Payroll marked as PAID";
    }
}