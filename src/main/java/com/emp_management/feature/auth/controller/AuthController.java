package com.emp_management.feature.auth.controller;

import com.emp_management.feature.auth.dto.*;
import com.emp_management.feature.auth.service.AuthService;
import com.emp_management.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth controller — JWT-only (no cookies, no refresh tokens).
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  Endpoint                   │ Auth required │ Description            │
 * ├──────────────────────────────────────────────────────────────────────┤
 * │  POST /api/auth/login        │ No            │ Returns JWT in body    │
 * │  POST /api/auth/force-change │ Yes (JWT)     │ First-login pwd change │
 * │  PUT  /api/auth/change-pass  │ Yes (JWT)     │ Known-old-pwd change   │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * Removed: /refresh, /logout  (stateless JWT — no server-side session to clear)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/login
     * Body: { "employeeId": "EMP001", "password": "Secret@1" }
     *
     * Returns:
     * {
     *   "employeeId": "EMP001",
     *   "role": "EMPLOYEE",
     *   "token": "eyJ...",
     *   "forcePasswordChange": false
     * }
     *
     * Frontend responsibility:
     *  - Store token in sessionStorage.
     *  - If forcePasswordChange == true → redirect to /force-change.
     *  - Send token as: Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ── FORCE CHANGE PASSWORD (first login) ───────────────────────────────

    /**
     * POST /api/auth/force-change
     * Header: Authorization: Bearer <token>
     * Body: { "newPassword": "MyNewPwd1" }
     *
     * Rules:
     *  - No complexity / history check (admin default "1234" being replaced).
     *  - Clears forcePasswordChange flag.
     *  - Does NOT invalidate the current JWT (user stays logged in).
     */
    @PostMapping("/force-change")
    public ResponseEntity<?> forceChangePassword(
            @RequestBody ForceChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        authService.forceChangePassword(request);
        return ResponseEntity.ok(Map.of("message",
                "Password changed. You may now access the system."));
    }

    // ── CHANGE PASSWORD (known old password) ─────────────────────────────

    /**
     * PUT /api/auth/change-password
     * Header: Authorization: Bearer <token>
     * Body: { "oldPassword": "OldPass@1", "newPassword": "NewPass@2" }
     *
     * Rules:
     *  - Old password must match.
     *  - New password must pass complexity.
     *  - Must not match last 3 passwords.
     *  - Invalidates ALL previously issued tokens (session invalidation).
     */
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(Map.of("message",
                "Password changed successfully. Please log in again."));
    }
}