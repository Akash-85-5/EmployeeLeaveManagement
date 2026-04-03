package com.emp_management.feature.auth.service;

import com.emp_management.feature.auth.entity.OtpToken;
import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.OtpTokenRepository;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final RefreshTokenService refreshTokenService;

    private static final int OTP_EXPIRY_MINUTES = 10;

    public OtpService(OtpTokenRepository otpTokenRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JavaMailSender mailSender,
                      RefreshTokenService refreshTokenService) {
        this.otpTokenRepository = otpTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.refreshTokenService = refreshTokenService;
    }

    // ─── SEND OTP ─────────────────────────────────────────

    @Transactional
    public void sendOtp(String email) {

        userRepository.findByEmployee_Email(email)
                .orElseThrow(() -> new EntityNotFoundException("No account found for this email"));

        otpTokenRepository.deleteExpiredOrUsed();

        String otp = String.format("%06d", new SecureRandom().nextInt(999999));

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setOtp(otp);
        otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpToken.setUsed(false);

        otpTokenRepository.save(otpToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset OTP");
        message.setText(
                "Your OTP for password reset is: " + otp +
                        "\n\nThis OTP is valid for " + OTP_EXPIRY_MINUTES + " minutes." +
                        "\nDo not share this OTP with anyone."
        );
        mailSender.send(message);
    }

    // ─── VERIFY OTP & RESET PASSWORD ──────────────────────
    // Also revokes ALL sessions so every logged-in device is kicked out

    @Transactional
    public void verifyOtpAndResetPassword(String email, String otp, String newPassword) {

        OtpToken otpToken = otpTokenRepository
                .findTopByEmailAndUsedFalseOrderByExpiresAtDesc(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No active OTP found. Please request a new one."));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        if (!otpToken.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        User user = userRepository.findByEmployee_Email(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePwdChange(false);
        userRepository.save(user);

        // ✅ Revoke ALL refresh tokens → all sessions terminated across all devices
        // Forgot password via OTP is a security-sensitive action —
        // someone else may have triggered it, so kick everyone out
        refreshTokenService.revokeAllTokensForUser(user);
    }
}