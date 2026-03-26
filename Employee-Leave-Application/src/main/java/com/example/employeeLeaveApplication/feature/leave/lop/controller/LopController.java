package com.example.employeeLeaveApplication.feature.leave.lop.controller;

import com.example.employeeLeaveApplication.feature.auth.entity.User;
import com.example.employeeLeaveApplication.feature.auth.repository.UserRepository;
import com.example.employeeLeaveApplication.feature.leave.lop.dto.LopResponse;
import com.example.employeeLeaveApplication.feature.leave.lop.dto.LopSummaryResponse;
import com.example.employeeLeaveApplication.feature.leave.lop.service.LopService;
import com.example.employeeLeaveApplication.shared.enums.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lop")
public class LopController {

    private final LopService lopService;
    private final UserRepository userRepository;

    public LopController(LopService lopService,
                         UserRepository userRepository) {
        this.lopService = lopService;
        this.userRepository = userRepository;
    }

    // ── Monthly detail ────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','ADMIN','HR','CFO')")
    @GetMapping("/monthly")
    public ResponseEntity<List<LopResponse>> getMonthlyLop(
            @RequestParam Long callerId,
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {

        Long resolvedId = resolveEmployeeId(callerId, authentication);
        return ResponseEntity.ok(
                lopService.getMonthlyLop(resolvedId, year, month));
    }

    // ── Yearly detail ─────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','ADMIN','HR','CFO')")
    @GetMapping("/yearly")
    public ResponseEntity<List<LopResponse>> getYearlyLop(
            @RequestParam Long callerId,
            @RequestParam int year,
            Authentication authentication) {

        Long resolvedId = resolveEmployeeId(callerId, authentication);
        return ResponseEntity.ok(
                lopService.getYearlyLop(resolvedId, year));
    }

    // ── HR & CFO: Aggregated monthly summary ──────────────────────

    @PreAuthorize("hasAnyRole('HR','CFO','ADMIN','MANAGER')")
    @GetMapping("/summary/monthly")
    public ResponseEntity<List<LopSummaryResponse>> getMonthlySummary(
            @RequestParam Long callerId,
            @RequestParam int year,
            @RequestParam int month) {

        return ResponseEntity.ok(
                lopService.getMonthlySummary(callerId, year, month));
    }

    // ── HR & CFO: Aggregated yearly summary ───────────────────────

    @PreAuthorize("hasAnyRole('HR','CFO','ADMIN','MANAGER')")
    @GetMapping("/summary/yearly")
    public ResponseEntity<List<LopSummaryResponse>> getYearlySummary(
            @RequestParam Long callerId,
            @RequestParam int year) {

        return ResponseEntity.ok(
                lopService.getYearlySummary(callerId, year));
    }

    // ── Dashboard metric card: total LOP days this month ──────────

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','ADMIN','HR','CFO')")
    @GetMapping("/my/total/monthly")
    public ResponseEntity<Double> getMyMonthlyTotal(
            @RequestParam(required = false) Long employeeId,
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {

        Long resolvedId = resolveEmployeeId(employeeId, authentication);
        return ResponseEntity.ok(
                lopService.getMyMonthlyLopTotal(resolvedId, year, month));
    }

    // ── Dashboard metric card: total LOP days this year ───────────

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','ADMIN','HR','CFO')")
    @GetMapping("/my/total/yearly")
    public ResponseEntity<Double> getMyYearlyTotal(
            @RequestParam(required = false) Long employeeId,
            @RequestParam int year,
            Authentication authentication) {

        Long resolvedId = resolveEmployeeId(employeeId, authentication);
        return ResponseEntity.ok(
                lopService.getMyYearlyLopTotal(resolvedId, year));
    }

    // ── LOP Reversal — HR only ────────────────────────────────────

    @PreAuthorize("hasRole('HR')")
    @PutMapping("/{lopId}/reverse")
    public ResponseEntity<LopResponse> reverseLop(
            @PathVariable Long lopId,
            @RequestParam Long reversedBy,
            @RequestParam String reason) {

        return ResponseEntity.ok(
                lopService.reverseLop(lopId, reversedBy, reason));
    }

    // ── Helper Method ─────────────────────────────────────────────

    private Long resolveEmployeeId(Long requestedId,
                                   Authentication authentication) {
        // Get logged in user
        String email = authentication.getName();
        User loggedInUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = loggedInUser.getRole();

        // ✅ MANAGER, HR, CFO, ADMIN → can view any employee
        if (role == Role.MANAGER ||
                role == Role.HR ||
                role == Role.ADMIN) {
            return requestedId != null ? requestedId : loggedInUser.getId();
        }

        // ✅ EMPLOYEE, TEAM_LEADER → only their own data!
        return loggedInUser.getId();
    }
}
