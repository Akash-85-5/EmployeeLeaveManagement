package com.emp_management.feature.leave.annual.controller;

import com.emp_management.feature.leave.annual.dto.LeaveApplicationWithAttachmentsDto;
import com.emp_management.feature.leave.annual.dto.LeaveDecisionRequest;
import com.emp_management.feature.leave.annual.entity.LeaveApproval;
import com.emp_management.feature.leave.annual.service.LeaveApprovalService;
import com.emp_management.shared.exceptions.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/leave-approvals")
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;

    public LeaveApprovalController(LeaveApprovalService leaveApprovalService) {
        this.leaveApprovalService = leaveApprovalService;
    }

    // ── Pending per role (WITH ATTACHMENTS) ─────────────────────

//    @GetMapping("/pending/team-leader/{teamLeaderId}")
//    @PreAuthorize("hasRole('TEAM_LEADER') and #teamLeaderId == authentication.principal.user.id")
//    public ResponseEntity<Page<LeaveApplicationWithAttachmentsDto>> getPendingLeavesForTeamLeader(
//            @PathVariable Long teamLeaderId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        Page<LeaveApplicationWithAttachmentsDto> result =
//                leaveApprovalService.getPendingLeavesForTeamLeaderWithAttachments(teamLeaderId, pageable);
//        return ResponseEntity.ok(result);
//    }

    @GetMapping("/pending/manager/{managerId}")
    public ResponseEntity<Page<LeaveApplicationWithAttachmentsDto>> getPendingLeavesForManager(
            @PathVariable String managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveApplicationWithAttachmentsDto> result =
                leaveApprovalService.getPendingLeavesForManagerWithAttachments(managerId, pageable);
        return ResponseEntity.ok(result);
    }

//    @GetMapping("/pending/hr")
//    @PreAuthorize("hasRole('HR')")
//    public ResponseEntity<Page<LeaveApplicationWithAttachmentsDto>> getPendingLeavesForHr(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        Page<LeaveApplicationWithAttachmentsDto> result =
//                leaveApprovalService.getPendingLeavesForHrWithAttachments(pageable);
//        return ResponseEntity.ok(result);
//    }

    // ── Get single leave with attachments ────────────────────────

//    @GetMapping("/{leaveId}/with-attachments")
//    public ResponseEntity<LeaveApplicationWithAttachmentsDto> getLeaveWithAttachments(
//            @PathVariable Long leaveId) {
//        LeaveApplicationWithAttachmentsDto dto =
//                leaveApprovalService.getLeaveApplicationWithAttachments(leaveId);
//        return ResponseEntity.ok(dto);
//    }

    // ── Core decision ─────────────────────────────────────────────

    @PatchMapping("/decision")
    public ResponseEntity<String> decideLeave(@Valid @RequestBody LeaveDecisionRequest request) {
        leaveApprovalService.decideLeave(request);
        return ResponseEntity.ok("Decision recorded: " + request.getDecision());
    }

    @PatchMapping("/{leaveId}/approve")
    public ResponseEntity<String> approveLeave(
            @PathVariable Long leaveId,
            @RequestParam String approverId,
            @RequestParam String comments) { // ❌ make required
        if (comments == null || comments.isBlank()) {
            throw new BadRequestException("Remarks are required for approval");
        }

        leaveApprovalService.approveLeave(leaveId, approverId, comments);
        return ResponseEntity.ok("Leave approved successfully");
    }

    @PatchMapping("/{leaveId}/reject")
    public ResponseEntity<String> rejectLeave(
            @PathVariable Long leaveId,
            @RequestParam String approverId,
            @RequestParam String comments) { // ❌ make required
        if (comments == null || comments.isBlank()) {
            throw new BadRequestException("Remarks are required for rejection");
        }

        leaveApprovalService.rejectLeave(leaveId, approverId, comments);
        return ResponseEntity.ok("Leave rejected successfully");
    }

    // ── Bulk decisions ────────────────────────────────────────────

//    @PostMapping("/manager/bulk-decision")
//    @PreAuthorize("hasRole('MANAGER')")
//    public ResponseEntity<String> managerBulkDecision(
//            @RequestBody BulkLeaveDecisionRequest request) {
//        return ResponseEntity.ok(leaveApprovalService.bulkDecision(request, false));
//    }

//    @PostMapping("/hr/bulk-decision")
//    @PreAuthorize("hasRole('HR')")
//    public ResponseEntity<String> hrBulkDecision(
//            @RequestBody BulkLeaveDecisionRequest request) {
//        return ResponseEntity.ok(leaveApprovalService.bulkDecision(request, true));
//    }

    // ── History & audit ───────────────────────────────────────────

    @GetMapping("/history/{leaveId}")
    public ResponseEntity<Page<LeaveApproval>> getApprovalHistory(
            @PathVariable Long leaveId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                leaveApprovalService.getApprovalHistory(leaveId, PageRequest.of(page, size)));
    }

    @GetMapping("/my-decisions/{approverId}")
    public ResponseEntity<Page<LeaveApproval>> getMyDecisions(
            @PathVariable String approverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                leaveApprovalService.getManagerDecisions(approverId, PageRequest.of(page, size)));
    }

    @GetMapping("/hr/escalated")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<?> getEscalatedLeavesForHr() {
        return ResponseEntity.ok(leaveApprovalService.getEscalatedLeavesForHr());
    }
}