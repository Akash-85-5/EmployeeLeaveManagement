package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.NotificationResponse;
import com.example.employeeLeaveApplication.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ==================== GET USER NOTIFICATIONS (WITH PAGINATION) - UPDATED ====================
    @GetMapping("/{employeeId}")
    public Page<NotificationResponse> getNotifications(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationService.getNotifications(employeeId, pageable);
    }

    // ==================== GET UNREAD NOTIFICATIONS COUNT - NEW ====================
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    // ==================== MARK NOTIFICATION AS READ - NEW ====================
    @PatchMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Notification marked as read");
    }

    // ==================== MARK ALL NOTIFICATIONS AS READ - NEW ====================
    @PostMapping("/user/{userId}/mark-all-read")
    public ResponseEntity<String> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    // ==================== DELETE NOTIFICATION - NEW ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok("Notification deleted successfully");
    }

    // ==================== DELETE ALL READ NOTIFICATIONS - NEW ====================
    @DeleteMapping("/user/{userId}/clear-read")
    public ResponseEntity<String> clearReadNotifications(@PathVariable Long userId) {
        notificationService.clearReadNotifications(userId);
        return ResponseEntity.ok("Read notifications cleared successfully");
    }

    // ==================== GET NOTIFICATION BY ID - NEW ====================
    @GetMapping("/{id}")
    public NotificationResponse getNotification(@PathVariable Long id) {
        return notificationService.getNotification(id);
    }
}