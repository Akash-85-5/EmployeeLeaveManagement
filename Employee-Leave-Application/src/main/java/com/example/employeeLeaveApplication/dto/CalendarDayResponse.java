package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CalendarDayResponse {

    private LocalDate date;
    private int totalMeetings;
    private List<MeetingSlot> meetings;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MeetingSlot {
        private Long id;
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String type;
        private String locationOrLink;
        private String agenda;
        private String priority;
        private MeetingStatus status;
        private boolean isOrganizer;       // true if this employee created it
        private boolean hrApprovalRequired;
        private List<AttendeeInfo> attendees;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class AttendeeInfo {
        private Long id;
        private String name;
        private String role;
    }
}