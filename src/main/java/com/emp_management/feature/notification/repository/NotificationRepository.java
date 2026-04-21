package com.emp_management.feature.notification.repository;

import com.emp_management.feature.notification.entity.Notification;
import com.emp_management.shared.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    // Get notifications with pagination
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);


    // Count unread notifications
    Long countByUserIdAndNotificationStatus(String userId, NotificationStatus status);

    // Get notifications by status (for bulk operations)
    List<Notification> findByUserIdAndNotificationStatus(String userId, NotificationStatus status);
}