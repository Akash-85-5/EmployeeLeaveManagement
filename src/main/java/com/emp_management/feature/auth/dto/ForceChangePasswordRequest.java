package com.emp_management.feature.auth.dto;

/**
 * Used for /force-change-password (first-login flow).
 * Only the new password is required — no old-password check, no complexity
 * or history guard, because the current password is the admin default "1234".
 */
public class ForceChangePasswordRequest {

    private String newPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}