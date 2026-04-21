package com.emp_management.feature.auth.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/debug")
public class PasswordDebugController {

    private final PasswordEncoder passwordEncoder;

    public PasswordDebugController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/hash")
    public String hash(@RequestParam String password) {

        return passwordEncoder.encode(password);
    }
}