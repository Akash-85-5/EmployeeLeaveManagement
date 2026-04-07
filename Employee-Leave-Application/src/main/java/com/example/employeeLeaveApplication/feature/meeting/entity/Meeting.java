//package com.example.employeeLeaveApplication.feature.meeting.entity;
//
//import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
//import com.example.employeeLeaveApplication.shared.enums.MeetingStatus;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//import java.util.Set;
//
//@Entity
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class Meeting {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String title;
//
//    private LocalDateTime startTime;
//    private LocalDateTime endTime;
//
//    private String type;
//    private String locationOrLink;
//    private String agenda;
//    private String priority;
//
//    @Enumerated(EnumType.STRING)
//    private MeetingStatus status;
//
//    private Long createdBy;
//    private Long teamId;
//
//    // ✅ NEW: Flag to track if the meeting requires HR's specific approval
//    private boolean hrApprovalRequired;
//
//    @ManyToMany
//    @JoinTable(
//            name = "meeting_attendees",
//            joinColumns = @JoinColumn(name = "meeting_id"),
//            inverseJoinColumns = @JoinColumn(name = "employee_id")
//    )
//    private Set<Employee> attendees;
//}