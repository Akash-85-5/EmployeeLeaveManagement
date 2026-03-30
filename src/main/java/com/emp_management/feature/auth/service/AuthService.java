package com.emp_management.feature.auth.service;

import com.emp_management.feature.auth.dto.ChangePasswordRequest;
import com.emp_management.feature.auth.dto.LoginRequest;
import com.emp_management.feature.auth.dto.LoginResponse;
import com.emp_management.feature.auth.entity.RefreshToken;
import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.security.JwtTokenProvider;
import com.emp_management.shared.enums.EmployeeStatus;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────
    // Returns [accessJwt, refreshTokenString, LoginResponse]

    public Object[] login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmployee_Email(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus()== EmployeeStatus.ACTIVE) {
            throw new RuntimeException("Account is disabled. Contact admin.");
        }

        String accessToken    = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        LoginResponse loginResponse = new LoginResponse(
                user.getEmployee().getEmpId(),
                user.getRole(),
                user.isForcePwdChange()
        );

        return new Object[]{accessToken, refreshToken.getToken(), loginResponse};
    }

    // ─── REFRESH ─────────────────────────────────────────────────────────────
    // Returns [newAccessJwt, newRefreshTokenString]

    @Transactional
    public Object[] refreshAccessToken(String rawRefreshToken) {

        RefreshToken validated    = refreshTokenService.validateRefreshToken(rawRefreshToken);
        RefreshToken newRefresh   = refreshTokenService.rotateRefreshToken(validated);
        String newAccessToken     = jwtTokenProvider.generateToken(validated.getUser());

        return new Object[]{newAccessToken, newRefresh.getToken()};
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    // ─── LOGOUT (CURRENT DEVICE ONLY) ─────────────────────────────

    public void logout(String rawRefreshToken) {

        if (rawRefreshToken == null) return;

        // validate token (optional but safe)
        RefreshToken rt = refreshTokenService.validateRefreshToken(rawRefreshToken);

        // ✅ revoke ONLY this token
        refreshTokenService.revokeToken(rt.getToken());
    }

    // ─── CHANGE PASSWORD ─────────────────────────────────────────────────────

    public void changePassword(ChangePasswordRequest request) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmployee_Email(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Old password incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePwdChange(false);
        userRepository.save(user);
    }
}