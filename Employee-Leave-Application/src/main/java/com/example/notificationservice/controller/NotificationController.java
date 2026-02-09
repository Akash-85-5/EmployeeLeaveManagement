package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {


    private final NotificationService notificationService;

    public NotificationController (NotificationService notificationService){
        this.notificationService = notificationService;
    }

//    @PostMapping
//    public ResponseEntity<NotificationResponse> createNotification(@RequestBody NotificationRequest notificationRequest){
//        Notification savedNotification = notificationService.createNotification(
//                notificationRequest.getUserId(),
//                notificationRequest.getEventType(),
//                notificationRequest.getRecipientType(),
//                notificationRequest.getChannel(),
//                notificationRequest.getContext());
//
//        NotificationResponse response = new NotificationResponse();
//        response.setId(savedNotification.getId());
//        response.setUserId(savedNotification.getUserId());
//        response.setEventType(savedNotification.getEventType());
//        response.setChannel(savedNotification.getChannel());
//        response.setMessage(savedNotification.getMessage());
//        response.setNotificationStatus(savedNotification.getNotificationStatus());
//        response.setCreatedAt(savedNotification.getCreatedAt());
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@PathVariable Long userId){
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

}
