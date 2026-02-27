package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.CreateUserRequest;
import com.example.employeeLeaveApplication.dto.UserDropdownResponse;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.service.AdminService;
import com.example.employeeLeaveApplication.service.CarryForwardService;
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

    public AdminController(CarryForwardService carryForwardService,
                           AdminService adminService){
        this.carryForwardService=carryForwardService;
        this.adminService=adminService;
    }

    @PostMapping("/carry-forward")
    public ResponseEntity<String> processCarryForward(
            @RequestParam Integer fromYear) {

        log.info("[API] POST carry-forward: fromYear={}", fromYear);

        try {
            carryForwardService.processYearEndCarryForward(fromYear);
            return ResponseEntity.ok(
                    "Carry forward processed successfully for year " + fromYear);
        } catch (Exception e) {
            log.error("[API] Carry forward failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/carry-forward/{employeeId}")
    public ResponseEntity<String> processEmployeeCarryForward(
            @PathVariable Long employeeId,
            @RequestParam Integer fromYear) {

        log.info("[API] POST carry-forward: employee={}, fromYear={}", employeeId, fromYear);

        try {
            carryForwardService.processEmployeeCarryForward(employeeId, fromYear);
            return ResponseEntity.ok(
                    "Carry forward processed for employee " + employeeId);
        } catch (Exception e) {
            log.error("[API] Carry forward failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/users/add")
    @PreAuthorize("hasRole('ADMIN')")
    public void createUser(@RequestBody CreateUserRequest request) {
        adminService.createUser(request);
    }

    @PostMapping("/reset-password/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void resetPassword(@PathVariable Long userId) {
        adminService.resetPassword(userId);
    }

    @GetMapping("/eligible-managers")
    @PreAuthorize("hasRole('ADMIN')")
    public List< UserDropdownResponse> getEligibleManagers(
            @RequestParam Role role) {

        return adminService.getEligibleManagers(role);
    }
}
