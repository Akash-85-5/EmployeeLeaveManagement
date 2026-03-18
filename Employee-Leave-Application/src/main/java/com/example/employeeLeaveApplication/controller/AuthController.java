package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.ChangePasswordRequest;
import com.example.employeeLeaveApplication.dto.LoginRequest;
import com.example.employeeLeaveApplication.dto.LoginResponse;
import com.example.employeeLeaveApplication.service.AuthService;
import jakarta.validation.Valid;
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
