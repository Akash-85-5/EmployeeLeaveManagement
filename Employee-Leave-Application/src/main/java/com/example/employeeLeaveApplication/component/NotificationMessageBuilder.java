package com.example.employeeLeaveApplication.component;

import com.example.employeeLeaveApplication.dto.EmailMessage;
import com.example.employeeLeaveApplication.enums.EventType;
import org.springframework.stereotype.Component;

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

            case PENDING_LEAVE_REMINDER:
                return new EmailMessage(
                        "Reminder: Pending Leave Approval Required",
                        reason
                );

            case LEAVE_IN_PROGRESS:
                return new EmailMessage(
                        "Leave Application Progress",
                        reason
                );

            default:
                return new EmailMessage(
                        "Notification",
                        "You have a new notification."
                );
        }
    }
}
