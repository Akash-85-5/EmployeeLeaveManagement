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

    public LoginResponse login(LoginRequest request) {

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

    @Transactional
    public void forceChangePassword(ForceChangePasswordRequest request,String userId) {


        User user = userRepository.findByEmployee_EmpId(userId)
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

        passwordAuditService.recordPassword(userId, newHash);
    }

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

    private String authenticatedEmpId() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }
}