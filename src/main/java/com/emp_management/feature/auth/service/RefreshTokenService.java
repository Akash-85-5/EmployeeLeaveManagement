package com.emp_management.feature.auth.service;

import com.emp_management.feature.auth.entity.RefreshToken;
import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // ✅ CLEANUP
    @Transactional
    public void cleanUpTokens() {
        refreshTokenRepository.deleteAllExpiredOrRevoked();
    }

    // ─── LOGIN (MULTI-DEVICE) ─────────────────────────────
    @Transactional
    public RefreshToken createRefreshToken(User user) {

        cleanUpTokens();

        // ❌ REMOVED revokeAllByUser → allows multiple sessions

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        token.setRevoked(false);

        return refreshTokenRepository.save(token);
    }

    // ─── REFRESH (ROTATION PER DEVICE) ────────────────────
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {

        cleanUpTokens();

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(oldToken.getUser());
        newToken.setToken(UUID.randomUUID().toString());
        newToken.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        newToken.setRevoked(false);

        return refreshTokenRepository.save(newToken);
    }

    // ─── VALIDATE ─────────────────────────────────────────
    public RefreshToken validateRefreshToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token has expired");
        }

        return refreshToken;
    }

    // ─── LOGOUT CURRENT DEVICE ONLY ✅ ─────────────────────
    @Transactional
    public void revokeToken(String token) {

        if (token == null) return;

        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }
    // ❗ KEEP THIS ONLY IF YOU WANT "LOGOUT ALL DEVICES"
    @Transactional
    public void revokeAllTokensForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}