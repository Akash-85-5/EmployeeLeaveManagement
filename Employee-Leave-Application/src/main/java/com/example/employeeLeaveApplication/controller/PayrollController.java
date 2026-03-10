package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.service.PayrollService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService){
        this.payrollService = payrollService;
    }

    @PostMapping("/generate")
    public String generatePayroll(
            @RequestParam Integer year,
            @RequestParam Integer month) {

        payrollService.generatePayroll(year, month);

        return "Payroll generated successfully";
    }
}
