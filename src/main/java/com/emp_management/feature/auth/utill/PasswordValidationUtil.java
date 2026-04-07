package com.emp_management.feature.auth.util;

import java.util.regex.Pattern;

public class PasswordValidationUtil {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{6,}$");

    public static void validate(String password) {

        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new RuntimeException(
                    "Password must contain at least 1 uppercase letter, 1 number, and 1 special character."
            );
        }
    }
}