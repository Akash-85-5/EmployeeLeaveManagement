package com.example.employeeLeaveApplication.feature.auth.dto;

public class LoginResponse {

    private Long id;
    private String token;       // short-lived access JWT
    private String role;
    private boolean forcePasswordChange;

    public LoginResponse(Long id, String token, String role, boolean forcePasswordChange) {
        this.id = id;
        this.token = token;
        this.role = role;
        this.forcePasswordChange = forcePasswordChange;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isForcePasswordChange() { return forcePasswordChange; }
    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }
}
