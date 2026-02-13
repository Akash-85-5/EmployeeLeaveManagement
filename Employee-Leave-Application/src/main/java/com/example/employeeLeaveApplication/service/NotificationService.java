package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.NotificationMessageBuilder;
import com.example.employeeLeaveApplication.dto.EmailMessage;
import com.example.employeeLeaveApplication.dto.NotificationResponse;
import com.example.employeeLeaveApplication.component.EmailSender;
import com.example.employeeLeaveApplication.entity.Notification;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class NotificationService {


    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final NotificationMessageBuilder notificationMessageBuilder;

    public NotificationService (NotificationRepository notificationRepository, EmailSender emailSender, NotificationMessageBuilder notificationMessageBuilder){
        this.notificationRepository=notificationRepository;
        this.emailSender=emailSender;
        this.notificationMessageBuilder=notificationMessageBuilder;
    }

    public Notification createNotification(Long userId,
                                           String email,
                                           EventType eventType,
                                           Role recipientType,
                                           Channel channel,
                                           String context){

        EmailMessage emailMessage = notificationMessageBuilder.buildmessage(eventType, context);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setEventType(eventType);
        notification.setChannel(channel);
        notification.setMessage(emailMessage.getBody());
        notification.setNotificationStatus(NotificationStatus.SENT);

        Notification saved = notificationRepository.save(notification);

        if(channel == Channel.EMAIL){
            emailSender.sendEmail(
                    email,
                    emailMessage.getSubject(),
                    emailMessage.getBody()
            );
        }

        return saved;
    }

    public List<NotificationResponse> getNotifications(Long userId){

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::mapToResponse).toList();
    }

    public NotificationResponse mapToResponse(Notification notification){
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setMessage(notification.getMessage());
        response.setCreatedAt(notification.getCreatedAt());
        response.setNotificationStatus(notification.getNotificationStatus());

        return response;
    }

}
