package com.emp_management.feature.auth.dto;

public class LoginRequest {

    private String identifier;
    private String password;

    // keep getEmail() as alias so nothing else breaks
    public String getEmail() { return identifier; }

    public String getIdentifier() { return identifier; }
    public String getPassword() { return password; }
}