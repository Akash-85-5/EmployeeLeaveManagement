package com.emp_management.feature.auth.dto;

public class OtpVerifyRequest {
    private String email;
    private String otp;
    private String newPassword;

    public String getEmail()      { return email; }
    public void setEmail(String e){ this.email = e; }

    public String getOtp()        { return otp; }
    public void setOtp(String o)  { this.otp = o; }

    public String getNewPassword()        { return newPassword; }
    public void setNewPassword(String np) { this.newPassword = np; }
}