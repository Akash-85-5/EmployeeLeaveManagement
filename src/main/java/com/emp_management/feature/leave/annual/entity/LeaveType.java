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

    /**
     * If set, only employees of this gender are eligible.
     * NULL means no gender restriction.
     * Values: "MALE", "FEMALE"
     */
    @Column(name = "eligible_gender")
    private String eligibleGender;

    /**
     * If true, only married employees are eligible.
     * False or NULL means no marital restriction.
     */
    @Column(name = "married_only")
    private Boolean marriedOnly = false;

    public Long getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public Double getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(Double allocatedDays) { this.allocatedDays = allocatedDays; }

    public boolean isAutoAllocate() { return autoAllocate; }
    public void setAutoAllocate(boolean autoAllocate) { this.autoAllocate = autoAllocate; }

    public String getEligibleGender() { return eligibleGender; }
    public void setEligibleGender(String eligibleGender) { this.eligibleGender = eligibleGender; }

    public Boolean getMarriedOnly() { return marriedOnly; }
    public void setMarriedOnly(Boolean marriedOnly) { this.marriedOnly = marriedOnly; }
}
