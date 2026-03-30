package com.emp_management.feature.leave.annual.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_type")
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveTypeId;

    @Column(name = "leave_type")
    private String leaveType;
}
