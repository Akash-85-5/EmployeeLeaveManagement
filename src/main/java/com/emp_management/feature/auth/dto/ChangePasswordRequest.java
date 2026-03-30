package com.emp_management.feature.auth.dto;

public class ChangePasswordRequest {

    private String oldPassword = "1234";
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
