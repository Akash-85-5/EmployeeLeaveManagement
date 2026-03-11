package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.MeetingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDateTime startTime;   // ✅ NEW
    private LocalDateTime endTime;     // ✅ NEW

    private String type;
    private String locationOrLink;
    private String agenda;
    private String priority;

    @Enumerated(EnumType.STRING)
    private MeetingStatus status;

    private Long createdBy;
    private Long teamId;

    @ManyToMany
    @JoinTable(
            name = "meeting_attendees",
            joinColumns = @JoinColumn(name = "meeting_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private Set<Employee> attendees;
}