package com.example.employeeLeaveApplication.feature.od.controller;

import com.example.employeeLeaveApplication.feature.od.entity.ODRequest;
import com.example.employeeLeaveApplication.feature.od.service.ODService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/od")
@CrossOrigin(origins = "*")
public class ODRequestController {

    private final ODService odService;

    public ODRequestController(ODService odService) {
        this.odService = odService;
    }

    @PostMapping("/request")
    public ResponseEntity<ODRequest> createOD(@RequestParam Long employeeId,
                                              @RequestBody ODRequest request) {
        return ResponseEntity.ok(odService.createOD(employeeId, request));
    }

    @PutMapping("/approve/{odId}")
    public ResponseEntity<ODRequest> approveOD(@PathVariable Long odId,
                                               @RequestParam Long approverId) {
        return ResponseEntity.ok(odService.approveOD(odId, approverId));
    }

    @PutMapping("/reject/{odId}")
    public ResponseEntity<ODRequest> rejectOD(@PathVariable Long odId,
                                              @RequestParam Long approverId,
                                              @RequestParam String reason) {
        return ResponseEntity.ok(odService.rejectOD(odId, approverId, reason));
    }

    @PutMapping("/cancel/{odId}")
    public ResponseEntity<ODRequest> cancelOD(@PathVariable Long odId,
                                              @RequestParam Long userId) {
        return ResponseEntity.ok(odService.cancelOD(odId, userId));
    }

    @GetMapping("/my/{employeeId}")
    public ResponseEntity<List<ODRequest>> getMyODRequests(@PathVariable Long employeeId) {
        return ResponseEntity.ok(odService.getMyODRequests(employeeId));
    }

    /** Manager's inbox — all ODs currently waiting on them */
    @GetMapping("/pending/approver/{managerId}")
    public ResponseEntity<List<ODRequest>> getPendingForApprover(@PathVariable Long managerId) {
        return ResponseEntity.ok(odService.getPendingForApprover(managerId));
    }

    /** HR/Admin view — all pending ODs across the system */
    @GetMapping("/pending/all")
    public ResponseEntity<List<ODRequest>> getAllPending() {
        return ResponseEntity.ok(odService.getAllPendingODs());
    }
}