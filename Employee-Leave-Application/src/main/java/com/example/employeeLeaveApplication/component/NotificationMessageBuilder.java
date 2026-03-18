package com.example.employeeLeaveApplication.component;

import com.example.employeeLeaveApplication.dto.EmailMessage;
import com.example.employeeLeaveApplication.enums.EventType;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageBuilder {

    public EmailMessage buildmessage(EventType eventType, String reason) {
        switch (eventType) {

            // ── Existing leave events — UNCHANGED ─────────────────
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
            // ── NEW: profile verification events ──────────────────
            case PROFILE_SUBMITTED:
                // reason = employee name, sent to HR
                return new EmailMessage(
                        "New Profile Pending Your Verification",
                        reason);
            case PROFILE_VERIFIED:
                // reason = employee name, sent to employee
                return new EmailMessage(
                        "Your Profile Has Been Verified",
                        reason);
            case PROFILE_REJECTED:
                // reason = rejection remarks, sent to employee
                return new EmailMessage(
                        "Your Profile Submission Was Rejected",
                        reason);

            default:
                return new EmailMessage("Notification", "You have a new notification.");
        }
    }
}