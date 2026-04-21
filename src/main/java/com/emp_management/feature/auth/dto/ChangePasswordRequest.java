package com.emp_management.feature.auth.dto;

/**
 * Used for /change-password (known old password required).
 * Complexity rules + last-3 audit check are enforced.
 * NOTE: old default was hardcoded to "1234" — removed; must be supplied by caller.
 */
public class ChangePasswordRequest {

    private String oldPassword;
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
