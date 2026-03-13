package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.Meeting;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.exceptions.ResourceNotFoundException;
import com.example.employeeLeaveApplication.exceptions.UnauthorizedException;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.MeetingRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    public MeetingService(MeetingRepository meetingRepository,
                          EmployeeRepository employeeRepository,
                          NotificationService notificationService) {
        this.meetingRepository = meetingRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
    }

    // ── Create Meeting ────────────────────────────────────────────
    public Meeting createMeeting(Long employeeId, Meeting meeting,
                                 List<Long> attendeeIds) {
        Employee creator = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found"));

        // 1. Team-wide overlap check
        List<Meeting> conflicts = meetingRepository.findOverlappingMeetings(
                creator.getTeamId(),
                meeting.getStartTime(),
                meeting.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new BadRequestException(
                    "Meeting time overlaps with team schedule");
        }

        // 2. Identify HR presence & check HR availability
        boolean isHrRequired = false;
        if (attendeeIds != null && !attendeeIds.isEmpty()) {
            List<Employee> selectedAttendees =
                    employeeRepository.findAllById(attendeeIds);

            for (Employee attendee : selectedAttendees) {
                if (attendee.getRole() == Role.HR) {
                    isHrRequired = true;
                    if (meetingRepository.existsOverlappingMeetingForAttendee(
                            attendee.getId(),
                            meeting.getStartTime(),
                            meeting.getEndTime())) {
                        throw new BadRequestException(
                                "HR Representative " + attendee.getName()
                                        + " is already booked.");
                    }
                }
            }
            meeting.setAttendees(new HashSet<>(selectedAttendees));
        }

        // 3. Set metadata
        meeting.setCreatedBy(employeeId);
        meeting.setTeamId(creator.getTeamId());
        meeting.setHrApprovalRequired(isHrRequired);

        // 4. State machine + notifications
        if (creator.getRole() == Role.TEAM_LEADER) {
            // TL created → Manager must approve
            meeting.setStatus(MeetingStatus.PENDING);
            Meeting saved = meetingRepository.save(meeting);

            // Notify Manager
            notifyManager(saved, creator,
                    creator.getName() + " (Team Leader) has created a meeting '"
                            + saved.getTitle() + "' on " + saved.getStartTime()
                            + ". Awaiting your approval.");

            // Notify all attendees about the scheduled meeting request
            notifyAttendees(saved, creator,
                    "You have been invited to a meeting '"
                            + saved.getTitle() + "' scheduled on "
                            + saved.getStartTime()
                            + ". Pending Manager approval.");

            return saved;

        } else if (creator.getRole() == Role.MANAGER) {
            // Manager created → HR approval if HR invited, else auto-scheduled
            meeting.setStatus(isHrRequired
                    ? MeetingStatus.PENDING_HR_APPROVAL
                    : MeetingStatus.SCHEDULED);
            Meeting saved = meetingRepository.save(meeting);

            if (isHrRequired) {
                // Notify HR
                notifyAllHr(saved, creator,
                        creator.getName() + " (Manager) has created a meeting '"
                                + saved.getTitle() + "' on " + saved.getStartTime()
                                + ". Awaiting your approval.");
            }

            // Notify attendees — meeting scheduled or pending HR
            String attendeeMsg = isHrRequired
                    ? "You have been invited to meeting '" + saved.getTitle()
                    + "' on " + saved.getStartTime() + ". Pending HR approval."
                    : "You have been invited to meeting '" + saved.getTitle()
                    + "' scheduled on " + saved.getStartTime() + ".";
            notifyAttendees(saved, creator, attendeeMsg);

            return saved;

        } else if (creator.getRole() == Role.HR) {
            // HR created → auto-scheduled
            meeting.setStatus(MeetingStatus.SCHEDULED);
            Meeting saved = meetingRepository.save(meeting);

            // Notify all attendees — meeting confirmed
            notifyAttendees(saved, creator,
                    "You have been invited to meeting '" + saved.getTitle()
                            + "' scheduled on " + saved.getStartTime() + ".");

            return saved;

        } else {
            throw new UnauthorizedException(
                    "Standard Employees cannot create meetings.");
        }
    }

    // ── Manager Approval ──────────────────────────────────────────
    public Meeting approveByManager(Long meetingId, Long managerId) {
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manager not found"));

        if (manager.getRole() != Role.MANAGER) {
            throw new UnauthorizedException(
                    "Only Managers can provide team-level approval");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found"));

        if (meeting.getStatus() != MeetingStatus.PENDING) {
            throw new BadRequestException(
                    "Meeting is not awaiting Manager approval");
        }

        Employee creator = employeeRepository.findById(meeting.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creator not found"));

        if (meeting.isHrApprovalRequired()) {
            // Advance to HR
            meeting.setStatus(MeetingStatus.PENDING_HR_APPROVAL);
            meetingRepository.save(meeting);

            // Notify HR
            notifyAllHr(meeting, manager,
                    creator.getName() + "'s meeting '" + meeting.getTitle()
                            + "' on " + meeting.getStartTime()
                            + " has been approved by Manager. Awaiting your approval.");

            // Notify creator — progress update
            notifyPerson(meeting, creator, manager,
                    EventType.LEAVE_IN_PROGRESS,
                    "Your meeting '" + meeting.getTitle()
                            + "' has been approved by Manager. Pending HR approval.");

            // Notify attendees — still pending
            notifyAttendees(meeting, manager,
                    "Meeting '" + meeting.getTitle() + "' on "
                            + meeting.getStartTime()
                            + " has been approved by Manager. Pending HR approval.");

        } else {
            // Fully scheduled
            meeting.setStatus(MeetingStatus.SCHEDULED);
            meetingRepository.save(meeting);

            // Notify creator — approved
            notifyPerson(meeting, creator, manager,
                    EventType.LEAVE_APPROVED,
                    "Your meeting '" + meeting.getTitle()
                            + "' on " + meeting.getStartTime()
                            + " has been approved and scheduled.");

            // Notify all attendees — confirmed
            notifyAttendees(meeting, manager,
                    "Meeting '" + meeting.getTitle() + "' on "
                            + meeting.getStartTime()
                            + " has been approved and is now scheduled.");
        }

        return meeting;
    }

    // ── HR Approval ───────────────────────────────────────────────
    public Meeting approveByHr(Long meetingId, Long hrId) {
        Employee hr = employeeRepository.findById(hrId)
                .orElseThrow(() -> new ResourceNotFoundException("HR not found"));

        if (hr.getRole() != Role.HR) {
            throw new UnauthorizedException(
                    "Only an HR staff member can provide HR approval");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found"));

        if (meeting.getStatus() != MeetingStatus.PENDING_HR_APPROVAL) {
            throw new BadRequestException(
                    "This meeting is not currently awaiting HR sign-off");
        }

        Employee creator = employeeRepository.findById(meeting.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creator not found"));

        meeting.setStatus(MeetingStatus.SCHEDULED);
        meetingRepository.save(meeting);

        // Notify creator — fully approved
        notifyPerson(meeting, creator, hr,
                EventType.LEAVE_APPROVED,
                "Your meeting '" + meeting.getTitle()
                        + "' on " + meeting.getStartTime()
                        + " has been approved by HR and is now scheduled.");

        // Notify all attendees — confirmed
        notifyAttendees(meeting, hr,
                "Meeting '" + meeting.getTitle() + "' on "
                        + meeting.getStartTime()
                        + " has been fully approved and is now scheduled.");

        return meeting;
    }

    // ── Reject Meeting ────────────────────────────────────────────
    public Meeting rejectMeeting(Long meetingId, Long reviewerId,
                                 String reason) {
        Employee reviewer = employeeRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found"));

        boolean isAuthorized =
                (meeting.getStatus() == MeetingStatus.PENDING
                        && reviewer.getRole() == Role.MANAGER) ||
                        (meeting.getStatus() == MeetingStatus.PENDING_HR_APPROVAL
                                && reviewer.getRole() == Role.HR);

        if (!isAuthorized) {
            throw new UnauthorizedException(
                    "You are not authorized to reject this meeting at this stage");
        }

        Employee creator = employeeRepository.findById(meeting.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creator not found"));

        meeting.setStatus(MeetingStatus.REJECTED);
        meetingRepository.save(meeting);

        // Notify creator — rejected
        notifyPerson(meeting, creator, reviewer,
                EventType.LEAVE_REJECTED,
                "Your meeting '" + meeting.getTitle()
                        + "' on " + meeting.getStartTime()
                        + " has been rejected by " + reviewer.getName()
                        + (reason != null ? ". Reason: " + reason : "."));

        // Notify attendees — meeting rejected
        notifyAttendees(meeting, reviewer,
                "Meeting '" + meeting.getTitle() + "' on "
                        + meeting.getStartTime()
                        + " has been rejected and will not proceed.");

        return meeting;
    }

    // ── Cancel Meeting ────────────────────────────────────────────
    public Meeting cancelMeeting(Long meetingId, Long employeeId) {
        Employee user = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found"));

        boolean canCancel = user.getRole() == Role.MANAGER
                || user.getRole() == Role.HR
                || meeting.getCreatedBy().equals(employeeId);

        if (!canCancel) {
            throw new UnauthorizedException(
                    "You don't have permission to cancel this meeting");
        }

        Employee creator = employeeRepository.findById(meeting.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creator not found"));

        meeting.setStatus(MeetingStatus.CANCELLED);
        meetingRepository.save(meeting);

        // Notify creator (only if someone else cancelled it)
        if (!meeting.getCreatedBy().equals(employeeId)) {
            notifyPerson(meeting, creator, user,
                    EventType.LEAVE_CANCELLED,
                    "Your meeting '" + meeting.getTitle()
                            + "' on " + meeting.getStartTime()
                            + " has been cancelled by " + user.getName() + ".");
        }

        // Notify all attendees — meeting cancelled
        notifyAttendees(meeting, user,
                "Meeting '" + meeting.getTitle() + "' on "
                        + meeting.getStartTime()
                        + " has been cancelled.");

        return meeting;
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    /** Notify a specific person */
    private void notifyPerson(Meeting meeting, Employee recipient,
                              Employee sender, EventType eventType,
                              String message) {
        notificationService.createNotification(
                recipient.getId(),
                sender.getEmail(),
                recipient.getEmail(),
                eventType,
                recipient.getRole(),
                Channel.EMAIL,
                message
        );
    }

    /** Notify the manager of the creator */
    private void notifyManager(Meeting meeting, Employee creator,
                               String message) {
        if (creator.getManagerId() == null) return;

        Employee manager = employeeRepository
                .findById(creator.getManagerId()).orElse(null);
        if (manager == null) return;

        notificationService.createNotification(
                manager.getId(),
                creator.getEmail(),
                manager.getEmail(),
                EventType.LEAVE_APPLIED,
                manager.getRole(),
                Channel.EMAIL,
                message
        );
    }

    /** Notify all HR employees */
    private void notifyAllHr(Meeting meeting, Employee sender,
                             String message) {
        List<Employee> hrList = employeeRepository.findAllHr();
        for (Employee hr : hrList) {
            notificationService.createNotification(
                    hr.getId(),
                    sender.getEmail(),
                    hr.getEmail(),
                    EventType.LEAVE_APPLIED,
                    hr.getRole(),
                    Channel.EMAIL,
                    message
            );
        }
    }

    /** Notify all meeting attendees (excluding the sender) */
    private void notifyAttendees(Meeting meeting, Employee sender,
                                 String message) {
        if (meeting.getAttendees() == null || meeting.getAttendees().isEmpty())
            return;

        for (Employee attendee : meeting.getAttendees()) {
            // Skip the sender themselves
            if (attendee.getId().equals(sender.getId())) continue;

            notificationService.createNotification(
                    attendee.getId(),
                    sender.getEmail(),
                    attendee.getEmail(),
                    EventType.MEETING_REQUIRED,
                    attendee.getRole(),
                    Channel.EMAIL,
                    message
            );
        }
    }
}