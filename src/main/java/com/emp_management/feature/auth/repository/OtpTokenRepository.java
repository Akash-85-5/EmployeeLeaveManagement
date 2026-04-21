package com.emp_management.feature.auth.repository;

import com.emp_management.feature.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByEmailAndUsedFalseOrderByExpiresAtDesc(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < CURRENT_TIMESTAMP OR o.used = true")
    void deleteExpiredOrUsed();
}
