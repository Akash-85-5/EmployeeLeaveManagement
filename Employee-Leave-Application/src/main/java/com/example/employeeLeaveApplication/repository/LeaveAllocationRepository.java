package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveAllocationRepository extends JpaRepository<LeaveAllocation, Long> {
    List<LeaveAllocation> findByEmployeeIdAndYear(Long employeeId, Integer year);

    // Get all allocations for a specific year (for year-end processing)
    List<LeaveAllocation> findByYear(Integer year);

    @Query("SELECT COALESCE(SUM(la.allocatedDays), 0.0) " +
            "FROM LeaveAllocation la " +
            "WHERE la.employeeId = :employeeId AND la.year = :year")
    Double getTotalAllocatedDays(@Param("employeeId") Long employeeId,
                                 @Param("year") Integer year);

    Optional<LeaveAllocation> findByEmployeeIdAndYearAndLeaveCategory(
            Long employeeId, Integer year, String leaveCategory);
}
