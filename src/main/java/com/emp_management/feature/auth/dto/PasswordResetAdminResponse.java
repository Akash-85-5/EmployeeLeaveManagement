package com.emp_management.feature.auth.dto;

import java.time.LocalDateTime;

public class PasswordResetAdminResponse {

    private String requestId;
    private String name;
    private String email;
    private LocalDateTime requestedAt;
    private String status;

    public PasswordResetAdminResponse(String requestId,
                                      String name,
                                      String email,
                                      LocalDateTime requestedAt,
                                      String status) {
        this.requestId = requestId;
        this.name = name;
        this.email = email;
        this.requestedAt = requestedAt;
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getStatus() {
        return status;
    }
}
