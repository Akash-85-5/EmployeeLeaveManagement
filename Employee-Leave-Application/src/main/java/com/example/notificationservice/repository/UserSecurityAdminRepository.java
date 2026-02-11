package com.example.notificationservice.repository;

import com.example.notificationservice.entity.UserSecurityAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSecurityAdminRepository extends JpaRepository<UserSecurityAdmin, Long> {
}
