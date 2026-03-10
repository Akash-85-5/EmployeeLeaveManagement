package com.example.employeeLeaveApplication.component;

import com.example.employeeLeaveApplication.service.PayrollService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PayrollScheduler {

    private final PayrollService payrollService;

    public PayrollScheduler (PayrollService payrollService){
        this.payrollService = payrollService;
    }
    @Scheduled(cron = "0 0 2 1 * ?")
    public void runMonthlyPayroll() {

        LocalDate now = LocalDate.now();

        payrollService.generatePayroll(
                now.getYear(),
                now.getMonthValue());
    }
}
