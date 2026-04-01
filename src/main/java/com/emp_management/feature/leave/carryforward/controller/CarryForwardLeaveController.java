package com.emp_management.feature.leave.carryforward.controller;


import com.emp_management.feature.leave.carryforward.dto.CarryForwardLeaveApplicationResponse;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardLeaveRequest;
import com.emp_management.feature.leave.carryforward.service.CarryForwardLeaveService;
import com.emp_management.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carryforward/leave")
public class CarryForwardLeaveController {

    private final CarryForwardLeaveService cfLeaveService;

    public CarryForwardLeaveController(CarryForwardLeaveService cfLeaveService) {
        this.cfLeaveService = cfLeaveService;
    }

    // ✅ APPLY (SELF ONLY)
    @PostMapping("/apply")
    @PreAuthorize("authentication.principal.id == #request.employeeId")
    public ResponseEntity<CarryForwardLeaveApplicationResponse> apply(
            @Valid @RequestBody CarryForwardLeaveRequest request,
            Authentication authentication) {

        Long loggedInUserId =
                ((CustomUserDetails) authentication.getPrincipal()).getId();

        if (!loggedInUserId.equals(request.getEmployeeId())) {
            throw new RuntimeException("You can only apply for yourself");
        }

        return ResponseEntity.ok(cfLeaveService.applyLeave(request));
    }

    // ✅ VIEW OWN
    @GetMapping("/my/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.id")
    public ResponseEntity<List<CarryForwardLeaveApplicationResponse>> getMyApplications(
            @PathVariable String employeeId) {

        return ResponseEntity.ok(cfLeaveService.getMyApplications(employeeId));
    }

    // ✅ VIEW SINGLE
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN') " +
            "or @cfLeaveOwnerGuard.isOwner(authentication, #id)")
    public ResponseEntity<CarryForwardLeaveApplicationResponse> getApplication(
            @PathVariable Long id) {

        return ResponseEntity.ok(cfLeaveService.getApplication(id));
    }

    // ✅ VIEW ALL
    @GetMapping("/all")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<List<CarryForwardLeaveApplicationResponse>> getAllApplications() {

        return ResponseEntity.ok(cfLeaveService.getAllApplications());
    }

    // ✅ APPROVE
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<CarryForwardLeaveApplicationResponse> approve(
            @PathVariable Long id,
            @RequestParam String approverId) {

        return ResponseEntity.ok(cfLeaveService.approveLeave(id, approverId));
    }

    // ✅ REJECT (FIXED NAME)
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<CarryForwardLeaveApplicationResponse> reject(
            @PathVariable Long id,
            @RequestParam String rejecterId,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = (body != null)
                ? body.getOrDefault("reason", "No reason provided")
                : "No reason provided";

        return ResponseEntity.ok(cfLeaveService.rejectLeave(id, rejecterId, reason));
    }

    // ✅ CANCEL
    @PutMapping("/{id}/cancel")
    @PreAuthorize("authentication.principal.id == @cfLeaveOwnerGuard.getOwnerId(#id)")
    public ResponseEntity<CarryForwardLeaveApplicationResponse> cancel(
            @PathVariable Long id,
            @RequestParam String employeeId) {

        return ResponseEntity.ok(cfLeaveService.cancelLeave(id, employeeId));
    }

//    // ✅ PENDING SELF
//    @GetMapping("/pending/my/{approverId}")
//    @PreAuthorize("#approverId == authentication.principal.id")
//    public ResponseEntity<List<CarryForwardLeaveApplicationResponse>> getPendingForApprover(
//            @PathVariable String approverId) {
//
//        return ResponseEntity.ok(cfLeaveService.getPendingForApprover(approverId));
//    }

    // ✅ ALL PENDING
    @GetMapping("/pending/all")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('TEAM_LEADER')")
    public ResponseEntity<List<CarryForwardLeaveApplicationResponse>> getAllPending() {

        return ResponseEntity.ok(cfLeaveService.getAllPending());
    }
}