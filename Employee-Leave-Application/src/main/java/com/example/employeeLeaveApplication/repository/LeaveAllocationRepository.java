package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveAllocationRepository extends JpaRepository<LeaveAllocation, Long> {
    List<LeaveAllocation> findByEmployeeIdAndYear(Long employeeId, Integer year);

    Optional<LeaveAllocation> findByEmployeeIdAndLeaveCategoryAndYear(
            Long employeeId, String category, Integer year);

    // Get all allocations for a specific year (for year-end processing)
    List<LeaveAllocation> findByYear(Integer year);
}
