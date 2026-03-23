package com.example.employeeLeaveApplication.feature.auth.controller;

import com.example.employeeLeaveApplication.feature.auth.dto.PasswordResetAdminResponse;
import com.example.employeeLeaveApplication.shared.enums.ResetStatus;
import com.example.employeeLeaveApplication.feature.auth.service.PasswordResetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {

    private final PasswordResetService resetService;

    public PasswordResetController(PasswordResetService resetService) {
        this.resetService = resetService;
    }

    // 🔹 USER
    @PostMapping("/request")
    public void requestReset(@RequestParam String email) {
        resetService.requestReset(email);
    }

    @PostMapping("/approve/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void approve(@PathVariable Long requestId) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        resetService.approveResetByEmail(requestId, email);
    }

    @PostMapping("/reject/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void reject(@PathVariable Long requestId) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        resetService.rejectResetByEmail(requestId, email);
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PasswordResetAdminResponse> getByStatus(
            @RequestParam ResetStatus status) {

        return resetService.getByStatus(status);
    }
}
