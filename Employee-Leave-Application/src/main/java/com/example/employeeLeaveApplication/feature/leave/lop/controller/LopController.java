package com.example.employeeLeaveApplication.feature.leave.lop.controller;

import com.example.employeeLeaveApplication.feature.leave.lop.dto.LopResponse;
import com.example.employeeLeaveApplication.feature.leave.lop.dto.LopSummaryResponse;
import com.example.employeeLeaveApplication.feature.leave.lop.service.LopService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/lop")
public class LopController {

    private final LopService lopService;

    public LopController(LopService lopService) {
        this.lopService = lopService;
    }

    // ── Monthly detail ────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','ADMIN','HR','CFO')")
    @GetMapping("/monthly")
    public ResponseEntity<List<LopResponse>> getMonthlyLop(
            @RequestParam Long callerId,
            @RequestParam int  year,
            @RequestParam int  month) {

        return ResponseEntity.ok(
                lopService.getMonthlyLop(callerId, year, month));
    }

    // ── Yearly detail ─────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','ADMIN','HR','CFO')")
    @GetMapping("/yearly")
    public ResponseEntity<List<LopResponse>> getYearlyLop(
            @RequestParam Long callerId,
            @RequestParam int  year) {

        return ResponseEntity.ok(
                lopService.getYearlyLop(callerId, year));
    }

    // ── HR & CFO: Aggregated monthly summary ──────────────────────

    @PreAuthorize("hasAnyRole('HR','CFO')")
    @GetMapping("/summary/monthly")
    public ResponseEntity<List<LopSummaryResponse>> getMonthlySummary(
            @RequestParam Long callerId,
            @RequestParam int  year,
            @RequestParam int  month) {

        return ResponseEntity.ok(
                lopService.getMonthlySummary(callerId, year, month));
    }

    // ── HR & CFO: Aggregated yearly summary ───────────────────────

    @PreAuthorize("hasAnyRole('HR','CFO')")
    @GetMapping("/summary/yearly")
    public ResponseEntity<List<LopSummaryResponse>> getYearlySummary(
            @RequestParam Long callerId,
            @RequestParam int  year) {

        return ResponseEntity.ok(
                lopService.getYearlySummary(callerId, year));
    }

    // ── Dashboard metric card: total LOP days this month ──────────

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','ADMIN')")
    @GetMapping("/my/total/monthly")
    public ResponseEntity<Double> getMyMonthlyTotal(
            @RequestParam Long employeeId,
            @RequestParam int  year,
            @RequestParam int  month) {

        return ResponseEntity.ok(
                lopService.getMyMonthlyLopTotal(employeeId, year, month));
    }

    // ── Dashboard metric card: total LOP days this year ───────────

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','ADMIN')")
    @GetMapping("/my/total/yearly")
    public ResponseEntity<Double> getMyYearlyTotal(
            @RequestParam Long employeeId,
            @RequestParam int  year) {

        return ResponseEntity.ok(
                lopService.getMyYearlyLopTotal(employeeId, year));
    }

    // ── LOP Reversal — HR only ────────────────────────────────────

    @PreAuthorize("hasRole('HR')")
    @PutMapping("/{lopId}/reverse")
    public ResponseEntity<LopResponse> reverseLop(
            @PathVariable Long   lopId,
            @RequestParam Long   reversedBy,
            @RequestParam String reason) {

        return ResponseEntity.ok(
                lopService.reverseLop(lopId, reversedBy, reason));
    }
}
