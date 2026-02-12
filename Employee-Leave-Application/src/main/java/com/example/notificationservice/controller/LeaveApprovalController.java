package com.example.notificationservice.controller;

import com.example.notificationservice.dto.BulkLeaveDecisionRequest;
import com.example.notificationservice.dto.LeaveDecisionRequest;
import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.service.LeaveApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-approvals")

public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;

    public LeaveApprovalController(LeaveApprovalService leaveApprovalService){
        this.leaveApprovalService=leaveApprovalService;
    }

    @GetMapping("/pending/{managerEmployeeId}")
    public List<LeaveApplication> getPendingLeaves(
            @PathVariable Long managerEmployeeId) {
        return leaveApprovalService.getPendingLeavesForManager(managerEmployeeId);
    }

    @PostMapping("/decision")
    public String decideLeave(@RequestBody LeaveDecisionRequest request) {
        leaveApprovalService.decideLeave(request);
        return "Leave " + request.getDecision();
    }
    @PostMapping("/hr/decision")
    public ResponseEntity<String> hrDecision(@RequestBody LeaveDecisionRequest request) {

        return ResponseEntity.ok(
                leaveApprovalService.hrDecision(
                        request.getLeaveId(),
                        request.getDecision(),
                        request.getManagerId()
                )
        );
    }
    @PostMapping("/manager/bulk-decision")
    public ResponseEntity<String> managerBulkDecision(
            @RequestBody BulkLeaveDecisionRequest request) {

        return ResponseEntity.ok(
                leaveApprovalService.bulkDecision(request, false)
        );
    }
    @PostMapping("/hr/bulk-decision")
    public ResponseEntity<String> hrBulkDecision(
            @RequestBody BulkLeaveDecisionRequest request) {

        return ResponseEntity.ok(
                leaveApprovalService.bulkDecision(request, true)
        );
    } }

