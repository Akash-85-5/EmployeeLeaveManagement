package com.example.employeeLeaveApplication.feature.leave.annual.controller;

import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.feature.leave.annual.service.LeaveAllocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // ✅ NEW IMPORT

@RestController
@RequestMapping("/api/allocations")
public class LeaveAllocationController {

    // ===================== EXISTING =====================
    private final LeaveAllocationService leaveAllocationService;

    // ===================== EXISTING =====================
    public LeaveAllocationController(
            LeaveAllocationService leaveAllocationService) {
        this.leaveAllocationService = leaveAllocationService;
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added @PreAuthorize — was missing before
    // URL: POST /api/allocations
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // ✅ ADDED
    public ResponseEntity<LeaveAllocation> createAllocation(
            @RequestBody LeaveAllocation leaveAllocation) {
        return ResponseEntity.ok(
                leaveAllocationService
                        .createEmployeeAllocation(leaveAllocation));
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added @PreAuthorize — was missing before
    // URL: POST /api/allocations/bulk/{employeeId}?year=2026
    @PostMapping("/bulk/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')") // ✅ ADDED
    public ResponseEntity<String> createBulkAllocations(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        leaveAllocationService.createBulkAllocations(employeeId, year);
        return ResponseEntity.ok(
                "Bulk allocations created for employee "
                        + employeeId + " for year " + year);
    }

    // ✅ NEW ENDPOINT
    // Reason: Admin allocates ALL employees at once
    // URL: POST /api/allocations/bulk-all?year=2026
    @PostMapping("/bulk-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createBulkAllocationsForAll(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(
                leaveAllocationService
                        .createBulkAllocationsForAllEmployees(year));
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added @PreAuthorize — was missing before
    // Employee sees own, HR/Admin/Manager see any
    // URL: GET /api/allocations/{employeeId}?year=2026
    @GetMapping("/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER')") // ✅ ADDED
    public ResponseEntity<List<LeaveAllocation>> getEmployeeAllocations(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(
                leaveAllocationService
                        .getEmployeeAllocations(employeeId, year));
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added @PreAuthorize — was missing before
    // URL: PUT /api/allocations/record/{id}
    @PutMapping("/record/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ✅ ADDED
    public ResponseEntity<LeaveAllocation> updateAllocation(
            @PathVariable Long id,
            @RequestBody LeaveAllocation leaveAllocation) {
        return ResponseEntity.ok(
                leaveAllocationService
                        .updateAllocation(id, leaveAllocation));
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added @PreAuthorize — was missing before
    // URL: DELETE /api/allocations/record/{id}
    @DeleteMapping("/record/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ✅ ADDED
    public ResponseEntity<String> deleteAllocation(
            @PathVariable Long id) {
        leaveAllocationService.deleteAllocation(id);
        return ResponseEntity.ok("Allocation deleted successfully");
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added @PreAuthorize — was missing before
    // URL: GET /api/allocations/year/{year}
    @GetMapping("/year/{year}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER')") // ✅ ADDED
    public ResponseEntity<List<LeaveAllocation>> getAllocationsByYear(
            @PathVariable Integer year) {
        return ResponseEntity.ok(
                leaveAllocationService.getAllocationsByYear(year));
    }
}