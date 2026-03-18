package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.LeaveBalanceResponse;
import com.example.employeeLeaveApplication.service.LeaveBalanceService;
import com.example.employeeLeaveApplication.service.LossOfPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves-balance")
public class LeaveBalanceController {

    private final LeaveBalanceService balanceService;
    private final LossOfPayService    lossOfPayService;

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(LeaveBalanceController.class);

    public LeaveBalanceController(LeaveBalanceService balanceService,
                                  LossOfPayService lossOfPayService) {
        this.balanceService   = balanceService;
        this.lossOfPayService = lossOfPayService;
    }

    // ── Leave balance (all types) ─────────────────────────────────
    @GetMapping("/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') or hasRole('CFO')" +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER') or hasRole('CFO')" )
    public ResponseEntity<LeaveBalanceResponse> getBalance(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("📊 [API] GET balance: employee={}, year={}", employeeId, year);
        try {
            return ResponseEntity.ok(balanceService.getBalance(employeeId, year));
        } catch (Exception e) {
            log.error("❌ [API] Error getting balance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ── LOP total for a year (manual CFO entry, read-only here) ───
    @GetMapping("/lop/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<Double> getLossOfPay(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("💰 [API] GET LOP: employee={}, year={}", employeeId, year);
        try {
            return ResponseEntity.ok(
                    lossOfPayService.getTotalLossOfPayPercentage(employeeId, year));
        } catch (Exception e) {
            log.error("❌ [API] Error getting LOP: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}