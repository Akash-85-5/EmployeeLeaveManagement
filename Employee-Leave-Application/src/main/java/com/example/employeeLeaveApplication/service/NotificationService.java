package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.NotificationMessageBuilder;
import com.example.employeeLeaveApplication.dto.EmailMessage;
import com.example.employeeLeaveApplication.dto.NotificationResponse;
import com.example.employeeLeaveApplication.component.EmailSender;
import com.example.employeeLeaveApplication.entity.Notification;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.NotificationRepository;
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

    public Notification createNotification(Long userId,
                                           String email,
                                           EventType eventType,
                                           Role recipientType,
                                           Channel channel,
                                           String context) {

        EmailMessage emailMessage = notificationMessageBuilder.buildmessage(eventType, context);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setEventType(eventType);
        notification.setChannel(channel);
        notification.setMessage(emailMessage.getBody());
        notification.setNotificationStatus(NotificationStatus.SENT);

        Notification saved = notificationRepository.save(notification);

        if (channel == Channel.EMAIL) {
            emailSender.sendEmail(
                    email,
                    emailMessage.getSubject(),
                    emailMessage.getBody()
            );
        }

        return saved;
    }

    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::mapToResponse);
    }


    /**
     * Get unread notification count
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndNotificationStatus(
                userId, NotificationStatus.PENDING);
    }

    /**
     * Mark single notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found with ID: " + notificationId));

        notification.setNotificationStatus(NotificationStatus.SENT);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndNotificationStatus(userId, NotificationStatus.PENDING);

        for (Notification notification : unreadNotifications) {
            notification.setNotificationStatus(NotificationStatus.SENT);
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
    public void clearReadNotifications(Long userId) {
        List<Notification> readNotifications = notificationRepository
                .findByUserIdAndNotificationStatus(userId, NotificationStatus.SENT);

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