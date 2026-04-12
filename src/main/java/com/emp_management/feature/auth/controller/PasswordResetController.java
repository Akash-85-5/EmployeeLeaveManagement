package com.emp_management.feature.auth.controller;

import com.emp_management.feature.auth.dto.OtpRequest;
import com.emp_management.feature.auth.dto.OtpVerifyRequest;
import com.emp_management.feature.auth.dto.PasswordResetAdminResponse;
import com.emp_management.feature.auth.service.OtpService;
import com.emp_management.feature.auth.service.PasswordResetService;
import com.emp_management.shared.enums.ResetStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Password-reset controller.
 *
 * ── Self-service OTP flow ───────────────────────────────────────────────────
 *  POST /api/password-reset/forgot-password      Step 1 — send OTP
 *  POST /api/password-reset/verify-otp           Step 2+3 combined — verify OTP + reset
 *
 * ── Admin override ──────────────────────────────────────────────────────────
 *  POST /api/password-reset/approve/{requestId}  ADMIN only
 *  POST /api/password-reset/reject/{requestId}   ADMIN only
 *  GET  /api/password-reset/admin/list           ADMIN only
 */
@RestController
@RequestMapping("/password-reset")
public class PasswordResetController {

    private final PasswordResetService resetService;
    private final OtpService           otpService;

    public PasswordResetController(PasswordResetService resetService,
                                   OtpService otpService) {
        this.resetService = resetService;
        this.otpService   = otpService;
    }

    // ── STEP 1: Request OTP ───────────────────────────────────────────────

    /**
     * POST /api/password-reset/forgot-password
     * Body: { "email": "user@example.com" }
     *
     * Sends OTP to the registered email. Valid for 10 minutes.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody OtpRequest request) {
        otpService.sendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "OTP sent to your email. Valid for 10 minutes."));
    }


    // ── STEP 2+3: Verify OTP and reset password ───────────────────────────

    /**
     * POST /api/password-reset/verify-otp
     * Body: { "email": "...", "otp": "123456", "newPassword": "NewPass@1" }
     *
     * Validates OTP, applies complexity + last-3 check, resets password,
     * and invalidates all active sessions via lastPasswordChangeAt.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtpAndReset(@RequestBody OtpVerifyRequest request) {
        otpService.verifyOtpAndResetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(Map.of("message",
                "Password reset successfully. Please log in."));
    }

    // ── ADMIN ENDPOINTS ───────────────────────────────────────────────────

    @PostMapping("/approve/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void approve(@PathVariable Long requestId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        resetService.approveResetByEmail(requestId, email);
    }

    @PostMapping("/reject/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void reject(@PathVariable Long requestId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        resetService.rejectResetByEmail(requestId, email);
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PasswordResetAdminResponse> getByStatus(@RequestParam ResetStatus status) {
        return resetService.getByStatus(status);
    }
}