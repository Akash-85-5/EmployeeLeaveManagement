package com.emp_management.feature.auth.service;

import com.emp_management.feature.auth.entity.PasswordAudit;
import com.emp_management.feature.auth.repository.PasswordAuditRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles all password-audit logic:
 *   1. assertNotRecentlyUsed  – rejects a candidate password if it matches
 *                               any of the last 3 stored hashes.
 *   2. recordPassword         – inserts a new audit row and prunes the table
 *                               so that only the 3 most-recent entries remain.
 *
 * "Last 3" semantics
 * ──────────────────
 * The rule is enforced against however many entries actually exist.
 * - First ever password change: 0 entries → nothing to match → allowed.
 *   Afterward 1 entry exists.
 * - Second change: 1 entry checked.
 * - Third change: 2 entries checked.
 * - Fourth change onward: always 3 entries checked, oldest is pruned after each save.
 *
 * Force-password-change (first login, password = "1234")
 * ───────────────────────────────────────────────────────
 * The admin's default "1234" password is intentionally NOT added to the audit
 * table at account-creation time, so the user is not blocked from picking
 * "1234" as their new password (edge-case: admin reuse).
 * If you want to block "1234" reuse you can call recordPassword() from the
 * admin-create-user flow.
 */
@Service
public class PasswordAuditService {

    private final PasswordAuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_AUDIT_ENTRIES = 3;

    public PasswordAuditService(PasswordAuditRepository auditRepository,
                                PasswordEncoder passwordEncoder) {
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── PUBLIC API ─────────────────────────────────────────────────────────

    /**
     * Throws RuntimeException if {@code candidatePassword} matches any of the
     * last {@value MAX_AUDIT_ENTRIES} stored hashes for the given user.
     *
     * @param userId            employee's empId (PK stored in audit table)
     * @param candidatePassword plain-text password to validate
     */
    public void assertNotRecentlyUsed(String userId, String candidatePassword) {
        List<PasswordAudit> recent =
                auditRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId);

        boolean reused = recent.stream()
                .anyMatch(a -> passwordEncoder.matches(candidatePassword, a.getPasswordHash()));

        if (reused) {
            throw new RuntimeException(
                    "New password must not match any of the last " + MAX_AUDIT_ENTRIES + " passwords.");
        }
    }

    /**
     * Persists a new audit entry and prunes the table so that at most
     * {@value MAX_AUDIT_ENTRIES} entries exist for the user.
     *
     * @param userId       employee's empId
     * @param encodedHash  BCrypt-encoded hash of the new password
     */
    @Transactional
    public void recordPassword(String userId, String encodedHash) {
        PasswordAudit entry = new PasswordAudit();
        entry.setUserId(userId);
        entry.setPasswordHash(encodedHash);
        auditRepository.save(entry);

        // Prune: keep only the most recent MAX_AUDIT_ENTRIES rows
        long count = auditRepository.countByUserId(userId);
        if (count > MAX_AUDIT_ENTRIES) {
            auditRepository.deleteOldestByUserId(userId);
        }
    }
}