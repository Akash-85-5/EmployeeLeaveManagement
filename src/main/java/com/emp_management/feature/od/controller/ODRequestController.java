package com.emp_management.feature.od.controller;

import com.emp_management.feature.od.dto.ODDecisionRequest;
import com.emp_management.feature.od.entity.ODApproval;
import com.emp_management.shared.enums.ODPurpose;
import com.emp_management.feature.od.entity.ODRequest;
import com.emp_management.feature.od.service.ODService;
import com.emp_management.shared.exceptions.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/od")
public class ODRequestController {

    private final ODService odService;

    public ODRequestController(ODService odService) {
        this.odService = odService;
    }

    // ── Apply OD ─────────────────────────────────────────────────

    @PostMapping("/apply")
    public ResponseEntity<ODRequest> applyOD(
            @RequestParam String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String purpose,
            @RequestParam String reason,
            @RequestParam(required = false) String clientLocation) {

        ODRequest request = new ODRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setReason(reason);
        request.setClientLocation(clientLocation);

        try {
            request.setPurpose(ODPurpose.valueOf(purpose.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid purpose: " + purpose);
        }

        return ResponseEntity.ok(odService.createOD(employeeId, request));
    }

    // ── Approve / Reject (separate endpoints, mirrors LeaveApprovalController) ──

    @PatchMapping("/{odId}/approve")
    public ResponseEntity<String> approveOD(
            @PathVariable Long odId,
            @RequestParam String approverId,
            @RequestParam(required = false) String comments) {
        odService.approveOD(odId, approverId, comments);
        return ResponseEntity.ok("OD approved successfully");
    }

    @PatchMapping("/{odId}/reject")
    public ResponseEntity<String> rejectOD(
            @PathVariable Long odId,
            @RequestParam String approverId,
            @RequestParam(required = false) String comments) {
        odService.rejectOD(odId, approverId, comments);
        return ResponseEntity.ok("OD rejected successfully");
    }

    // ── Unified decision endpoint (mirrors LeaveApprovalController.decideLeave) ──

    @PatchMapping("/decision")
    public ResponseEntity<String> decideOD(@RequestBody ODDecisionRequest request) {
        odService.decideOD(request);
        return ResponseEntity.ok("Decision recorded: " + request.getDecision());
    }

    // ── Cancel ──────────────────────────────────────────────────

    @PatchMapping("/{odId}/cancel")
    public ResponseEntity<String> cancelOD(
            @PathVariable Long odId,
            @RequestParam String requesterId) {
        odService.cancelOD(odId, requesterId);
        return ResponseEntity.ok("OD cancelled successfully");
    }

    // ── My OD requests ───────────────────────────────────────────

    @GetMapping("/my/{employeeId}")
    public ResponseEntity<Page<ODRequest>> getMyODRequests(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(odService.getMyODRequests(employeeId, pageable));
    }

    // ── Pending for a specific approver ─────────────────────────

    @GetMapping("/pending/approver/{approverId}")
    public ResponseEntity<List<ODRequest>> getPendingForApprover(
            @PathVariable String approverId) {
        return ResponseEntity.ok(odService.getPendingForApprover(approverId));
    }

    // ── All pending (HR / Admin view) ───────────────────────────

    @GetMapping("/pending/all")
    public ResponseEntity<List<ODRequest>> getAllPending() {
        return ResponseEntity.ok(odService.getAllPendingODs());
    }

    // ── Escalated ODs (HR view) ──────────────────────────────────

    @GetMapping("/escalated")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<ODRequest>> getEscalatedODs() {
        return ResponseEntity.ok(odService.getEscalatedODs());
    }

    // ── Approval history ─────────────────────────────────────────

    @GetMapping("/history/{odId}")
    public ResponseEntity<Page<ODApproval>> getApprovalHistory(
            @PathVariable Long odId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                odService.getApprovalHistory(odId, PageRequest.of(page, size)));
    }

    // ── My decisions (approver history) ──────────────────────────

    @GetMapping("/my-decisions/{approverId}")
    public ResponseEntity<Page<ODApproval>> getMyDecisions(
            @PathVariable String approverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                odService.getMyDecisions(approverId, PageRequest.of(page, size)));
    }
}