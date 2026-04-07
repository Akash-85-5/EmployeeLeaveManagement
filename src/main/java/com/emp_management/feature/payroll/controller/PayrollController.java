package com.emp_management.feature.payroll.controller;

import com.emp_management.feature.payroll.service.PayrollService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public String generatePayroll(@RequestParam Integer year,
                                  @RequestParam Integer month){

        payrollService.generatePayroll(year,month);

        return "Payroll generated successfully";
    }


    @PostMapping("/prepare")
    public String preparePayroll(@RequestParam Integer year,
                                 @RequestParam Integer month){

        payrollService.preparePayroll(year,month);

        return "Payroll prepared from previous month";
    }
}
