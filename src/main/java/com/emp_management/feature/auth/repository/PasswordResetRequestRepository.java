package com.emp_management.feature.auth.repository;

import com.emp_management.feature.auth.entity.PasswordResetRequest;
import com.emp_management.shared.enums.ResetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    Optional<PasswordResetRequest> findByUserIdAndStatus(String userId, ResetStatus status);

    List<PasswordResetRequest> findByStatus(ResetStatus status);

}
