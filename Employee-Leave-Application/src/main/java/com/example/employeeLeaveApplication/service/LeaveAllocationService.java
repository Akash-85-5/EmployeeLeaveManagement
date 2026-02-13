package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.repository.LeaveAllocationRepository;
import org.springframework.stereotype.Service;

@Service
public class LeaveAllocationService {

    private final LeaveAllocationRepository leaveAllocationRepository;

    public LeaveAllocationService(LeaveAllocationRepository leaveAllocationRepository){
        this.leaveAllocationRepository=leaveAllocationRepository;
    }

    public LeaveAllocation createEmployeeAllocation(LeaveAllocation leaveAllocation) {
        return leaveAllocationRepository.save(leaveAllocation);
    }
}
