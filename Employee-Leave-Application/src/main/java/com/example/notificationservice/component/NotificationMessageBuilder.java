package com.example.notificationservice.component;

import com.example.notificationservice.dto.EmailMessage;
import com.example.notificationservice.enums.EventType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationMessageBuilder {

    public EmailMessage buildmessage(EventType eventType,
                                     String reason) {
        switch (eventType) {

            case LEAVE_APPROVED:
                return new EmailMessage(
                        "Leave Approved",
                        reason
                );

            case LEAVE_REJECTED:

                return new EmailMessage(
                        "Leave Rejected",
                        reason
                );

            case MEETING_REQUIRED:
                return new EmailMessage(
                        "Meeting Required for Leave Request",
                        reason
                );

            case LEAVE_APPLIED:
                return new EmailMessage(
                        "Leave Approval Pending",
                        reason
                );

            case LEAVE_CANCELLED:
                return new EmailMessage(
                        "Leave Cancelled",
                        "Your leave request has been cancelled."
                );

            default:
                return new EmailMessage(
                        "Notification",
                        "You have a new notification."
                );
        }
    }
}
