package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.LeaveDecisionRequest;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveApproval;
import com.example.employeeLeaveApplication.service.LeaveApprovalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-approvals")
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;

    public LeaveApprovalController(LeaveApprovalService leaveApprovalService) {
        this.leaveApprovalService = leaveApprovalService;
    }

    // ==================== GET PENDING LEAVES FOR MANAGER (WITH PAGINATION) - UPDATED ====================
    @GetMapping("/pending/{managerEmployeeId}")
    public Page<LeaveApplication> getPendingLeaves(
            @PathVariable Long managerEmployeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return leaveApprovalService.getPendingLeavesForManager(managerEmployeeId, pageable);
    }

    // ==================== MAKE DECISION (APPROVE/REJECT)  ====================
    @PatchMapping("/decision")
    public String decideLeave(@RequestBody LeaveDecisionRequest request) {
        leaveApprovalService.decideLeave(request);
        return "Leave " + request.getDecision();
    }

    // ==================== APPROVE LEAVE (ALTERNATIVE ENDPOINT) - NEW ====================
    @PatchMapping("/{leaveId}/approve")
    public String approveLeave(
            @PathVariable Long leaveId,
            @RequestParam Long managerId,
            @RequestParam(required = false) String comments
    ) {
        leaveApprovalService.approveLeave(leaveId, managerId, comments);
        return "Leave approved successfully";
    }

    // ==================== REJECT LEAVE (ALTERNATIVE ENDPOINT) - NEW ====================
    @PatchMapping("/{leaveId}/reject")
    public String rejectLeave(
            @PathVariable Long leaveId,
            @RequestParam Long managerId,
            @RequestParam(required = false) String comments
    ) {
        leaveApprovalService.rejectLeave(leaveId, managerId, comments);
        return "Leave rejected successfully";
    }

    // ==================== GET APPROVAL HISTORY FOR A LEAVE  ====================
    @GetMapping("/history/{leaveId}")
    public Page<LeaveApproval> getApprovalHistory(@PathVariable Long leaveId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return leaveApprovalService.getApprovalHistory(leaveId, pageable);
    }

    // ==================== GET MANAGER'S PAST DECISIONS  ====================
    @GetMapping("/my-decisions/{managerId}")
    public Page<LeaveApproval> getManagerDecisions(
            @PathVariable Long managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return leaveApprovalService.getManagerDecisions(managerId, pageable);
    }
}