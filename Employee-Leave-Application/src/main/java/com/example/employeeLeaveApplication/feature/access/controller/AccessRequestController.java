package com.example.employeeLeaveApplication.feature.access.controller;

import com.example.employeeLeaveApplication.feature.access.dto.*;
import com.example.employeeLeaveApplication.feature.access.service.AccessRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Access Requests (VPN & Biometric)
 *
 * Endpoints:
 * - Employee: submit, view their requests
 * - Manager: view pending, approve/reject
 * - Admin: view pending, approve/reject
 */
@RestController
@RequestMapping("/api/access-requests")
@Slf4j
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    public AccessRequestController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EMPLOYEE ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Employee submits a new access request
     * POST /api/access-requests
     *
     * Request body:
     * {
     *   "accessType": "VPN",  // or "BIOMETRIC"
     *   "reason": "Required for project work"
     * }
     */
    @PostMapping("/apply/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TEAM_LEADER')")
    public ResponseEntity<AccessRequestResponseDto> submitAccessRequest(
            @RequestBody SubmitAccessRequestDto request,
            @PathVariable Long id) {

        log.info("Employee {} submitted {} access request",
                id, request.getAccessType());

        AccessRequestResponseDto response =
                accessRequestService.submitAccessRequest(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all access requests for logged-in employee
     * GET /api/access-requests/my-requests
     */
    @GetMapping("/my-requests/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<AccessRequestResponseDto>> getMyAccessRequests(
            @PathVariable Long id) {

        log.info("Employee {} viewing their access requests", id);

        List<AccessRequestResponseDto> requests =
                accessRequestService.getMyAccessRequests(id);

        return ResponseEntity.ok(requests);
    }


    // ═══════════════════════════════════════════════════════════════════════
    // MANAGER ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Manager views all pending access requests
     * GET /api/access-requests/manager/pending/id
     */
    @GetMapping("/manager/pending/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AccessRequestForManagerDto>> getPendingRequestsForManager(
            @PathVariable Long id) {

        log.info("Manager {} viewing pending access requests", id);

        List<AccessRequestForManagerDto> requests =
                accessRequestService.getPendingRequestsForManager(id);

        return ResponseEntity.ok(requests);
    }

    /**
     * Manager approves or rejects request
     * PATCH /api/access-requests/{requestId}/manager-decision
     *
     * Request body:
     * {
     *   "decision": "APPROVED",  // or "REJECTED"
     *   "remarks": "Approved for project X"  // optional if approved, required if rejected
     * }
     */

    @PatchMapping("/{requestId}/manager-decision")
    @PreAuthorize("hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<AccessRequestResponseDto> managerDecision(
            @PathVariable Long requestId,
            @RequestBody ManagerAccessDecisionDto decision) {
        log.info("Manager {} made decision on access request {}: {}",
                decision.getReportingId(), requestId, decision.getDecision());

        AccessRequestResponseDto response =
                accessRequestService.managerDecision(requestId, decision);

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Admin views all pending approvals (manager-approved requests)
     * GET /api/access-requests/admin/pending-approvals
     */
    @GetMapping("/admin/pending-approvals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccessRequestForAdminDto>> getPendingAdminApprovals() {

        log.info("Admin viewing pending access request approvals");

        List<AccessRequestForAdminDto> requests =
                accessRequestService.getPendingAdminApprovals();

        return ResponseEntity.ok(requests);
    }

    /**
     * Admin approves or rejects request (and grants/denies actual access)
     * PATCH /api/access-requests/{requestId}/admin-decision
     *
     * Request body:
     * {
     *   "decision": "APPROVED",  // or "REJECTED"
     *   "remarks": "Access granted"  // optional
     * }
     */
    @PatchMapping("/{requestId}/admin-decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccessRequestResponseDto> adminDecision(
            @PathVariable Long requestId,
            @RequestBody AdminAccessDecisionDto decision) {

        log.info("Admin made decision on access request {}: {}",
                requestId, decision.getDecision());

        AccessRequestResponseDto response =
                accessRequestService.adminDecision(requestId, decision);

        return ResponseEntity.ok(response);
    }
}