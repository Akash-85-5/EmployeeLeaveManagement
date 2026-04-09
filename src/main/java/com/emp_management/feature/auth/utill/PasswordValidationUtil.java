package com.emp_management.feature.auth.utill;

import com.emp_management.shared.exceptions.BadRequestException;

import java.util.regex.Pattern;

public class PasswordValidationUtil {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{6,}$");

    public static void validate(String password) {

        if (password == null || password.trim().isEmpty()) {
            throw new BadRequestException("Password cannot be empty.");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestException(
                    "Password must contain at least one uppercase letter, one number, and one special character."
            );
        }
    }
}