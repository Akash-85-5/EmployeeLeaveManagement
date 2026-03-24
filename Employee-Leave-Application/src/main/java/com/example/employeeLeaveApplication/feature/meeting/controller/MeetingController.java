//package com.example.employeeLeaveApplication.feature.meeting.controller;
//
//import com.example.employeeLeaveApplication.feature.meeting.dto.CalendarDayResponse;
//import com.example.employeeLeaveApplication.feature.meeting.entity.Meeting;
//import com.example.employeeLeaveApplication.feature.meeting.service.MeetingService;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/meetings")
//public class MeetingController {
//
//    private final MeetingService meetingService;
//
//    public MeetingController(MeetingService meetingService) {
//        this.meetingService = meetingService;
//    }
//
//    @PostMapping("/create/{employeeId}")
//    public Meeting createMeeting(
//            @PathVariable Long employeeId,
//            @RequestBody Meeting meeting,
//            @RequestParam(required = false) List<Long> attendeeIds) {
//        // Logic: Send attendeeIds as ?attendeeIds=111,112 in the URL
//        return meetingService.createMeeting(employeeId, meeting, attendeeIds);
//    }
//
////    @GetMapping("/pending/{managerId}")
////    public List<Meeting> pendingMeetingRequest(@PathVariable Long managerId){
////        return meetingService.getPendingMeeting(managerId);
////    }
//    @PutMapping("/approve/manager/{meetingId}/{managerId}")
//    public Meeting approveByManager(
//            @PathVariable Long meetingId,
//            @PathVariable Long managerId) {
//        return meetingService.approveByManager(meetingId, managerId);
//    }
//
//    // ✅ NEW: HR Approval (Second level of approval, if HR is invited)
//    @PutMapping("/approve/hr/{meetingId}/{hrId}")
//    public Meeting approveByHr(
//            @PathVariable Long meetingId,
//            @PathVariable Long hrId) {
//        return meetingService.approveByHr(meetingId, hrId);
//    }
//
//    // ✅ Reject Meeting (Supports rejection by Manager or HR based on current state)
//    // In MeetingController
//    @PatchMapping("/{meetingId}/reject")
//    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
//    public ResponseEntity<Meeting> rejectMeeting(
//            @PathVariable Long meetingId,
//            @RequestParam Long reviewerId,
//            @RequestParam(required = false) String reason) {
//        return ResponseEntity.ok(meetingService.rejectMeeting(meetingId, reviewerId, reason));
//    }
//
//    // ✅ Cancel Meeting (Manager or HR)
//    @DeleteMapping("/cancel/{meetingId}/{employeeId}")
//    public Meeting cancelMeeting(
//            @PathVariable Long meetingId,
//            @PathVariable Long employeeId) {
//        return meetingService.cancelMeeting(meetingId, employeeId);
//    }
//
//    @GetMapping("/my/{employeeId}")
//    public List<Meeting> getMyMeetings(@PathVariable Long employeeId) {
//        return meetingService.getMeetingsCreatedBy(employeeId);
//    }
//
//    @GetMapping("/pending/manager/{managerId}")
//    public List<Meeting> getPendingForManager(@PathVariable Long managerId) {
//        return meetingService.getPendingForManager(managerId);
//    }
//
//    @GetMapping("/pending/hr")
//    public List<Meeting> getPendingForHr() {
//        return meetingService.getPendingForHr();
//    }
//
//    // GET /api/meetings/calendar/day?employeeId=5&date=2025-07-15
//    @GetMapping("/calendar/day")
//    public ResponseEntity<CalendarDayResponse> getDayCalendar(
//            @RequestParam Long employeeId,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
//        return ResponseEntity.ok(meetingService.getDayCalendar(employeeId, date));
//    }
//}