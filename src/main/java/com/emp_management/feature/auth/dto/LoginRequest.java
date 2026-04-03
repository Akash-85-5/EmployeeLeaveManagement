package com.emp_management.feature.auth.dto;

/**
 * Login accepts employeeId (only) as the identifier.
 * The original system also supported email — removed per new requirements.
 */
public class LoginRequest {

    private String identifier;
    private String password;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}