package com.example.employeeLeaveApplication.feature.auth.repository;

import com.example.employeeLeaveApplication.feature.auth.entity.RefreshToken;
import com.example.employeeLeaveApplication.feature.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Revoke all tokens for a user (used on logout)
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
    void revokeAllByUser(User user);
}
