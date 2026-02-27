package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.service.LeaveAllocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
public class LeaveAllocationController {

    private final LeaveAllocationService leaveAllocationService;

    public LeaveAllocationController(LeaveAllocationService leaveAllocationService) {
        this.leaveAllocationService = leaveAllocationService;
    }

    @PostMapping
    public ResponseEntity<LeaveAllocation> createAllocation(
            @RequestBody LeaveAllocation leaveAllocation) {
        return ResponseEntity.ok(
                leaveAllocationService.createEmployeeAllocation(leaveAllocation));
    }

    // ✅ ADDED: POST /api/allocations/bulk/{employeeId} — bulk allocation
    @PostMapping("/bulk/{employeeId}")
    public ResponseEntity<String> createBulkAllocations(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {
        leaveAllocationService.createBulkAllocations(employeeId, year);
        return ResponseEntity.ok(
                "Bulk allocations created for employee " + employeeId
                        + " for year " + year);
    }

    // GET /api/allocations/{employeeId}
    @GetMapping("/{employeeId}")
    public ResponseEntity<List<LeaveAllocation>> getEmployeeAllocations(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(
                leaveAllocationService.getEmployeeAllocations(employeeId, year));
    }

    // PUT /api/allocations/record/{id} — update allocation
    @PutMapping("/record/{id}")
    public ResponseEntity<LeaveAllocation> updateAllocation(
            @PathVariable Long id,
            @RequestBody LeaveAllocation leaveAllocation) {
        return ResponseEntity.ok(
                leaveAllocationService.updateAllocation(id, leaveAllocation));
    }

    // ✅ ADDED: DELETE /api/allocations/record/{id} — delete allocation
    @DeleteMapping("/record/{id}")
    public ResponseEntity<String> deleteAllocation(@PathVariable Long id) {
        leaveAllocationService.deleteAllocation(id);
        return ResponseEntity.ok("Allocation deleted successfully");
    }

    // ✅ ADDED: GET /api/allocations/year/{year} — all allocations for a year (HR view)
    @GetMapping("/year/{year}")
    public ResponseEntity<List<LeaveAllocation>> getAllocationsByYear(
            @PathVariable Integer year) {
        return ResponseEntity.ok(
                leaveAllocationService.getAllocationsByYear(year));
    }
}