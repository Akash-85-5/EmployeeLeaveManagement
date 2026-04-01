package com.emp_management.feature.notification.service;

import com.emp_management.feature.notification.dto.NotificationResponse;
import com.emp_management.feature.notification.entity.Notification;
import com.emp_management.feature.notification.repository.NotificationRepository;
import com.emp_management.infrastructure.messaging.EmailSender;
import com.emp_management.infrastructure.messaging.NotificationMessageBuilder;
import com.emp_management.shared.dto.EmailMessage;
import com.emp_management.shared.enums.Channel;
import com.emp_management.shared.enums.EventType;
import com.emp_management.shared.enums.NotificationStatus;
import com.emp_management.shared.exceptions.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final NotificationMessageBuilder notificationMessageBuilder;

    public NotificationService(NotificationRepository notificationRepository,
                               EmailSender emailSender,
                               NotificationMessageBuilder notificationMessageBuilder) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.notificationMessageBuilder = notificationMessageBuilder;
    }

    // ==================== EXISTING METHODS ====================

    public Notification createNotification(String userId,
                                           String fromEmail,
                                           String toEmail,
                                           EventType eventType,
                                           Channel channel,
                                           String context) {

        EmailMessage emailMessage = notificationMessageBuilder.buildmessage(eventType, context);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setEventType(eventType);
        notification.setChannel(channel);
        notification.setMessage(emailMessage.getBody());
        notification.setNotificationStatus(NotificationStatus.UNREAD);

        Notification saved = notificationRepository.save(notification);

        // for testing i have used my mail in here.
        if (channel == Channel.EMAIL) {
            emailSender.sendEmail(
                    "crazyyy1235@gmail.com",
                    toEmail,
                    emailMessage.getSubject(),
                    emailMessage.getBody()
            );
        }

        return saved;
    }

    public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::mapToResponse);
    }


    /**
     * Get unread notification count
     */
    public Long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndNotificationStatus(
                userId, NotificationStatus.UNREAD);
    }

    /**
     * Mark single notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found with ID: " + notificationId));

        notification.setNotificationStatus(NotificationStatus.READ);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndNotificationStatus(userId, NotificationStatus.UNREAD);

        for (Notification notification : unreadNotifications) {
            notification.setNotificationStatus(NotificationStatus.READ);
        }

        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Delete single notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found with ID: " + notificationId));

        notificationRepository.delete(notification);
    }

    /**
     * Clear all read notifications for a user
     */
    @Transactional
    public void clearReadNotifications(String userId) {
        List<Notification> readNotifications = notificationRepository
                .findByUserIdAndNotificationStatus(userId, NotificationStatus.UNREAD);

        notificationRepository.deleteAll(readNotifications);
    }

    /**
     * Get single notification by ID
     */
    public NotificationResponse getNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found with ID: " + notificationId));

        return mapToResponse(notification);
    }

    // ==================== HELPER METHOD ====================

    private NotificationResponse mapToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setUserId(notification.getUserId());
        response.setEventType(notification.getEventType());
        response.setMessage(notification.getMessage());
        response.setChannel(notification.getChannel());
        response.setCreatedAt(notification.getCreatedAt());
        response.setNotificationStatus(notification.getNotificationStatus());

        return response;
    }
}