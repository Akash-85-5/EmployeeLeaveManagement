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

    // ✅ Create Meeting (Checks team and HR availability)
    @PostMapping("/create/{employeeId}")
    public Meeting createMeeting(
            @PathVariable Long employeeId,
            @RequestBody Meeting meeting,
            @RequestParam(required = false) List<Long> attendeeIds) {
        return meetingService.createMeeting(employeeId, meeting, attendeeIds);
    }

    // ✅ Manager Approval (First level of approval)
    @PutMapping("/approve/manager/{meetingId}/{managerId}")
    public Meeting approveByManager(
            @PathVariable Long meetingId,
            @PathVariable Long managerId) {
        return meetingService.approveByManager(meetingId, managerId);
    }

    // ✅ NEW: HR Approval (Second level of approval, if HR is invited)
    @PutMapping("/approve/hr/{meetingId}/{hrId}")
    public Meeting approveByHr(
            @PathVariable Long meetingId,
            @PathVariable Long hrId) {
        return meetingService.approveByHr(meetingId, hrId);
    }

    // ✅ Reject Meeting (Supports rejection by Manager or HR based on current state)
    @PutMapping("/reject/{meetingId}/{reviewerId}")
    public Meeting rejectMeeting(
            @PathVariable Long meetingId,
            @PathVariable Long reviewerId) {
        return meetingService.rejectMeeting(meetingId, reviewerId);
    }

    // ✅ Cancel Meeting (Manager, HR, or Creator)
    @DeleteMapping("/cancel/{meetingId}/{employeeId}")
    public Meeting cancelMeeting(
            @PathVariable Long meetingId,
            @PathVariable Long employeeId) {
        return meetingService.cancelMeeting(meetingId, employeeId);
    }
}