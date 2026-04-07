package com.emp_management.security;

import com.emp_management.feature.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

/**
 * JWT provider – JWT-only auth (no refresh tokens).
 *
 * Payload:
 * {
 *   "sub":        "EMP001",          ← employeeId
 *   "role":       "ADMIN",
 *   "iat":        1710000000,         ← issued-at (seconds)
 *   "exp":        1710003600          ← expiry
 * }
 *
 * Session invalidation after password reset
 * ─────────────────────────────────────────
 * User.lastPasswordChangeAt is set on every password change.
 * The filter calls isTokenIssuedAfterPasswordChange() and rejects
 * tokens whose iat is BEFORE that timestamp.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Token Generation ──────────────────────────────────────────────────

    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(user.getEmployee().getEmpId())
                .claim("role", user.getRole())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token Parsing ─────────────────────────────────────────────────────

    public String getEmployeeIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Date getIssuedAtFromToken(String token) {
        return parseClaims(token).getIssuedAt();
    }

    // ── Validation ────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns true when the token was issued AFTER (or at the same second as)
     * the last password change, meaning the session is still valid.
     *
     * @param token                   raw JWT string
     * @param lastPasswordChangeAt    value from User entity; null = never changed
     */
    public boolean isTokenIssuedAfterPasswordChange(String token, Instant lastPasswordChangeAt) {
        if (lastPasswordChangeAt == null) {
            // Password was never changed via the reset flow → no invalidation needed
            return true;
        }
        Date iat = getIssuedAtFromToken(token);
        // Token is valid if it was issued AT or AFTER the password change
        return !iat.toInstant().isBefore(lastPasswordChangeAt);
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}