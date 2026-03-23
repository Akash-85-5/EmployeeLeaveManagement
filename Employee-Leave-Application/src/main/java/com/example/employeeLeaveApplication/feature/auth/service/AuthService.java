package com.example.employeeLeaveApplication.feature.auth.service;

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

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔴 BLOCK INACTIVE USERS HERE
        if (user.getStatus() != Status.ACTIVE) {
            throw new RuntimeException("Account is disabled. Contact admin.");
        }

        String token = jwtTokenProvider.generateToken(user);

        return new LoginResponse(
                user.getId(),
                token,
                user.getRole().name(),
                user.isForcePwdChange()
        );
    }


    public void changePassword(ChangePasswordRequest request) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(),
                user.getPasswordHash())) {
            throw new RuntimeException("Old password incorrect");
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword()));
        user.setForcePwdChange(false);

        userRepository.save(user);
    }
}
