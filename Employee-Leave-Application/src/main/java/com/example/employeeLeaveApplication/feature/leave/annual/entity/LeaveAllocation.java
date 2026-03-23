package com.example.employeeLeaveApplication.feature.leave.annual.entity;

import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import jakarta.persistence.*;

@Entity
@Table(name = "leave_allocation")
public class LeaveAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_category", nullable = false)
    private LeaveType leaveCategory;

    @Column(name = "allocation_year", nullable = false)
    private Integer year;

    @Column(name = "allocated_days", nullable = false)
    private Double allocatedDays;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public LeaveType getLeaveCategory() { return leaveCategory; }
    public void setLeaveCategory(LeaveType leaveCategory) { this.leaveCategory = leaveCategory; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Double getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(Double allocatedDays) { this.allocatedDays = allocatedDays; }
}