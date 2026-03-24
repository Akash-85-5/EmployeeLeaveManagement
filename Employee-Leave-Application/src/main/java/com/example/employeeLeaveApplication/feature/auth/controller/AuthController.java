package com.example.employeeLeaveApplication.feature.auth.controller;

import com.example.employeeLeaveApplication.feature.auth.dto.ChangePasswordRequest;
import com.example.employeeLeaveApplication.feature.auth.dto.LoginRequest;
import com.example.employeeLeaveApplication.feature.auth.dto.LoginResponse;
import com.example.employeeLeaveApplication.feature.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final int COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60; // 7 days

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        Object[] result = authService.login(request);
        LoginResponse loginResponse = (LoginResponse) result[0];
        String refreshToken = (String) result[1];

        addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(loginResponse);
    }

    // ─── REFRESH ─────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                     HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No refresh token found"));
        }

        try {
            Object[] result = authService.refreshAccessToken(refreshToken);
            String newAccessToken = (String) result[0];
            String newRefreshToken = (String) result[1];

            // Rotate cookie
            addRefreshTokenCookie(response, newRefreshToken);

            return ResponseEntity.ok(Map.of("token", newAccessToken));

        } catch (RuntimeException e) {
            // Clear the cookie on failure
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ─── CHANGE PASSWORD ─────────────────────────────────────────────────────

    @PutMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }

    // ─── COOKIE HELPERS ──────────────────────────────────────────────────────

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);       // JS cannot read this
        cookie.setSecure(true);         // HTTPS only — set false for local dev if needed
        cookie.setPath("/api/auth");    // Only sent to auth endpoints
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);            // Immediately expire
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
