package com.emp_management.feature.od.controller;

import com.emp_management.feature.od.entity.ODRequest;
import com.emp_management.feature.od.service.ODService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OD (On-Duty) Request Controller
 *
 * Endpoints:
 * 1. POST   /api/od/request              - Create new OD request
 * 2. GET    /api/od/my/{empCode}         - Get all my OD requests
 * 3. GET    /api/od/pending/{empCode}    - Get pending approvals
 * 4. PUT    /api/od/approve/{odId}       - Approve OD
 * 5. PUT    /api/od/reject/{odId}        - Reject OD with reason
 * 6. PUT    /api/od/cancel/{odId}        - Cancel OD (only when PENDING)
 */
@RestController
@RequestMapping("/api/od")
@CrossOrigin(origins = "*")
public class ODRequestController {

    private final ODService odService;

    public ODRequestController(ODService odService) {
        this.odService = odService;
    }

    // =====================================================
    // CREATE OD REQUEST
    // =====================================================
    /**
     * Create a new OD request
     *
     * Request Body:
     * {
     *   "reason": "Official duty for client visit",
     *   "startDate": "2025-04-10",
     *   "endDate": "2025-04-12",
     *   "leaveType": { "id": 5 }
     * }
     *
     * Validations:
     * - Cannot apply for today or past dates
     * - Cannot have overlapping ODs
     * - Cannot apply OD on days with approved leave
     * - Employee must have a manager
     */
    @PostMapping("/request")
    public ResponseEntity<?> createOD(
            @RequestParam String empCode,
            @RequestBody ODRequest request) {

        try {
            ODRequest created = odService.createOD(empCode, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException | com.emp_management.shared.exceptions.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to create OD: " + e.getMessage()));
        }
    }

    // =====================================================
    // GET MY OD REQUESTS
    // =====================================================
    /**
     * Get all OD requests for the current employee
     *
     * URL: /api/od/my/{empCode}
     * Returns: List of all OD requests (any status)
     */
    @GetMapping("/my/{empCode}")
    public ResponseEntity<?> getMyODRequests(@PathVariable String empCode) {

        try {
            List<ODRequest> requests = odService.getMyODRequests(empCode);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse(e.getMessage()));
        }
    }

    // =====================================================
    // GET PENDING APPROVALS
    // =====================================================
    /**
     * Get all OD requests awaiting approval by the current manager
     *
     * URL: /api/od/pending/{empCode}
     * Returns: List of PENDING OD requests where empCode is current approver
     */
    @GetMapping("/pending/{empCode}")
    public ResponseEntity<?> getMyPendingApprovals(@PathVariable String empCode) {

        try {
            List<ODRequest> pending = odService.getMyPendingApprovals(empCode);
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse(e.getMessage()));
        }
    }

    // =====================================================
    // APPROVE OD REQUEST
    // =====================================================
    /**
     * Approve an OD request
     *
     * Two-Level Approval Flow:
     *
     * Level 1 Approval:
     * - Current approver (Level 1 Manager) approves
     * - Moves to Level 2 (Level 2 Manager)
     * - Notification sent to Level 2 Manager
     *
     * Level 2 Approval:
     * - Current approver (Level 2 Manager) approves
     * - OD status becomes APPROVED
     * - Notification sent to employee
     *
     * URL: /api/od/approve/{odId}?approverEmpCode=EMP001
     */
    @PutMapping("/approve/{odId}")
    public ResponseEntity<?> approveOD(
            @PathVariable Long odId,
            @RequestParam String approverEmpCode) {

        try {
            ODRequest updated = odService.approveOD(odId, approverEmpCode, null);
            return ResponseEntity.ok(updated);
        } catch (com.emp_management.shared.exceptions.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse(e.getMessage()));
        } catch (com.emp_management.shared.exceptions.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to approve OD: " + e.getMessage()));
        }
    }

    // =====================================================
    // REJECT OD REQUEST
    // =====================================================
    /**
     * Reject an OD request
     *
     * Business Rule:
     * - Can only reject PENDING OD requests
     * - Current approver must authorize rejection
     * - Reason is optional but recommended
     *
     * URL: /api/od/reject/{odId}?approverEmpCode=EMP001&reason=Insufficient%20justification
     */
    @PutMapping("/reject/{odId}")
    public ResponseEntity<?> rejectOD(
            @PathVariable Long odId,
            @RequestParam String approverEmpCode,
            @RequestParam(required = false) String reason) {

        try {
            ODRequest updated = odService.rejectOD(odId, approverEmpCode, reason);
            return ResponseEntity.ok(updated);
        } catch (com.emp_management.shared.exceptions.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse(e.getMessage()));
        } catch (com.emp_management.shared.exceptions.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to reject OD: " + e.getMessage()));
        }
    }

    // =====================================================
    // CANCEL OD REQUEST
    // =====================================================
    /**
     * Cancel an OD request
     *
     * Business Rules:
     * - Only employee can cancel their own OD
     * - Can only cancel when OD status is PENDING
     * - Cannot cancel APPROVED, REJECTED, or CANCELLED ODs
     *
     * URL: /api/od/cancel/{odId}?empCode=EMP001
     */
    @PutMapping("/cancel/{odId}")
    public ResponseEntity<?> cancelOD(
            @PathVariable Long odId,
            @RequestParam String empCode) {

        try {
            ODRequest updated = odService.cancelOD(odId, empCode);
            return ResponseEntity.ok(updated);
        } catch (com.emp_management.shared.exceptions.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse(e.getMessage()));
        } catch (com.emp_management.shared.exceptions.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to cancel OD: " + e.getMessage()));
        }
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    /**
     * Build error response object
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}