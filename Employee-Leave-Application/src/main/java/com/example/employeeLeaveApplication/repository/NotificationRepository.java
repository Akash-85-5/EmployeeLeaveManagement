package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Notification;
import com.example.employeeLeaveApplication.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    // Get notifications with pagination
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);


    // Count unread notifications
    Long countByUserIdAndNotificationStatus(Long userId, NotificationStatus status);

    // Get notifications by status (for bulk operations)
    List<Notification> findByUserIdAndNotificationStatus(Long userId, NotificationStatus status);
}