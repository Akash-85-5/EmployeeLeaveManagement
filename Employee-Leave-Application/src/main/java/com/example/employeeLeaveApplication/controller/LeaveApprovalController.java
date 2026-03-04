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

    @GetMapping("/pending/{managerId}")
    @PreAuthorize("#managerId == authentication.principal.user.id")
    public ResponseEntity<Page<LeaveApplication>> getPendingLeaves(
            @PathVariable Long managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                leaveApprovalService.getPendingLeavesForManager(managerId, pageable));
    }

    // ✅ FIXED: now returns ResponseEntity<String>
    @PatchMapping("/decision")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<String> decideLeave(@RequestBody LeaveDecisionRequest request) {
        leaveApprovalService.decideLeave(request);
        return ResponseEntity.ok("Leave decision recorded: " + request.getDecision());
    }

    // ✅ FIXED: now returns ResponseEntity<String>
    @PatchMapping("/{leaveId}/approve")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<String> approveLeave(
            @PathVariable Long leaveId,
            @RequestParam Long managerId,
            @RequestParam(required = false) String comments) {
        leaveApprovalService.approveLeave(leaveId, managerId, comments);
        return ResponseEntity.ok("Leave approved successfully");
    }

    @PatchMapping("/{leaveId}/reject")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<String> rejectLeave(
            @PathVariable Long leaveId,
            @RequestParam Long managerId,
            @RequestParam(required = false) String comments) {
        leaveApprovalService.rejectLeave(leaveId, managerId, comments);
        return ResponseEntity.ok("Leave rejected successfully");
    }

    @GetMapping("/history/{leaveId}")
    public ResponseEntity<Page<LeaveApproval>> getApprovalHistory(
            @PathVariable Long leaveId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(leaveApprovalService.getApprovalHistory(leaveId, pageable));
    }

    @GetMapping("/my-decisions/{managerId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<Page<LeaveApproval>> getManagerDecisions(
            @PathVariable Long managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(leaveApprovalService.getManagerDecisions(managerId, pageable));
    }

    @PostMapping("/hr/decision")
    public ResponseEntity<String> hrDecision(@RequestBody LeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveApprovalService.hrDecision(
                request.getLeaveId(), request.getDecision(), request.getManagerId()));
    }

    @PostMapping("/manager/bulk-decision")
    public ResponseEntity<String> managerBulkDecision(
            @RequestBody BulkLeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveApprovalService.bulkDecision(request, false));
    }

    @PostMapping("/hr/bulk-decision")
    public ResponseEntity<String> hrBulkDecision(
            @RequestBody BulkLeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveApprovalService.bulkDecision(request, true));
    }

    @GetMapping("/hr/escalated")
    public ResponseEntity<?> getEscalatedLeavesForHr() {
        return ResponseEntity.ok(leaveApprovalService.getEscalatedLeavesForHr());
    }
}