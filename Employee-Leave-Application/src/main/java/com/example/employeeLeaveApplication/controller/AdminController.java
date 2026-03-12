package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.CreateUserRequest;
import com.example.employeeLeaveApplication.dto.PersonalDetailsRequest;
import com.example.employeeLeaveApplication.dto.UserDropdownResponse;
import com.example.employeeLeaveApplication.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.service.AdminService;
import com.example.employeeLeaveApplication.service.CarryForwardService;
import com.example.employeeLeaveApplication.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final CarryForwardService carryForwardService;
    private final EmployeeService employeeService;

    public AdminController(CarryForwardService carryForwardService,
                           AdminService adminService,
                           EmployeeService employeeService) {
        this.carryForwardService = carryForwardService;
        this.adminService = adminService;
        this.employeeService = employeeService;
    }

    // ── Existing endpoints (unchanged) ───────────────────────────

    @PostMapping("/carry-forward")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> processCarryForward(@RequestParam Integer fromYear) {
        try {
            carryForwardService.processYearEndCarryForward(fromYear);
            return ResponseEntity.ok(
                    "Carry forward processed successfully for year " + fromYear);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/carry-forward/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> processEmployeeCarryForward(
            @PathVariable Long employeeId,
            @RequestParam Integer fromYear) {
        try {
            carryForwardService.processEmployeeCarryForward(employeeId, fromYear);
            return ResponseEntity.ok(
                    "Carry forward processed for employee " + employeeId);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/users/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest request) {
        adminService.createUser(request);
        return ResponseEntity.ok("User created successfully");
    }

    @PostMapping("/reset-password/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetPassword(@PathVariable Long userId) {
        adminService.resetPassword(userId);
        return ResponseEntity.ok("Password reset successfully");
    }

    @GetMapping("/eligible-managers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDropdownResponse> getEligibleManagers(@RequestParam Role role) {
        return adminService.getEligibleManagers(role);
    }

    // ── Admin/HR edits personal details (NO LOCK CHECK) ──────────
    // Uses adminUpdatePersonalDetails → bypasses lock
    @PostMapping("/employees/{employeeId}/personal-details")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<EmployeePersonalDetails> addPersonalDetails(
            @PathVariable Long employeeId,
            @RequestBody PersonalDetailsRequest request) {
        return ResponseEntity.ok(
                employeeService.adminUpdatePersonalDetails(employeeId, request));
    }

    @PutMapping("/employees/{employeeId}/personal-details")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<EmployeePersonalDetails> updatePersonalDetails(
            @PathVariable Long employeeId,
            @RequestBody PersonalDetailsRequest request) {
        return ResponseEntity.ok(
                employeeService.adminUpdatePersonalDetails(employeeId, request));
    }
}