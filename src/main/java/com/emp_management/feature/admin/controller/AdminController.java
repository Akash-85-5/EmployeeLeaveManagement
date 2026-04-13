package com.emp_management.feature.admin.controller;

import com.emp_management.feature.admin.dto.CreateUserRequest;
import com.emp_management.feature.admin.dto.UpdateUserRequest;
import com.emp_management.feature.admin.service.AdminService;
import com.emp_management.infrastructure.scheduler.CarryForwardScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final CarryForwardScheduler carryForwardScheduler;

    public AdminController(AdminService adminService,
                           CarryForwardScheduler carryForwardScheduler) {
        this.adminService = adminService;
        this.carryForwardScheduler = carryForwardScheduler;
    }

    // ================= USER MANAGEMENT =================

    @PostMapping("/users/add")
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest request) {
        adminService.createUser(request);
        return ResponseEntity.ok("User created successfully");
    }

    @PutMapping("/users/update")
    public ResponseEntity<String> updateUser(@RequestBody UpdateUserRequest request){
        adminService.updateUser(request);
        return ResponseEntity.ok("Updated Sucessfully");
    }
    @PostMapping("/reset-password/{userId}")
    public ResponseEntity<String> resetPassword(@PathVariable String userId) {
        adminService.resetPassword(userId);
        return ResponseEntity.ok("Password reset successfully");
    }

    // ================= SCHEDULER MANUAL TRIGGER =================

    @PostMapping("/carryforward/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerCarryForward(
            @RequestParam int year) {

        // year = previous year (example: 2024 → generates 2025)
        carryForwardScheduler.triggerYearEndProcessing(year);

        return ResponseEntity.ok(
                "Carry-forward + allocation triggered for year " + (year + 1)
        );
    }
}