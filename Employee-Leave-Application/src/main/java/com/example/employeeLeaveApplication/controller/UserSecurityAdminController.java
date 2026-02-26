package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.UserSecurityAdmin;
import com.example.employeeLeaveApplication.service.UserSecurityAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/security")
public class UserSecurityAdminController {

    private final UserSecurityAdminService service;

    public UserSecurityAdminController(UserSecurityAdminService service) {
        this.service = service;
    }

    @PostMapping("/save")
    public ResponseEntity<String> updateSecurity(
            @RequestBody UserSecurityAdmin security) {

        UserSecurityAdmin existing = service.findByUserId(security.getUserId());

        // ✅ FIXED: return 404 instead of 200 when not found
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        existing.setVpnEnabled(security.getVpnEnabled());
        existing.setBiometricRegistered(security.getBiometricRegistered());
        service.save(existing);

        return ResponseEntity.ok("Admin security details updated successfully");
    }
}