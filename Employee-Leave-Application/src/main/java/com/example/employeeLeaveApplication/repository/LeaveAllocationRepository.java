package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveAllocationRepository extends JpaRepository<LeaveAllocation, Long> {

    List<LeaveAllocation> findByEmployeeIdAndYear(Long employeeId, Integer year);

    List<LeaveAllocation> findByYear(Integer year);

    // ✅ Fixed: LeaveType enum instead of String
    Optional<LeaveAllocation> findByEmployeeIdAndYearAndLeaveCategory(
            Long employeeId, Integer year, LeaveType leaveCategory);

    @Query("SELECT COALESCE(SUM(la.allocatedDays), 0.0) " +
            "FROM LeaveAllocation la " +
            "WHERE la.employeeId = :employeeId AND la.year = :year")
    Double getTotalAllocatedDays(@Param("employeeId") Long employeeId,
                                 @Param("year") Integer year);
}