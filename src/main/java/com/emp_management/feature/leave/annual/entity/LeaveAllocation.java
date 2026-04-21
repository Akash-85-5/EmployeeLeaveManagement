package com.emp_management.feature.leave.annual.entity;

import com.emp_management.feature.employee.entity.Employee;
import jakarta.persistence.*;

@Entity
@Table(name = "leave_allocation")
public class LeaveAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveCategory;

    @Column(name = "allocation_year", nullable = false)
    private Integer year;

    @Column(name = "allocated_days", nullable = false)
    private Double allocatedDays;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LeaveType getLeaveCategory() { return leaveCategory; }
    public void setLeaveCategory(LeaveType leaveCategory) { this.leaveCategory = leaveCategory; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Double getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(Double allocatedDays) { this.allocatedDays = allocatedDays; }
}