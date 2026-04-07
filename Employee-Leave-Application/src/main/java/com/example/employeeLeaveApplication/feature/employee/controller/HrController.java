package com.example.employeeLeaveApplication.feature.employee.controller;

import com.example.employeeLeaveApplication.feature.employee.dto.HrVerificationRequest;
import com.example.employeeLeaveApplication.feature.employee.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.feature.employee.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final EmployeeService employeeService;

    public HrController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * GET /api/hr/verifications/pending
     * Returns all employee profiles with status = PENDING.
     * Since hrId is hardcoded as 2, all pending profiles belong to this HR.
     */
    @GetMapping("/verifications/pending")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<EmployeePersonalDetails>> getPendingVerifications() {
        return ResponseEntity.ok(employeeService.getPendingVerifications());
    }

    /**
     * GET /api/hr/verifications/all
     * Returns all submitted profiles (any status) — HR history view.
     */
    @GetMapping("/verifications/all")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<EmployeePersonalDetails>> getAllVerifications() {
        return ResponseEntity.ok(employeeService.getAllVerifications());
    }

    /**
     * GET /api/hr/employees/{employeeId}/personal-details
     * HR views the full profile of a specific employee before deciding.
     */
    @GetMapping("/employees/{employeeId}/personal-details")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<EmployeePersonalDetails> getEmployeeDetails(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getPersonalDetails(employeeId));
    }

    /**
     * PUT /api/hr/verify/{employeeId}
     *
     * Verify:  { "status": "VERIFIED" }
     * Reject:  { "status": "REJECTED", "remarks": "Document is unclear." }
     *
     * VERIFIED → permanently locked, employee notified
     * REJECTED → unlocked so employee can resubmit, employee notified with remarks
     */
    @PutMapping("/verify/{employeeId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<EmployeePersonalDetails> verifyProfile(
            @PathVariable Long employeeId,
            @RequestBody HrVerificationRequest request) {
        return ResponseEntity.ok(
                employeeService.verifyPersonalDetails(employeeId, request));
    }
}