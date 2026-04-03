package com.emp_management.feature.notification.controller;


import com.emp_management.feature.notification.dto.NotificationResponse;
import com.emp_management.feature.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    @GetMapping("/user/{employeeId}")
    public Page<NotificationResponse> getNotifications(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationService.getNotifications(employeeId, pageable);
    }
    @GetMapping("/{id}")
    public NotificationResponse getNotification(@PathVariable Long id) {
        return notificationService.getNotification(id);
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PostMapping("/user/{userId}/mark-all-read")
    public ResponseEntity<String> markAllAsRead(@PathVariable String userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok("Notification deleted successfully");
    }

    @DeleteMapping("/user/{userId}/clear-read")
    public ResponseEntity<String> clearReadNotifications(@PathVariable String userId) {
        notificationService.clearReadNotifications(userId);
        return ResponseEntity.ok("Read notifications cleared successfully");
    }
}