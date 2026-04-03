package com.emp_management.feature.auth.service;

import com.emp_management.feature.auth.dto.ChangePasswordRequest;
import com.emp_management.feature.auth.dto.ForceChangePasswordRequest;
import com.emp_management.feature.auth.dto.LoginRequest;
import com.emp_management.feature.auth.dto.LoginResponse;
import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.feature.auth.util.PasswordValidationUtil;
import com.emp_management.security.JwtTokenProvider;
import com.emp_management.shared.enums.EmployeeStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Auth service — JWT-only (no refresh tokens).
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Flow summary                                               │
 * │  LOGIN          → validate → issue JWT in response body     │
 * │  FORCE-CHANGE   → set new password, clear forceFlag         │
 * │                   (no audit check, no complexity guard,     │
 * │                    no session invalidation)                 │
 * │  CHANGE-PASS    → old-pass check + complexity + last-3      │
 * │                   update lastPasswordChangeAt → all old     │
 * │                   tokens rejected by JWT filter             │
 * └─────────────────────────────────────────────────────────────┘
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      jwtTokenProvider;
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final PasswordAuditService  passwordAuditService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordAuditService passwordAuditService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider      = jwtTokenProvider;
        this.userRepository        = userRepository;
        this.passwordEncoder       = passwordEncoder;
        this.passwordAuditService  = passwordAuditService;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────

    /**
     * Validates credentials and returns a JWT in the response body.
     * Frontend must store the token in sessionStorage and send it as
     * {@code Authorization: Bearer <token>} on every subsequent request.
     */
    public LoginResponse login(LoginRequest request) {

        // Spring Security validates password via CustomUserDetailsService
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmployee_EmpId(request.getIdentifier())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() != EmployeeStatus.ACTIVE) {
            throw new RuntimeException("Account is disabled. Contact admin.");
        }

        String jwt = jwtTokenProvider.generateToken(user);

        return new LoginResponse(
                user.getEmployee().getEmpId(),
                user.getRole(),
                jwt,
                user.isForcePwdChange()
        );
    }

    // ── FORCE-CHANGE PASSWORD (first login) ───────────────────────────────

    /**
     * Called when {@code forcePasswordChange == true}.
     *
     * Rules:
     *  - No old-password check (caller is already authenticated via JWT).
     *  - No password complexity enforcement.
     *  - No last-3 history check (audit table may be empty).
     *  - Does NOT update lastPasswordChangeAt → existing token remains valid,
     *    allowing the frontend to stay logged in after the forced change.
     *  - Clears forcePwdChange flag.
     */
    @Transactional
    public void forceChangePassword(ForceChangePasswordRequest request) {

        String empId = authenticatedEmpId();

        User user = userRepository.findByEmployee_EmpId(empId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isForcePwdChange()) {
            throw new RuntimeException("Force password change is not required.");
        }

        // ✅ ADD THIS
        PasswordValidationUtil.validate(request.getNewPassword());

        String newHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newHash);
        user.setForcePwdChange(false);
        userRepository.save(user);

        passwordAuditService.recordPassword(empId, newHash);
    }
    // ── CHANGE PASSWORD (known old password) ─────────────────────────────

    /**
     * Standard change-password flow (authenticated user knows their old password).
     *
     * Rules:
     *  ✅ Old password must match.
     *  ✅ New password must pass complexity check.
     *  ✅ New password must not match last 3 audit entries.
     *  ✅ Sets lastPasswordChangeAt → all previously issued JWTs become invalid,
     *     effectively logging out all other devices / sessions.
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {

        String empId = authenticatedEmpId();

        User user = userRepository.findByEmployee_EmpId(empId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Old password is incorrect.");
        }

        // 2. Complexity
        PasswordValidationUtil.validate(request.getNewPassword());

        // 3. Last-3 history check (checks however many entries exist — may be 0, 1, 2, or 3)
        passwordAuditService.assertNotRecentlyUsed(empId, request.getNewPassword());

        // 4. Persist
        String newHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newHash);

        // 5. Invalidate all previous sessions (JWT filter compares iat vs this)
        user.setLastPasswordChangeAt(Instant.now());
        userRepository.save(user);

        // 6. Update audit table
        passwordAuditService.recordPassword(empId, newHash);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    /** Extracts the authenticated employee-id from the SecurityContext. */
    private String authenticatedEmpId() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }
}