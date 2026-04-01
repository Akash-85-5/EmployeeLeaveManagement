package com.emp_management.feature.auth.controller;

import com.emp_management.feature.auth.dto.PasswordResetAdminResponse;
import com.emp_management.feature.auth.service.PasswordResetService;
import com.emp_management.shared.enums.ResetStatus;
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
    public void approve(@PathVariable Long requestId) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        resetService.approveResetByEmail(requestId, email);
    }

    @PostMapping("/reject/{requestId}")
    public void reject(@PathVariable Long requestId) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        resetService.rejectResetByEmail(requestId, email);
    }

    @GetMapping("/admin/list")
    public List<PasswordResetAdminResponse> getByStatus(
            @RequestParam ResetStatus status) {

        return resetService.getByStatus(status);
    }
}
