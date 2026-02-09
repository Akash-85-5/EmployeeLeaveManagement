package com.example.notificationservice.service;

import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.entity.LeaveAllocation;
import com.example.notificationservice.repository.LeaveAllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
