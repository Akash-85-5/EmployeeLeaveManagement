package com.emp_management.feature.leave.carryforward.controller;

import com.emp_management.feature.leave.carryforward.dto.CarryForwardBalanceResponse;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardEligibilityResponse;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardMonthlyUsageResponse;
import com.emp_management.feature.leave.carryforward.service.CarryForwardBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/carryforward")
public class CarryForwardController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CarryForwardController.class);

    private final CarryForwardBalanceService carryForwardBalanceService;

    public CarryForwardController(CarryForwardBalanceService carryForwardBalanceService) {
        this.carryForwardBalanceService = carryForwardBalanceService;
    }

    // ── Yearly balance for one employee ─────────────────────────────

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<CarryForwardBalanceResponse> getBalance(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {

        if (year == null) year = LocalDate.now().getYear();

        log.info("[CARRYFORWARD] Fetching balance: employee={}, year={}", employeeId, year);

        return ResponseEntity.ok(
                carryForwardBalanceService.getBalance(employeeId, year)
        );
    }

    // ── Eligibility check ───────────────────────────────────────────

    @GetMapping("/eligibility/{employeeId}")
    public ResponseEntity<CarryForwardEligibilityResponse> checkEligibility(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {

        if (year == null) year = LocalDate.now().getYear();

        log.info("[CARRYFORWARD] Checking eligibility: employee={}, year={}", employeeId, year);

        return ResponseEntity.ok(
                carryForwardBalanceService.checkEligibility(employeeId, year)
        );
    }

    // ── All balances (HR/ADMIN) ─────────────────────────────────────

    @GetMapping("/balances/{year}")
    public ResponseEntity<List<CarryForwardBalanceResponse>> getAllBalances(
            @PathVariable Integer year) {

        log.info("[CARRYFORWARD] Fetching all balances for year: {}", year);

        return ResponseEntity.ok(
                carryForwardBalanceService.getAllBalances(year)
        );
    }

    // ── Monthly usage ───────────────────────────────────────────────

    @GetMapping("/monthly/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<List<CarryForwardMonthlyUsageResponse>> getMonthlyUsage(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {

        if (year == null) year = LocalDate.now().getYear();

        log.info("[CARRYFORWARD] Fetching monthly usage: employee={}, year={}", employeeId, year);

        return ResponseEntity.ok(
                carryForwardBalanceService.getMonthlyUsage(employeeId, year)
        );
    }
}