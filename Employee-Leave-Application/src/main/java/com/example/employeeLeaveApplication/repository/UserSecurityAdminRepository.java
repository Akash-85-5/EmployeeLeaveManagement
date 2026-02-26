package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.UserSecurityAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSecurityAdminRepository extends JpaRepository<UserSecurityAdmin, Long> {

    // 🔹 Find record by userId
    Optional<UserSecurityAdmin> findByUserId(Long userId);
}