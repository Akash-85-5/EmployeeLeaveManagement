package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.ApprovalLevel;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<LeaveApplication> findByStatusAndCreatedAtBeforeAndEscalatedFalse(
            LeaveStatus status,
            LocalDateTime createdAt
    );

    List<LeaveApplication> findByStatus(LeaveStatus status);
    Page<LeaveApplication> findByStatus(LeaveStatus status, Pageable pageable);

    Page<LeaveApplication> findByEmployeeId(Long employeeId, Pageable pageable);

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
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("month") Integer month
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
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("month") Integer month
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
    Page<LeaveApplication> findRecentLeaves(
            @Param("employeeId") Long employeeId,
            Pageable pageable
    );

    @Query("SELECT COUNT(la) FROM LeaveApplication la " +
            "WHERE la.employeeId = :employeeId " +
            "AND la.year = :year " +
            "AND la.status = :status")
    Integer countByStatus(@Param("employeeId") Long employeeId,
                          @Param("year") Integer year,
                          @Param("status") LeaveStatus status);

    List<LeaveApplication> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    // For "Who's Out" feature
    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.status = 'APPROVED'
        AND l.startDate <= :date
        AND l.endDate >= :date
    """)
    List<LeaveApplication> findApprovedLeavesOnDate(@Param("date") LocalDate date);

    List<LeaveApplication> findByEmployeeIdAndYear(Long employeeId, Integer year);

    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employeeId IN " +
            "(SELECT e.id FROM Employee e WHERE e.managerId = :managerId) " +
            "AND la.status = 'PENDING' " +
            "ORDER BY la.createdAt ASC")
    List<LeaveApplication> findPendingTeamRequests(@Param("managerId") Long managerId);

    @Query("SELECT DISTINCT la.employeeId FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND :currentDate BETWEEN la.startDate AND la.endDate")
    List<Long> findEmployeesCurrentlyOnLeave(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND (:startDate <= la.endDate AND :endDate >= la.startDate) " +
            "ORDER BY la.startDate ASC")
    List<LeaveApplication> findApprovedLeavesInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT la.approvedBy FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND la.approvedRole = 'MANAGER' " +
            "AND la.year = :year")
    List<Long> findManagersWhoApprovedLeaves(@Param("year") Integer year);

    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND la.approvedRole = 'MANAGER' " +
            "AND la.year = :year " +
            "ORDER BY la.approvedAt DESC")
    List<LeaveApplication> findLeavesApprovedByManagers(@Param("year") Integer year);

    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND la.approvedBy = :managerId " +
            "AND la.year = :year " +
            "ORDER BY la.approvedAt DESC")
    List<LeaveApplication> findLeavesApprovedByManager(@Param("managerId") Long managerId, @Param("year") Integer year);

    @Query("""
    SELECT COALESCE(SUM(lr.days), 0)
    FROM LeaveApplication lr
    WHERE lr.employeeId = :empId
      AND lr.status = :status
      AND lr.year = :year
      AND lr.leaveType = :leaveType
""")
    Double getTotalUsedDaysByType(
            @Param("empId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("year") Integer year,
            @Param("leaveType") LeaveType leaveType
    );

    // For getAnnualLeaveSummary and getLeaveTypeDistribution
    @Query("""
    SELECT l FROM LeaveApplication l
    WHERE l.year = :year AND l.status = :status
""")
    List<LeaveApplication> findByYearAndStatus(
            @Param("year") Integer year,
            @Param("status") LeaveStatus status
    );

    // For getMonthlyReport
    @Query("""
    SELECT l FROM LeaveApplication l
    WHERE YEAR(l.startDate) = :year
      AND MONTH(l.startDate) = :month
      AND l.status = :status
""")
    List<LeaveApplication> findByYearAndMonthAndStatus(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("status") LeaveStatus status
    );

    List<LeaveApplication> findByTeamLeaderIdAndStatusAndCurrentApprovalLevel(
            Long teamLeaderId, LeaveStatus status, ApprovalLevel currentApprovalLevel);

    // Pending leaves at MANAGER level for a specific manager
    List<LeaveApplication> findByManagerIdAndStatusAndCurrentApprovalLevel(
            Long managerId, LeaveStatus status, ApprovalLevel currentApprovalLevel);

    // Pending leaves at HR level (any HR can see these)
    List<LeaveApplication> findByStatusAndCurrentApprovalLevel(
            LeaveStatus status, ApprovalLevel currentApprovalLevel);
}