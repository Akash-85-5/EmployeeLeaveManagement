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
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final EmployeeRepository employeeRepository;

    public MeetingService(MeetingRepository meetingRepository,
                          EmployeeRepository employeeRepository) {
        this.meetingRepository = meetingRepository;
        this.employeeRepository = employeeRepository;
    }

    // ✅ Consolidated Create Meeting Logic
    public Meeting createMeeting(Long employeeId, Meeting meeting, List<Long> attendeeIds) {

        Employee creator = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // 1. Overlap Check
        List<Meeting> conflicts = meetingRepository.findOverlappingMeetings(
                creator.getTeamId(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Meeting time overlaps with another scheduled meeting");
        }

        // 2. Set Metadata
        meeting.setCreatedBy(employeeId);
        meeting.setTeamId(creator.getTeamId());

        // 3. Logic for selecting specific people (HR Selection Logic)
        if (attendeeIds != null && !attendeeIds.isEmpty()) {
            List<Employee> selectedAttendees = employeeRepository.findAllById(attendeeIds);
            meeting.setAttendees(new HashSet<>(selectedAttendees));
        }

        // 4. Role-based Status Logic
        if (creator.getRole() == Role.TEAM_LEADER) {
            meeting.setStatus(MeetingStatus.PENDING);
        }
        else if (creator.getRole() == Role.MANAGER || creator.getRole() == Role.HR) {
            meeting.setStatus(MeetingStatus.SCHEDULED);
        }
        else {
            throw new UnauthorizedException("You are not allowed to create a meeting");
        }

        return meetingRepository.save(meeting);
    }

    // ✅ Approve Meeting
    public Meeting approveMeeting(Long meetingId, Long managerId) {
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (manager.getRole() != Role.MANAGER) {
            throw new UnauthorizedException("Only Team Manager can approve meetings");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getStatus() != MeetingStatus.PENDING) {
            throw new BadRequestException("Only pending meetings can be approved");
        }

        meeting.setStatus(MeetingStatus.SCHEDULED);
        return meetingRepository.save(meeting);
    }

    // ✅ Reject Meeting
    public Meeting rejectMeeting(Long meetingId, Long managerId) {
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (manager.getRole() != Role.MANAGER) {
            throw new UnauthorizedException("Only Team Manager can reject meetings");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
            throw new BadRequestException("Approved meeting cannot be rejected");
        }

        meeting.setStatus(MeetingStatus.REJECTED);
        return meetingRepository.save(meeting);
    }

    // ✅ Cancel Meeting
    public Meeting cancelMeeting(Long meetingId, Long employeeId) {
        Employee user = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        Employee creator = employeeRepository.findById(meeting.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        if (user.getRole() == Role.MANAGER &&
                (creator.getRole() == Role.TEAM_LEADER || creator.getRole() == Role.MANAGER)) {
            meeting.setStatus(MeetingStatus.CANCELLED);
        }
        else if (user.getRole() == Role.HR && creator.getRole() == Role.HR) {
            meeting.setStatus(MeetingStatus.CANCELLED);
        } else {
            throw new UnauthorizedException("You are not allowed to cancel this meeting");
        }

        return meetingRepository.save(meeting);
    }
//    public List<Meeting> getPendingMeeting(Long managerId){
//        return meetingRepository.findByManagerId(managerId);
//    }
}