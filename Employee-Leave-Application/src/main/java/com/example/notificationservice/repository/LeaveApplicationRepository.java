package com.example.notificationservice.repository;

import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    List<LeaveApplication> findByEmployeeId(Long employeeId);

    List<LeaveApplication> findByEmployeeIdInAndStatus(
            List<Long> employeeIds,
            LeaveStatus status
    );
    List<LeaveApplication> findByStatusAndSubmittedAtBeforeAndEscalatedFalse(
            LeaveStatus status,
            LocalDateTime submittedAt

    );
    List<LeaveApplication>findByManagerId(
            Long managerId
    );
    List<LeaveApplication> findByManagerIdAndStatus(
            Long managerId,
            LeaveStatus status
    );
    List<LeaveApplication> findByEscalatedTrueAndStatus(LeaveStatus status);

    List<LeaveApplication> findByEscalatedTrue();
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
        AND l.status IN ('PENDING','APPROVED')
        AND l.startDate <= :endDate
        AND l.endDate >= :startDate
    """)
    List<LeaveApplication> findOverlappingLeaves(
            @Param("empId") Long empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    @Query(
            "SELECT l FROM LeaveApplication l " +
                    "WHERE l.managerId = :managerId " +
                    "AND l.startDate <= :weekEnd " +
                    "AND l.endDate >= :weekStart"
    )
    List<LeaveApplication> findTeamLeavesForWeek(
            @Param("managerId") Long managerId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd
    );





}
