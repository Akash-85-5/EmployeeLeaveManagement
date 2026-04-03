package com.emp_management.feature.auth.dto;

/**
 * Returned on successful login.
 * token — JWT the frontend stores in sessionStorage.
 */
public class LoginResponse {

    private String employeeId;
    private String role;
    private String token;
    private boolean forcePasswordChange;

    public LoginResponse(String employeeId, String role, String token,
                         boolean forcePasswordChange) {
        this.employeeId          = employeeId;
        this.role                = role;
        this.token               = token;
        this.forcePasswordChange = forcePasswordChange;
    }

    public String getEmployeeId()        { return employeeId; }
    public String getRole()              { return role; }
    public String getToken()             { return token; }
    public boolean isForcePasswordChange() { return forcePasswordChange; }
}