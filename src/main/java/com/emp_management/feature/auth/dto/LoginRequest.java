package com.emp_management.feature.auth.dto;

public class LoginRequest {

    // User can provide either email or employeeId (e.g. "WENXT01")
    private String email;
    private String employeeId;
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
