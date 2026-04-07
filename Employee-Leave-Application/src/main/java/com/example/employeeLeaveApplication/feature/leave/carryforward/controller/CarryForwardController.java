package com.example.employeeLeaveApplication.feature.leave.carryforward.controller;

import com.example.employeeLeaveApplication.feature.leave.carryforward.dto.CarryForwardBalanceResponse;
import com.example.employeeLeaveApplication.feature.leave.carryforward.dto.CarryForwardEligibilityResponse;
import com.example.employeeLeaveApplication.feature.leave.carryforward.service.CarryForwardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/carryforward")
@RequiredArgsConstructor
public class CarryForwardController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CarryForwardController.class);

    private final CarryForwardService carryForwardService;

    // ── Get balance for an employee ───────────────────────────────

    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<CarryForwardBalanceResponse> getBalance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        log.info("[CARRYFORWARD] Fetching balance: employee={}, year={}", employeeId, year);
        return ResponseEntity.ok(carryForwardService.getBalance(employeeId, year));
    }

    // ── Check eligibility for carry-forward ───────────────────────

    @GetMapping("/eligibility/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<CarryForwardEligibilityResponse> checkEligibility(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        log.info("[CARRYFORWARD] Checking eligibility: employee={}, year={}", employeeId, year);
        return ResponseEntity.ok(carryForwardService.checkEligibility(employeeId, year));
    }

    // ── HR / Admin: all balances for a year ───────────────────────

    @GetMapping("/balances/{year}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<List<CarryForwardBalanceResponse>> getAllBalances(
            @PathVariable Integer year) {
        log.info("[CARRYFORWARD] Fetching all balances for year: {}", year);
        return ResponseEntity.ok(carryForwardService.getAllBalances(year));
    }
}