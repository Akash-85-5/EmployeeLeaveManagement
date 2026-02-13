package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.LeaveBalanceResponse;
import com.example.employeeLeaveApplication.service.LeaveBalanceService;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

@RestController
@RequestMapping("/api/leaves")
public class LeaveBalanceController {

    private final LeaveBalanceService balanceService;

    public LeaveBalanceController(LeaveBalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balance/{employeeId}")
    public LeaveBalanceResponse getBalance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {

        int targetYear = (year != null)
                ? year
                : Year.now().getValue();

        return balanceService.getBalance(employeeId, targetYear);
    }
}



