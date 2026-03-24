package com.example.employeeLeaveApplication.feature.admin.controller;

import com.example.employeeLeaveApplication.feature.admin.dto.CreateUserRequest;
import com.example.employeeLeaveApplication.feature.admin.dto.UserDropdownResponse;
import com.example.employeeLeaveApplication.feature.employee.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.feature.payroll.dto.PfUpdateRequest;
import com.example.employeeLeaveApplication.shared.enums.EmployeeType;
import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.feature.access.service.AccessRequestService;
import com.example.employeeLeaveApplication.feature.admin.service.AdminService;
import com.example.employeeLeaveApplication.feature.leave.carryforward.service.CarryForwardService;
import com.example.employeeLeaveApplication.feature.employee.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final CarryForwardService carryForwardService;
    private final EmployeeService employeeService;
    private final AccessRequestService accessRequestService;

    public AdminController(CarryForwardService carryForwardService,
                           AdminService adminService,
                           EmployeeService employeeService,
                           AccessRequestService accessRequestService) {
        this.carryForwardService = carryForwardService;
        this.adminService = adminService;
        this.employeeService = employeeService;
        this.accessRequestService = accessRequestService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXISTING ENDPOINTS (UNCHANGED)
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/carry-forward")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> processCarryForward(@RequestParam Integer fromYear) {
        try {
            carryForwardService.processYearEndCarryForward(fromYear);
            return ResponseEntity.ok("Carry forward processed successfully for year " + fromYear);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/carry-forward/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> processEmployeeCarryForward(
            @PathVariable Long employeeId, @RequestParam Integer fromYear) {
        try {
            carryForwardService.processEmployeeCarryForward(employeeId, fromYear);
            return ResponseEntity.ok("Carry forward processed for employee " + employeeId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Carry forward failed: " + e.getMessage());
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

    @PutMapping("/employees/{employeeId}/pf-number")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> updatePfNumber(
            @PathVariable Long employeeId,
            @RequestBody PfUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updatePfNumber(employeeId, request));
    }

    @PutMapping(value = "/employees/{employeeId}/personal-details",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> adminUpdatePersonalDetails(
            @PathVariable Long employeeId,
            @RequestParam EmployeeType employeeType,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "aadhaarCard", required = false) MultipartFile aadhaarCard,
            @RequestPart(value = "doc1", required = false) MultipartFile doc1,
            @RequestPart(value = "doc2", required = false) MultipartFile doc2) {

        return ResponseEntity.ok(
                employeeService.adminUpdatePersonalDetails(
                        employeeId, dataJson, aadhaarCard, doc1, doc2, employeeType));
    }

    @GetMapping("/employees/{employeeId}/personal-details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> getPersonalDetails(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getPersonalDetails(employeeId));
    }
}