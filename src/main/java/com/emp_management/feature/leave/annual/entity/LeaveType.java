package com.emp_management.feature.leave.annual.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_type")
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveTypeId;

    @Column(name = "leave_type", nullable = false, unique = true)
    private String leaveType;

    @Column(name = "allocated_days")
    private Double allocatedDays;

    @Column(name = "auto_allocate", nullable = false)
    private boolean autoAllocate = false;

    // --- getters / setters ---

    public Long getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public Double getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(Double allocatedDays) { this.allocatedDays = allocatedDays; }

    public boolean isAutoAllocate() { return autoAllocate; }
    public void setAutoAllocate(boolean autoAllocate) { this.autoAllocate = autoAllocate; }
}
