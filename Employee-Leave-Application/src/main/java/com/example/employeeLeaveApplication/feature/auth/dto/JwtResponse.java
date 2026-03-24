package com.example.employeeLeaveApplication.feature.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtResponse {

    private String token;
    private String role;
    private boolean forcePasswordChange;
}
