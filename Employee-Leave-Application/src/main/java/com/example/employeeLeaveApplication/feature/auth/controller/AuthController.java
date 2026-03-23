package com.example.employeeLeaveApplication.feature.auth.controller;

import com.example.employeeLeaveApplication.feature.auth.dto.ChangePasswordRequest;
import com.example.employeeLeaveApplication.feature.auth.dto.LoginRequest;
import com.example.employeeLeaveApplication.feature.auth.dto.LoginResponse;
import com.example.employeeLeaveApplication.feature.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PutMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }
}
