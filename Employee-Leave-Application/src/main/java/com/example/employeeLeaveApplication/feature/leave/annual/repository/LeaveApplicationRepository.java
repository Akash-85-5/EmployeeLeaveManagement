package com.example.employeeLeaveApplication.feature.leave.annual.repository;

import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
import com.example.employeeLeaveApplication.shared.enums.ApprovalLevel;
import com.example.employeeLeaveApplication.shared.enums.LeaveStatus;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    List<LeaveApplication> findByManagerId(Long managerId);

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

    // ─────────────────────────────────────────────────────────────
    // Counts NUMBER OF APPLICATIONS approved in a month
    // Used for: general reporting, dashboard counts
    // NOT used for LOP calculation (use getTotalApprovedDaysInMonth)
    // ─────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────
    // ✅ NEW — Counts total DAYS approved in a month
    //
    // WHY THIS IS NEEDED:
    // countApprovedInMonth() counts applications (1, 2, 3...)
    // but monthly limit = 2 DAYS not 2 applications
    //
    // Example:
    //   Leave 1 → March 3 to March 5 = 3 days (1 application)
    //   Leave 2 → March 10           = 1 day  (1 application)
    //
    //   countApprovedInMonth = 2 applications → 2 > 2? ❌ NO LOP
    //   getTotalApprovedDaysInMonth = 4 days  → 4 > 2? ✅ LOP!
    //
    // Also handles half days correctly:
    //   Leave 1 → 0.5 days (half day)
    //   Leave 2 → 0.5 days (half day)
    //   Leave 3 → 1.5 days
    //   Total   → 2.5 days → 2.5 > 2 ✅ → excess = 0.5 → LOP = 0.50%
    //
    // Used in: LeaveApprovalService.finalizeLeave()
    //          for CF check and LOP calculation
    // ─────────────────────────────────────────────────────────────
    @Query("""
        SELECT COALESCE(SUM(l.days), 0)
        FROM LeaveApplication l
        WHERE l.employeeId = :employeeId
          AND l.status = 'APPROVED'
          AND YEAR(l.startDate) = :year
          AND MONTH(l.startDate) = :month
    """)
    Double getTotalApprovedDaysInMonth(
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
            "SELECT l FROM LeaveApplication l WHERE l.managerId = :managerId OR l.teamLeaderId = :managerId AND l.startDate <= :weekEnd AND l.endDate >= :weekStart"
    )
    List<LeaveApplication> findTeamLeavesForWeek(
            @Param("managerId") Long managerId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd
    );

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employeeId = :employeeId
        AND l.status = 'APPROVED'
        AND l.startDate > :currentDate
        ORDER BY l.startDate ASC
    """)
    List<LeaveApplication> findUpcomingLeaves(
            @Param("employeeId") Long employeeId,
            @Param("currentDate") LocalDate currentDate
    );

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

    @Query("SELECT COUNT(la) FROM LeaveApplication la WHERE la.employeeId = :employeeId AND la.year = :year AND la.status = :status")
    Integer countByStatus(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("status") LeaveStatus status
    );

    List<LeaveApplication> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

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
            @Param("endDate") LocalDate endDate
    );

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
    List<LeaveApplication> findLeavesApprovedByManager(
            @Param("managerId") Long managerId,
            @Param("year") Integer year
    );

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

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.year = :year AND l.status = :status
    """)
    List<LeaveApplication> findByYearAndStatus(
            @Param("year") Integer year,
            @Param("status") LeaveStatus status
    );

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
            Long teamLeaderId,
            LeaveStatus status,
            ApprovalLevel currentApprovalLevel
    );

    List<LeaveApplication> findByManagerIdAndStatusAndCurrentApprovalLevel(
            Long managerId,
            LeaveStatus status,
            ApprovalLevel currentApprovalLevel
    );

    List<LeaveApplication> findByStatusAndCurrentApprovalLevel(
            LeaveStatus status,
            ApprovalLevel currentApprovalLevel
    );

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE  l.employeeId = :empId
          AND  l.status     = :status
          AND  l.startDate  <= :date
          AND  l.endDate    >= :date
    """)
    Optional<LeaveApplication> findApprovedLeaveForEmployeeOnDate(
            @Param("empId")  Long        employeeId,
            @Param("date")   LocalDate   date,
            @Param("status") LeaveStatus status);
}