package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long>, JpaSpecificationExecutor<LeaveApplication> {

    List<LeaveApplication> findByEmployeeId(Long employeeId);

    List<LeaveApplication> findByEmployeeIdInAndStatus(
            List<Long> employeeIds,
            LeaveStatus status
    );

    // ===== NEW: Pagination support =====
    List<LeaveApplication> findByStatus(LeaveStatus status);

    Page<LeaveApplication> findByStatus(LeaveStatus status, Pageable pageable);

    Page<LeaveApplication> findByEmployeeId(Long employeeId, Pageable pageable);

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
            Long employeeId,
            Integer year,
            Integer month
    );

    @Query("""
    SELECT COUNT(l)
    FROM LeaveApplication l
    WHERE l.employeeId = :employeeId
      AND l.status = 'APPROVED'
      AND YEAR(l.startDate) = :year
      AND MONTH(l.startDate) = :month
""")
    int countApprovedInMonth(
            Long employeeId,
            Integer year,
            Integer month
    );

    int countByEmployeeIdInAndStatus(
            List<Long> employeeIds,
            LeaveStatus status
    );

    @Query("""
        SELECT l
        FROM LeaveApplication l
        WHERE l.employeeId = :employeeId
          AND l.status = :status
          AND YEAR(l.startDate) = :year
    """)
    List<LeaveApplication> findByEmployeeIdAndStatusAndYear(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

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

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employeeId = :empId
        AND l.status IN ('APPLIED','APPROVED')
        AND l.startDate <= :endDate
        AND l.endDate >= :startDate
    """)
    List<LeaveApplication> findOverlappingLeaves(
            @Param("empId") Long empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ===== NEW: Additional queries for dashboard =====
    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employeeId = :employeeId
        AND l.status = 'APPROVED'
        AND l.startDate > :currentDate
        ORDER BY l.startDate ASC
    """)
    List<LeaveApplication> findUpcomingLeaves(@Param("employeeId") Long employeeId, @Param("currentDate") LocalDate currentDate);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employeeId = :employeeId
        AND l.status IN ('APPROVED', 'REJECTED')
        ORDER BY l.createdAt DESC
    """)
    List<LeaveApplication> findRecentLeaves(@Param("employeeId") Long employeeId, Pageable pageable);

    // For "Who's Out" feature
    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.status = 'APPROVED'
        AND l.startDate <= :date
        AND l.endDate >= :date
    """)
    List<LeaveApplication> findApprovedLeavesOnDate(@Param("date") LocalDate date);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.status = 'APPROVED'
        AND l.startDate <= :endDate
        AND l.endDate >= :startDate
    """)
    List<LeaveApplication> findApprovedLeavesInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}