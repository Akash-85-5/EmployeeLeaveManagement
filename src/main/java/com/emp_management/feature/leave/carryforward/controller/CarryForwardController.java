package com.emp_management.feature.leave.carryforward.controller;


import com.emp_management.feature.leave.carryforward.dto.CarryForwardBalanceResponse;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardEligibilityResponse;
import com.emp_management.feature.leave.carryforward.service.CarryForwardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/carryforward")
public class CarryForwardController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CarryForwardController.class);

    private final CarryForwardService carryForwardService;

    public CarryForwardController(CarryForwardService carryForwardService) {
        this.carryForwardService = carryForwardService;
    }

    // ── Get balance for an employee ───────────────────────────────

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<CarryForwardBalanceResponse> getBalance(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        log.info("[CARRYFORWARD] Fetching balance: employee={}, year={}", employeeId, year);
        return ResponseEntity.ok(carryForwardService.getBalance(employeeId, year));
    }

    // ── Check eligibility for carry-forward ───────────────────────

    @GetMapping("/eligibility/{employeeId}")
    public ResponseEntity<CarryForwardEligibilityResponse> checkEligibility(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        log.info("[CARRYFORWARD] Checking eligibility: employee={}, year={}", employeeId, year);
        return ResponseEntity.ok(carryForwardService.checkEligibility(employeeId, year));
    }

    // ── HR / Admin: all balances for a year ───────────────────────

    @GetMapping("/balances/{year}")
    public ResponseEntity<List<CarryForwardBalanceResponse>> getAllBalances(
            @PathVariable Integer year) {
        log.info("[CARRYFORWARD] Fetching all balances for year: {}", year);
        return ResponseEntity.ok(carryForwardService.getAllBalances(year));
    }
}