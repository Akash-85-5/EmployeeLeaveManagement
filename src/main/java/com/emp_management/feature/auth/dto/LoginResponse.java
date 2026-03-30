package com.emp_management.feature.auth.dto;

public class LoginResponse {

    private String id;
    private String role;
    private boolean forcePasswordChange;

    public LoginResponse(String id, String role, boolean forcePasswordChange) {
        this.id = id;
        this.role = role;
        this.forcePasswordChange = forcePasswordChange;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isForcePasswordChange() { return forcePasswordChange; }
    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }
}