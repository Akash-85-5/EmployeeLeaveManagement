package com.example.employeeLeaveApplication.feature.auth.controller;

import com.example.employeeLeaveApplication.feature.auth.dto.ChangePasswordRequest;
import com.example.employeeLeaveApplication.feature.auth.dto.LoginRequest;
import com.example.employeeLeaveApplication.feature.auth.dto.LoginResponse;
import com.example.employeeLeaveApplication.feature.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private static final String ACCESS_TOKEN_COOKIE  = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private static final int ACCESS_TOKEN_MAX_AGE  = 15 * 60;           // 15 minutes
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ─── LOGIN ─────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request,
                                               HttpServletResponse response) {

        Object[] result        = authService.login(request);
        String accessToken     = (String) result[0];
        String refreshToken    = (String) result[1];
        LoginResponse body     = (LoginResponse) result[2];

        // Both tokens go into HTTP-only cookies — JS cannot read either
        setCookie(response, ACCESS_TOKEN_COOKIE,  accessToken,  ACCESS_TOKEN_MAX_AGE);
        setCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_MAX_AGE);

        return ResponseEntity.ok(body);
    }

    // ─── REFRESH ───────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                     HttpServletResponse response) {

        String refreshToken = extractCookie(request, REFRESH_TOKEN_COOKIE);

        if (refreshToken == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "No refresh token found"));
        }

        try {
            Object[] result     = authService.refreshAccessToken(refreshToken);
            String newAccess    = (String) result[0];
            String newRefresh   = (String) result[1];

            setCookie(response, ACCESS_TOKEN_COOKIE,  newAccess,  ACCESS_TOKEN_MAX_AGE);
            setCookie(response, REFRESH_TOKEN_COOKIE, newRefresh, REFRESH_TOKEN_MAX_AGE);

            return ResponseEntity.ok(Map.of("message", "Token refreshed"));

        } catch (RuntimeException e) {
            clearCookie(response, ACCESS_TOKEN_COOKIE);
            clearCookie(response, REFRESH_TOKEN_COOKIE);
            return ResponseEntity.status(401)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── LOGOUT ────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {

        String refreshToken = extractCookie(request, REFRESH_TOKEN_COOKIE);
        authService.logout(refreshToken);

        clearCookie(response, ACCESS_TOKEN_COOKIE);
        clearCookie(response, REFRESH_TOKEN_COOKIE);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ─── CHANGE PASSWORD ───────────────────────────────────

    @PutMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }

    // ─── COOKIE HELPERS ────────────────────────────────────

    private void setCookie(HttpServletResponse response,
                           String name, String value, int maxAgeSeconds) {

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true) // required for SameSite=None
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
    private void clearCookie(HttpServletResponse response, String name) {

        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
