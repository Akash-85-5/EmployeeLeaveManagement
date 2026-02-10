package com.example.notificationservice.repository;

import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    // Find all leaves for an employee
    List<LeaveApplication> findByEmployeeId(Long employeeId);

    // Find leaves for multiple employees with a specific status
    List<LeaveApplication> findByEmployeeIdInAndStatus(
            List<Long> employeeIds,
            LeaveStatus status
    );

    // Count approved leaves in a month (default APPROVED)
    @Query("""
        SELECT COUNT(l)
        FROM LeaveApplication l
        WHERE l.employeeId = :employeeId
          AND l.status = 'APPROVED'
          AND YEAR(l.startDate) = :year
          AND MONTH(l.startDate) = :month
    """)
    int countApprovedInMonth(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    // Monthly statistics: count and sum of leave days per type
    @Query("""
        SELECT l.leaveType,
               COUNT(l),
               SUM(l.days)
        FROM LeaveApplication l
        WHERE l.employeeId = :employeeId
          AND l.status = 'APPROVED'
          AND YEAR(l.startDate) = :year
          AND MONTH(l.startDate) = :month
        GROUP BY l.leaveType
    """)
    List<Object[]> getMonthlyStats(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    // Count leaves for multiple employees with specific status
    int countByEmployeeIdInAndStatus(
            List<Long> employeeIds,
            LeaveStatus status
    );

    // Find leaves by employee, status, and year
    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employeeId = :employeeId
          AND l.status = :status
          AND YEAR(l.startDate) = :year
    """)
    List<LeaveApplication> findByEmployeeIdAndStatusAndYear(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

    // Total used days by status and year
    @Query("""
        SELECT COALESCE(SUM(lr.days), 0)
        FROM LeaveApplication lr
        WHERE lr.employeeId = :empId
          AND lr.status = :status
          AND lr.year = :year
    """)
    Double getTotalUsedDays(
            @Param("empId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

    // Find overlapping leaves with PENDING and APPROVED status
    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employeeId = :empId
          AND l.status IN (:pending, :approved)
          AND l.startDate <= :endDate
          AND l.endDate >= :startDate
    """)
    List<LeaveApplication> findOverlappingLeaves(
            @Param("empId") Long empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("pending") LeaveStatus pending,
            @Param("approved") LeaveStatus approved
    );

}
