package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.Meeting;
import com.example.employeeLeaveApplication.service.MeetingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    // ✅ Create Meeting (Supports HR selecting specific attendees)
    @PostMapping("/create/{employeeId}")
    public Meeting createMeeting(
            @PathVariable Long employeeId,
            @RequestBody Meeting meeting,
            @RequestParam(required = false) List<Long> attendeeIds) {
        // Logic: Send attendeeIds as ?attendeeIds=111,112 in the URL
        return meetingService.createMeeting(employeeId, meeting, attendeeIds);
    }

    // ✅ Approve Meeting (Manager Only)
    @PutMapping("/approve/{meetingId}/{managerId}")
    public Meeting approveMeeting(
            @PathVariable Long meetingId,
            @PathVariable Long managerId) {
        return meetingService.approveMeeting(meetingId, managerId);
    }

    // ✅ Reject Meeting (Manager Only)
    @PutMapping("/reject/{meetingId}/{managerId}")
    public Meeting rejectMeeting(
            @PathVariable Long meetingId,
            @PathVariable Long managerId) {
        return meetingService.rejectMeeting(meetingId, managerId);
    }

    // ✅ Cancel Meeting (Manager or HR)
    @DeleteMapping("/cancel/{meetingId}/{employeeId}")
    public Meeting cancelMeeting(
            @PathVariable Long meetingId,
            @PathVariable Long employeeId) {
        return meetingService.cancelMeeting(meetingId, employeeId);
    }
}