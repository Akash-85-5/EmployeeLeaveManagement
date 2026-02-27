package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.LeaveBalanceResponse;
import com.example.employeeLeaveApplication.service.LeaveBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

@RestController
@RequestMapping("/api/leaves-balance")
public class LeaveBalanceController {

    private final LeaveBalanceService balanceService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LeaveBalanceController.class);

    public LeaveBalanceController(LeaveBalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ResponseEntity<LeaveBalanceResponse> getBalance(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("📊 [API] GET balance: employee={}, year={}", employeeId, year);

        try {
            LeaveBalanceResponse response = balanceService.getBalance(employeeId, year);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [API] Error getting balance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeAllocations(
            @RequestParam Long employeeId,
            @RequestParam Integer year) {

        log.info("🆕 [API] POST initialize: employee={}, year={}", employeeId, year);

        try {
            balanceService.initializeAllocations(employeeId, year);
            return ResponseEntity.ok("Allocations initialized successfully for employee: " + employeeId);
        } catch (Exception e) {
            log.error("❌ [API] Error initializing: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/lop/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ResponseEntity<Double> getLossOfPay(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("💰 [API] GET LOP: employee={}, year={}", employeeId, year);

        try {
            Double lop = balanceService.getTotalLossOfPayPercentage(employeeId, year);
            return ResponseEntity.ok(lop);
        } catch (Exception e) {
            log.error("❌ [API] Error getting LOP: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}



