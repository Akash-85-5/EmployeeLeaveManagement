package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.PayrollSettings;
import com.example.employeeLeaveApplication.repository.PayrollSettingsRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll")
public class PayrollSettingsController {

    private final PayrollSettingsRepository payrollSettingsRepository;

    public PayrollSettingsController(PayrollSettingsRepository payrollSettingsRepository) {
        this.payrollSettingsRepository = payrollSettingsRepository;
    }

    @PostMapping("/settings")
    public PayrollSettings createSettings(@RequestBody PayrollSettings settings) {
        return payrollSettingsRepository.save(settings);
    }

    @GetMapping("/settings")
    public PayrollSettings getSettings() {
        return payrollSettingsRepository.findAll().get(0);
    }
}