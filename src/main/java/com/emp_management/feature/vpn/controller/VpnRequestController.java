package com.emp_management.feature.vpn.controller;

import com.emp_management.feature.vpn.dto.VpnRequestDtos.*;
import com.emp_management.feature.vpn.service.VpnRequestService;
import com.emp_management.shared.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/vpn")
public class VpnRequestController {

    private final VpnRequestService vpnRequestService;

    public VpnRequestController(VpnRequestService vpnRequestService) {
        this.vpnRequestService = vpnRequestService;
    }

    // Submit a new VPN request (Employee or Manager)
    @PostMapping("/apply")
    public ResponseEntity<VpnRequestResponse> apply(
            @Valid @RequestBody ApplyRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(vpnRequestService.applyForVpn(currentUserId, request));
    }

    // Applicant tracks their own requests — shows statusLabel at each stage
    @GetMapping("/my-requests")
    public ResponseEntity<List<VpnRequestResponse>> getMyRequests() {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(vpnRequestService.getMyRequests(currentUserId));
    }

    // Applicant views a single request with full status detail
    @GetMapping("/request/{requestId}")
    public ResponseEntity<VpnRequestResponse> getRequestById(
            @PathVariable Long requestId) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(vpnRequestService.getRequestById(requestId, currentUserId));
    }

    // Manager: view requests pending their action
    @GetMapping("/manager/pending")
    public ResponseEntity<List<VpnRequestResponse>> getPendingForManager() {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(vpnRequestService.getPendingForManager(currentUserId));
    }

    // Manager: view all requests from their team (all statuses)
    @GetMapping("/manager/all")
    public ResponseEntity<List<VpnRequestResponse>> getAllForManager() {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(vpnRequestService.getAllForManager(currentUserId));
    }

    // Manager approves or rejects a request
    @PutMapping("/manager/action/{requestId}")
    public ResponseEntity<VpnRequestResponse> managerAction(
            @PathVariable Long requestId,
            @RequestBody ActionRequest action) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(vpnRequestService.managerAction(requestId, currentUserId, action));
    }

    // Admin: view requests pending admin action
    // Multi-admin safe: once one admin acts, status changes and it leaves this list
    @GetMapping("/admin/pending")
    public ResponseEntity<List<VpnRequestResponse>> getPendingForAdmin() {
        return ResponseEntity.ok(vpnRequestService.getPendingForAdmin());
    }

    // Admin: view all VPN requests across the organisation
    @GetMapping("/admin/all")
    public ResponseEntity<List<VpnRequestResponse>> getAllRequests() {
        return ResponseEntity.ok(vpnRequestService.getAllRequests());
    }

    // Admin approves or rejects a request
    @PutMapping("/admin/action/{requestId}")
    public ResponseEntity<VpnRequestResponse> adminAction(
            @PathVariable Long requestId,
            @RequestBody ActionRequest action) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(vpnRequestService.adminAction(requestId, currentUserId, action));
    }
}