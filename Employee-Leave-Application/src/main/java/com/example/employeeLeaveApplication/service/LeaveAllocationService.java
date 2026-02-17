package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.LeaveAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
public class LeaveAllocationService {

    private final LeaveAllocationRepository leaveAllocationRepository;

    public LeaveAllocationService(LeaveAllocationRepository leaveAllocationRepository) {
        this.leaveAllocationRepository = leaveAllocationRepository;
    }


    public LeaveAllocation createEmployeeAllocation(LeaveAllocation leaveAllocation) {
        return leaveAllocationRepository.save(leaveAllocation);
    }


    /**
     * Get employee allocations for a specific year (or current year)
     */
    public List<LeaveAllocation> getEmployeeAllocations(Long employeeId, Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }

        return leaveAllocationRepository.findByEmployeeIdAndYear(employeeId, year);
    }

    /**
     * Update leave allocation
     */
    @Transactional
    public LeaveAllocation updateAllocation(Long id, LeaveAllocation allocation) {
        LeaveAllocation existing = leaveAllocationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Leave allocation not found with ID: " + id));

        // Update fields if provided
        if (allocation.getAllocatedDays() != null) {
            existing.setAllocatedDays(allocation.getAllocatedDays());
        }
        if (allocation.getCarriedForwardDays() != null) {
            existing.setCarriedForwardDays(allocation.getCarriedForwardDays());
        }
        if (allocation.getLeaveCategory() != null) {
            existing.setLeaveCategory(allocation.getLeaveCategory());
        }

        return leaveAllocationRepository.save(existing);
    }


    /**
     * Delete allocation
     */
    @Transactional
    public void deleteAllocation(Long id) {
        LeaveAllocation allocation = leaveAllocationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Leave allocation not found with ID: " + id));

        leaveAllocationRepository.delete(allocation);
    }

    /**
     * Get all allocations for a year (admin use)
     */
    public List<LeaveAllocation> getAllocationsByYear(Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }

        return leaveAllocationRepository.findByYear(year);
    }

    /**
     * Create bulk allocations for new employee
     */
    @Transactional
    public List<LeaveAllocation> createBulkAllocations(Long employeeId, Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }

        // Check if allocations already exist
        List<LeaveAllocation> existing = leaveAllocationRepository.findByEmployeeIdAndYear(employeeId, year);
        if (!existing.isEmpty()) {
            throw new BadRequestException("Allocations already exist for employee " + employeeId + " in year " + year);
        }

        // Standard categories with default allocations
        String[] categories = {"SICK", "CASUAL", "EARNED", "PERSONAL"};
        double[] allocations = {12.0, 6.0, 4.0, 2.0};  // Total = 24

        List<LeaveAllocation> created = new java.util.ArrayList<>();

        for (int i = 0; i < categories.length; i++) {
            LeaveAllocation alloc = new LeaveAllocation();
            alloc.setEmployeeId(employeeId);
            alloc.setLeaveCategory(categories[i]);
            alloc.setYear(year);
            alloc.setAllocatedDays(allocations[i]);
            alloc.setCarriedForwardDays(0.0);

            created.add(leaveAllocationRepository.save(alloc));
        }

        return created;
    }
}