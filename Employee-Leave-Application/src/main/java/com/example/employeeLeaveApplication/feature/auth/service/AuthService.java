package com.example.employeeLeaveApplication.feature.auth.service;

import com.example.employeeLeaveApplication.feature.auth.entity.RefreshToken;
import com.example.employeeLeaveApplication.security.JwtTokenProvider;
import com.example.employeeLeaveApplication.feature.auth.dto.ChangePasswordRequest;
import com.example.employeeLeaveApplication.feature.auth.dto.LoginRequest;
import com.example.employeeLeaveApplication.feature.auth.dto.LoginResponse;
import com.example.employeeLeaveApplication.shared.enums.Status;
import com.example.employeeLeaveApplication.feature.auth.entity.User;
import com.example.employeeLeaveApplication.feature.auth.repository.UserRepository;
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

    public Object[] login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() != Status.ACTIVE) {
            throw new RuntimeException("Account is disabled. Contact admin.");
        }

        String accessToken = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        LoginResponse loginResponse = new LoginResponse(
                user.getId(),
                accessToken,
                user.getRole().name(),
                user.isForcePwdChange()
        );

        return new Object[]{loginResponse, refreshToken.getToken()};
    }

    // ─── REFRESH ACCESS TOKEN ─────────────────────────────────────────────────

    public Object[] refreshAccessToken(String rawRefreshToken) {

        RefreshToken validated = refreshTokenService.validateRefreshToken(rawRefreshToken);
        User user = validated.getUser();

        String newAccessToken = jwtTokenProvider.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new Object[]{newAccessToken, newRefreshToken.getToken()};
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null) {
            // Single validate — get the token object, then revoke all for that user
            RefreshToken rt = refreshTokenService.validateRefreshToken(rawRefreshToken);
            refreshTokenService.revokeAllTokensForUser(rt.getUser());
        }
    }

    // ─── CHANGE PASSWORD ─────────────────────────────────────────────────────

    public void changePassword(ChangePasswordRequest request) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Old password incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePwdChange(false);

        userRepository.save(user);
    }
}
