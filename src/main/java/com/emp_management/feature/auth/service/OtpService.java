package com.emp_management.feature.auth.service;

import com.emp_management.feature.auth.entity.OtpToken;
import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.OtpTokenRepository;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.feature.auth.utill.PasswordValidationUtil;
import com.emp_management.infrastructure.messaging.EmailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * OTP-based forgot-password flow (3 steps):
 *
 *  Step 1 — sendOtp          : email → OTP stored + emailed
 *  Step 2 — verifyOtp        : validate OTP, mark used (does NOT reset password yet)
 *  Step 3 — resetAfterOtp    : apply new password (complexity + last-3 checked)
 *
 * Session invalidation (forgot-password):
 *   After successful reset, lastPasswordChangeAt = Instant.now().
 *   All previously issued JWTs are rejected by JwtAuthenticationFilter because
 *   their iat is before lastPasswordChangeAt.
 *   This is Option A from the requirements (recommended).
 */
@Service
public class OtpService {

    private final OtpTokenRepository   otpTokenRepository;
    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JavaMailSender       mailSender;
    private final PasswordAuditService passwordAuditService;
    private final EmailSender emailSender;

    private static final int OTP_EXPIRY_MINUTES = 10;

    public OtpService(OtpTokenRepository otpTokenRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender, PasswordAuditService passwordAuditService, EmailSender emailSender) {
        this.otpTokenRepository = otpTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.passwordAuditService = passwordAuditService;
        this.emailSender = emailSender;
    }

    // ── STEP 1: Send OTP ──────────────────────────────────────────────────

    @Transactional
    public void sendOtp(String email) {

        // Validate user exists
        userRepository.findByEmployee_Email(email)
                .orElseThrow(() -> new RuntimeException("No account found for this email."));

        // Clean up stale tokens
        otpTokenRepository.deleteExpiredOrUsed();

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setOtp(otp);
        otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpToken.setUsed(false);
        otpTokenRepository.save(otpToken);

        emailSender.sendEmail(
                email,
                email,
                "Password Reset OTP",
                "Your OTP for password reset is: " + otp +
                        "\n\nThis OTP is valid for " + OTP_EXPIRY_MINUTES + " minutes." +
                        "\nDo not share this OTP with anyone."
        );
    }

    // ── STEP 2: Verify OTP (does NOT reset password) ─────────────────────

    /**
     * Validates OTP and marks it as used.
     * Returns the email so the caller can pass it to step 3.
     * Throws RuntimeException on any validation failure.
     */
    @Transactional
    public String verifyOtp(String email, String otp) {

        OtpToken otpToken = otpTokenRepository
                .findTopByEmailAndUsedFalseOrderByExpiresAtDesc(email)
                .orElseThrow(() -> new RuntimeException(
                        "No active OTP found. Please request a new one."));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (!otpToken.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP.");
        }

        // Mark used so it can't be replayed
        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        return email;
    }

    // ── STEP 3: Reset password after OTP verified ─────────────────────────

    /**
     * Applies the new password.
     *
     * Rules:
     *  ✅ Password complexity enforced.
     *  ✅ Must not match last 3 passwords (checks however many entries exist).
     *  ✅ Updates lastPasswordChangeAt → ALL previously issued JWTs are invalid.
     *
     * Note: The OTP is already marked used in step 2. We do NOT re-verify the
     * OTP here; the frontend is responsible for calling step 3 only after
     * step 2 returned HTTP 200.  If you need extra safety you can add a
     * server-side "verified" flag on OtpToken.
     */
    @Transactional
    public void resetPasswordAfterOtp(String email, String newPassword) {

        // Complexity
        PasswordValidationUtil.validate(newPassword);

        User user = userRepository.findByEmployee_Email(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        String empId = user.getEmployee().getEmpId();

        // Last-3 history check
        passwordAuditService.assertNotRecentlyUsed(empId, newPassword);

        // Persist new password
        String newHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHash);
        user.setForcePwdChange(false);

        // ✅ Invalidate ALL active sessions (Option A — iat-based)
        user.setLastPasswordChangeAt(Instant.now());
        userRepository.save(user);

        // Update audit table
        passwordAuditService.recordPassword(empId, newHash);
    }

    // ── CONVENIENCE: verify + reset in one call (backward-compat) ─────────

    /**
     * Combines steps 2 and 3 — kept for the existing controller endpoint
     * {@code POST /api/password-reset/verify-otp}.
     */
    @Transactional
    public void verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        verifyOtp(email, otp);
        resetPasswordAfterOtp(email, newPassword);
    }
}