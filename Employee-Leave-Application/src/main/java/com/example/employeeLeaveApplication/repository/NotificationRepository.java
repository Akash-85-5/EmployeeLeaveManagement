package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc (Long userId);
}
