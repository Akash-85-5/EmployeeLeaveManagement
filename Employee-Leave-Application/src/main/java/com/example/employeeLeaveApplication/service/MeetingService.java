package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.Meeting;
import com.example.employeeLeaveApplication.enums.MeetingStatus;
import com.example.employeeLeaveApplication.enums.Role;
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

    public MeetingService(MeetingRepository meetingRepository, EmployeeRepository employeeRepository) {
        this.meetingRepository = meetingRepository;
        this.employeeRepository = employeeRepository;
    }

    public Meeting createMeeting(Long employeeId, Meeting meeting, List<Long> attendeeIds) {
        Employee creator = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // 1. Team-wide Overlap Check
        List<Meeting> conflicts = meetingRepository.findOverlappingMeetings(
                creator.getTeamId(), meeting.getStartTime(), meeting.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Meeting time overlaps with team schedule");
        }

        // 2. Identify HR Presence & Check HR Availability
        boolean isHrRequired = false;
        if (attendeeIds != null && !attendeeIds.isEmpty()) {
            List<Employee> selectedAttendees = employeeRepository.findAllById(attendeeIds);

            for (Employee attendee : selectedAttendees) {
                if (attendee.getRole() == Role.HR) {
                    isHrRequired = true;
                    // Strict HR Overlap Check (across all teams)
                    if (meetingRepository.existsOverlappingMeetingForAttendee(
                            attendee.getId(), meeting.getStartTime(), meeting.getEndTime())) {
                        throw new BadRequestException("HR Representative " + attendee.getName() + " is already booked.");
                    }
                }
            }
            meeting.setAttendees(new HashSet<>(selectedAttendees));
        }

        // 3. Set Metadata
        meeting.setCreatedBy(employeeId);
        meeting.setTeamId(creator.getTeamId());
        meeting.setHrApprovalRequired(isHrRequired);

        // 4. State Machine Logic
        if (creator.getRole() == Role.TEAM_LEADER) {
            // Team Leads always start at PENDING (Manager's turn)
            meeting.setStatus(MeetingStatus.PENDING);
        } else if (creator.getRole() == Role.MANAGER) {
            // Managers skip team approval, but wait for HR if HR is invited
            meeting.setStatus(isHrRequired ? MeetingStatus.PENDING_HR_APPROVAL : MeetingStatus.SCHEDULED);
        } else if (creator.getRole() == Role.HR) {
            // HR can auto-schedule their own meetings
            meeting.setStatus(MeetingStatus.SCHEDULED);
        } else {
            throw new UnauthorizedException("Standard Employees cannot create meetings.");
        }

        return meetingRepository.save(meeting);
    }

    // Manager Approval Phase
    public Meeting approveByManager(Long meetingId, Long managerId) {
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (manager.getRole() != Role.MANAGER) {
            throw new UnauthorizedException("Only Managers can provide team-level approval");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getStatus() != MeetingStatus.PENDING) {
            throw new BadRequestException("Meeting is not awaiting Manager approval");
        }

        // Logic Bridge: If HR is needed, move to HR phase; otherwise, Schedule it.
        if (meeting.isHrApprovalRequired()) {
            meeting.setStatus(MeetingStatus.PENDING_HR_APPROVAL);
        } else {
            meeting.setStatus(MeetingStatus.SCHEDULED);
        }

        return meetingRepository.save(meeting);
    }

    // HR Approval Phase
    public Meeting approveByHr(Long meetingId, Long hrId) {
        Employee hr = employeeRepository.findById(hrId)
                .orElseThrow(() -> new ResourceNotFoundException("HR not found"));

        if (hr.getRole() != Role.HR) {
            throw new UnauthorizedException("Only an HR staff member can provide HR approval");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getStatus() != MeetingStatus.PENDING_HR_APPROVAL) {
            throw new BadRequestException("This meeting is not currently awaiting HR sign-off");
        }

        meeting.setStatus(MeetingStatus.SCHEDULED);
        return meetingRepository.save(meeting);
    }

    public Meeting rejectMeeting(Long meetingId, Long reviewerId) {
        Employee reviewer = employeeRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        // Validation: Only a Manager can reject a PENDING meeting,
        // and only HR can reject a PENDING_HR_APPROVAL meeting.
        boolean isAuthorized = (meeting.getStatus() == MeetingStatus.PENDING && reviewer.getRole() == Role.MANAGER) ||
                (meeting.getStatus() == MeetingStatus.PENDING_HR_APPROVAL && reviewer.getRole() == Role.HR);

        if (!isAuthorized) {
            throw new UnauthorizedException("You are not authorized to reject this meeting at this stage");
        }

        meeting.setStatus(MeetingStatus.REJECTED);
        return meetingRepository.save(meeting);
    }

    public Meeting cancelMeeting(Long meetingId, Long employeeId) {
        Employee user = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        // Creators, Managers, and HR can cancel scheduled or pending meetings
        if (user.getRole() == Role.MANAGER || user.getRole() == Role.HR || meeting.getCreatedBy().equals(employeeId)) {
            meeting.setStatus(MeetingStatus.CANCELLED);
            return meetingRepository.save(meeting);
        }

        throw new UnauthorizedException("You don't have permission to cancel this meeting");
    }
}