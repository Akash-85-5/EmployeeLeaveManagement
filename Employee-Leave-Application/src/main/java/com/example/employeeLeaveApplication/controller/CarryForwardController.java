package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.CarryForwardBalanceResponse;
import com.example.employeeLeaveApplication.dto.CarryForwardEligibilityResponse;
import com.example.employeeLeaveApplication.service.CarryForwardService;
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

    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ResponseEntity<CarryForwardBalanceResponse> getBalance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        log.info("[CARRYFORWARD] Fetching balance: employee={}, year={}", employeeId, year);
        return ResponseEntity.ok(carryForwardService.getBalance(employeeId, year));
    }

    @GetMapping("/eligibility/{employeeId}")
    public ResponseEntity<CarryForwardEligibilityResponse> checkEligibility(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        log.info("[CARRYFORWARD] Checking eligibility: employee={}, year={}", employeeId, year);
        return ResponseEntity.ok(carryForwardService.checkEligibility(employeeId, year));
    }


    @GetMapping("/balances/{year}")
    public ResponseEntity<List<CarryForwardBalanceResponse>> getAllBalances(
            @PathVariable Integer year) {
        log.info("[CARRYFORWARD] Fetching all balances for year: {}", year);
        return ResponseEntity.ok(carryForwardService.getAllBalances(year));
    }
}