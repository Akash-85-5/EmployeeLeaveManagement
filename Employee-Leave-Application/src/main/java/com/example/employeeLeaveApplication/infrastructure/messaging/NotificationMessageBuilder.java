package com.example.employeeLeaveApplication.infrastructure.messaging;

import com.example.employeeLeaveApplication.shared.dto.EmailMessage;
import com.example.employeeLeaveApplication.shared.enums.EventType;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageBuilder {

    public EmailMessage buildmessage(EventType eventType, String reason) {
        switch (eventType) {

            case LEAVE_APPROVED:
                return new EmailMessage("Leave Approved", reason);
            case LEAVE_REJECTED:
                return new EmailMessage("Leave Rejected", reason);
            case MEETING_REQUIRED:
                return new EmailMessage("Meeting Required for Leave Request", reason);
            case LEAVE_APPLIED:
                return new EmailMessage("Leave Approval Pending", reason);
            case LEAVE_CANCELLED:
                return new EmailMessage("Leave Cancelled", "Your leave request has been cancelled.");
            case PENDING_LEAVE_REMINDER:
                return new EmailMessage("Reminder: Pending Leave Approval Required", reason);
            case LEAVE_IN_PROGRESS:
                return new EmailMessage("Leave Application Progress", reason);
            case OD_APPROVED:
                return new EmailMessage("OD Approved", reason);
            case OD_REJECTED:
                return new EmailMessage("OD Rejected", reason);
            case OD_APPLIED:
                return new EmailMessage("OD Approval Pending", reason);
            case OD_CANCELLED:
                return new EmailMessage("OD Cancelled", "Your leave request has been cancelled.");
            case OD_IN_PROGRESS:
                return new EmailMessage("OD Application Progress", reason);
            case PROFILE_SUBMITTED:
                return new EmailMessage(
                        "New Profile Pending Your Verification",
                        reason);
            case PROFILE_VERIFIED:
                return new EmailMessage(
                        "Your Profile Has Been Verified",
                        reason);
            case PROFILE_REJECTED:
                return new EmailMessage(
                        "Your Profile Submission Was Rejected",
                        reason);
            case ACCESS_REQUEST_SUBMITTED:
                return new EmailMessage(
                        "New Access Request Pending Your Approval",
                        reason);
            case ACCESS_REQUEST_MANAGER_APPROVED:
                return new EmailMessage(
                        "Your Access Request Approved by Manager",
                        reason);
            case ACCESS_REQUEST_MANAGER_REJECTED:
                return new EmailMessage(
                        "Your Access Request Was Rejected by Manager",
                        reason);
            case ACCESS_REQUEST_ADMIN_APPROVED:
                return new EmailMessage(
                        "Your Access Request Approved – Access Granted!",
                        reason);
            case ACCESS_REQUEST_ADMIN_REJECTED:
                return new EmailMessage(
                        "Your Access Request Was Rejected by Admin",
                        reason);
            default:
                return new EmailMessage("Notification", "You have a new notification.");
        }
    }
}