package com.example.notificationservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_allocations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"employee_id", "allocation_year", "leave_category"})
        })
public class LeaveAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String leaveCategory; // SICK, CASUAL, etc.

    @Column(name = "allocation_year")
    private int year;

    @Column(nullable = false)
    private Double allocatedDays;

    @Column(nullable = false)
    private Double carriedForwardDays = 0.0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getLeaveCategory() {
        return leaveCategory;
    }

    public void setLeaveCategory(String leaveCategory) {
        this.leaveCategory = leaveCategory;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getAllocatedDays() {
        return allocatedDays;
    }

    public void setAllocatedDays(Double allocatedDays) {
        this.allocatedDays = allocatedDays;
    }

    public Double getCarriedForwardDays() {
        return carriedForwardDays;
    }

    public void setCarriedForwardDays(Double carriedForwardDays) {
        this.carriedForwardDays = carriedForwardDays;
    }
}
