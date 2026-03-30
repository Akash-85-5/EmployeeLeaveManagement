package com.emp_management.feature.auth.repository;

import com.emp_management.feature.auth.entity.RefreshToken;
import com.emp_management.feature.auth.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
    void revokeAllByUser(User user);

    // ✅ NEW — cleanup old tokens
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true OR r.expiresAt < CURRENT_TIMESTAMP")
    void deleteAllExpiredOrRevoked();
}