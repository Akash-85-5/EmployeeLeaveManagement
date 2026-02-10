package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.LeaveDecisionRequest;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.service.LeaveApprovalService;
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
}
