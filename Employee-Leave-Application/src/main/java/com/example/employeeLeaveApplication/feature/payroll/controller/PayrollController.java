package com.example.employeeLeaveApplication.feature.payroll.controller;

import com.example.employeeLeaveApplication.feature.payroll.service.PayrollService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService){
        this.payrollService = payrollService;
    }

    @PreAuthorize("hasRole('CFO')")
    @PostMapping("/generate")
    public String generatePayroll(@RequestParam Integer year,
                                  @RequestParam Integer month){

        payrollService.generatePayroll(year,month);

        return "Payroll generated successfully";
    }


    @PreAuthorize("hasRole('CFO')")
    @PostMapping("/prepare")
    public String preparePayroll(@RequestParam Integer year,
                                 @RequestParam Integer month){

        payrollService.preparePayroll(year,month);

        return "Payroll prepared from previous month";
    }
}