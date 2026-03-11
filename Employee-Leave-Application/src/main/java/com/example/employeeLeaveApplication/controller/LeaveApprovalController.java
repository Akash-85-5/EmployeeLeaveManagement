// src/main/java/com/example/employeeLeaveApplication/controller/LeaveApprovalController.java
package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.BulkLeaveDecisionRequest;
import com.example.employeeLeaveApplication.dto.LeaveDecisionRequest;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveApproval;
import com.example.employeeLeaveApplication.service.LeaveApprovalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-approvals")
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;

    public LeaveApprovalController(LeaveApprovalService leaveApprovalService) {
        this.leaveApprovalService = leaveApprovalService;
    }

    // ── Team Leader: pending leaves waiting for TL approval ─────────────────

    @GetMapping("/pending/team-leader/{teamLeaderId}")
    @PreAuthorize("hasRole('TEAM_LEADER') and #teamLeaderId == authentication.principal.user.id")
    public ResponseEntity<Page<LeaveApplication>> getPendingLeavesForTeamLeader(
            @PathVariable Long teamLeaderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(leaveApprovalService.getPendingLeavesForTeamLeader(teamLeaderId, pageable));
    }

    // ── Manager: pending leaves waiting for Manager approval ─────────────────

    @GetMapping("/pending/manager/{managerId}")
    @PreAuthorize("hasRole('MANAGER') and #managerId == authentication.principal.user.id")
    public ResponseEntity<Page<LeaveApplication>> getPendingLeavesForManager(
            @PathVariable Long managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(leaveApprovalService.getPendingLeavesForManager(managerId, pageable));
    }

    // ── HR: pending leaves waiting for HR approval ───────────────────────────

    @GetMapping("/pending/hr")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Page<LeaveApplication>> getPendingLeavesForHr(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(leaveApprovalService.getPendingLeavesForHr(pageable));
    }

    // ── Universal decision endpoint (TL / Manager / HR all use this) ─────────

    @PatchMapping("/decision")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<String> decideLeave(@RequestBody LeaveDecisionRequest request) {
        leaveApprovalService.decideLeave(request);
        return ResponseEntity.ok("Decision recorded: " + request.getDecision());
    }

    // ── Convenience approve / reject endpoints ───────────────────────────────

    @PatchMapping("/{leaveId}/approve")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<String> approveLeave(
            @PathVariable Long leaveId,
            @RequestParam Long approverId,
            @RequestParam(required = false) String comments) {
        leaveApprovalService.approveLeave(leaveId, approverId, comments);
        return ResponseEntity.ok("Leave approved at current level successfully");
    }

    @PatchMapping("/{leaveId}/reject")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<String> rejectLeave(
            @PathVariable Long leaveId,
            @RequestParam Long approverId,
            @RequestParam(required = false) String comments) {
        leaveApprovalService.rejectLeave(leaveId, approverId, comments);
        return ResponseEntity.ok("Leave rejected successfully");
    }

    // ── Bulk decisions ────────────────────────────────────────────────────────

    @PostMapping("/manager/bulk-decision")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> managerBulkDecision(@RequestBody BulkLeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveApprovalService.bulkDecision(request, false));
    }

    @PostMapping("/hr/bulk-decision")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<String> hrBulkDecision(@RequestBody BulkLeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveApprovalService.bulkDecision(request, true));
    }

    // ── History & audit ───────────────────────────────────────────────────────

    @GetMapping("/history/{leaveId}")
    public ResponseEntity<Page<LeaveApproval>> getApprovalHistory(
            @PathVariable Long leaveId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(leaveApprovalService.getApprovalHistory(leaveId, PageRequest.of(page, size)));
    }

    @GetMapping("/my-decisions/{approverId}")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<Page<LeaveApproval>> getMyDecisions(
            @PathVariable Long approverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(leaveApprovalService.getManagerDecisions(approverId, PageRequest.of(page, size)));
    }

    @GetMapping("/hr/escalated")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<?> getEscalatedLeavesForHr() {
        return ResponseEntity.ok(leaveApprovalService.getEscalatedLeavesForHr());
    }
}