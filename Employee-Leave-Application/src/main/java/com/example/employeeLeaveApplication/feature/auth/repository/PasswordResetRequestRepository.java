package com.example.employeeLeaveApplication.feature.auth.repository;

import com.example.employeeLeaveApplication.feature.auth.entity.PasswordResetRequest;
import com.example.employeeLeaveApplication.shared.enums.ResetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    Optional<PasswordResetRequest> findByUserIdAndStatus(Long userId, ResetStatus status);

    List<PasswordResetRequest> findByStatus(ResetStatus status);

}
