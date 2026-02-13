package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.service.EscalationService;
import com.example.employeeLeaveApplication.service.LeaveApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EscalationTestController {

    private final EscalationService escalationService;
    private final LeaveApprovalService leaveApprovalService;

    public EscalationTestController(EscalationService escalationService,
                                    LeaveApprovalService leaveApprovalService) {
        this.escalationService = escalationService;
        this.leaveApprovalService = leaveApprovalService;
    }

    @PostMapping("/escalation")
    public String triggerEscalation() {
        escalationService.escalatePendingLeaves();
        return "Escalation executed";
    }

    @GetMapping("/hr/escalated")
    public ResponseEntity<List<LeaveApplication>> getEscalatedLeavesForHr() {

        List<LeaveApplication> leaves =
                leaveApprovalService.getEscalatedLeavesForHr();

        return ResponseEntity.ok(leaves);

    }
}
