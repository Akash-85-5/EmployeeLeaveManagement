package com.emp_management.feature.leave.annual.repository;

import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.RequestStatus;
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

    // ── Basic finders ─────────────────────────────────────────────

    List<LeaveApplication> findByEmployee_EmpId(String empId);

    Page<LeaveApplication> findByEmployee_EmpId(String empId, Pageable pageable);

    List<LeaveApplication> findByEmployee_EmpIdAndStatus(String empId, RequestStatus status);

    List<LeaveApplication> findByEmployee_EmpIdAndYear(String empId, Integer year);

    List<LeaveApplication> findByStatus(RequestStatus status);
    Page<LeaveApplication> findByStatus(RequestStatus status, Pageable pageable);

    List<LeaveApplication> findByCurrentApproverId(String approverId);

    List<LeaveApplication> findByCurrentApproverIdAndStatus(String approverId, RequestStatus status);

    List<LeaveApplication> findByFirstApproverIdAndStatusAndCurrentApprovalLevel(
            String firstApproverId, RequestStatus status, ApprovalLevel level);

    List<LeaveApplication> findByStatusAndCurrentApprovalLevel(
            RequestStatus status, ApprovalLevel level);

    List<LeaveApplication> findByEscalatedTrueAndStatus(RequestStatus status);

    List<LeaveApplication> findByEscalatedTrue();

    List<LeaveApplication> findByStatusAndCreatedAtBeforeAndEscalatedFalse(
            RequestStatus status, LocalDateTime createdAt);

    List<LeaveApplication> findByEmployee_EmpIdInAndStatus(
            List<String> empIds, RequestStatus status);

    // ── Overlap check ─────────────────────────────────────────────

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employee.empId = :empId
          AND l.status IN ('PENDING', 'APPROVED')
          AND l.startDate <= :endDate
          AND l.endDate   >= :startDate
    """)
    List<LeaveApplication> findOverlappingLeaves(
            @Param("empId") String empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ── Balance queries ───────────────────────────────────────────

    @Query("""
        SELECT COALESCE(SUM(l.days), 0)
        FROM LeaveApplication l
        WHERE l.employee.empId = :empId
          AND l.status         = :status
          AND l.year           = :year
    """)
    Double getTotalUsedDays(
            @Param("empId") String empId,
            @Param("status") RequestStatus status,
            @Param("year") Integer year);

    @Query("""
        SELECT COALESCE(SUM(l.days), 0)
        FROM LeaveApplication l
        WHERE l.employee.empId    = :empId
          AND l.status            = :status
          AND l.year              = :year
          AND l.leaveType.leaveType = :leaveTypeName
    """)
    Double getTotalUsedDaysByType(
            @Param("empId") String empId,
            @Param("status") RequestStatus status,
            @Param("year") Integer year,
            @Param("leaveTypeName") String leaveTypeName);

    @Query("""
        SELECT COALESCE(SUM(l.days), 0)
        FROM LeaveApplication l
        WHERE l.employee.empId = :empId
          AND l.status         = 'APPROVED'
          AND YEAR(l.startDate)  = :year
          AND MONTH(l.startDate) = :month
    """)
    Double getTotalApprovedDaysInMonth(
            @Param("empId") String empId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    @Query("""
        SELECT COUNT(l)
        FROM LeaveApplication l
        WHERE l.employee.empId = :empId
          AND l.status         = 'APPROVED'
          AND YEAR(l.startDate)  = :year
          AND MONTH(l.startDate) = :month
    """)
    int countApprovedInMonth(
            @Param("empId") String empId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    // ── Status / year filters ─────────────────────────────────────

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employee.empId = :empId
          AND l.status         = :status
          AND l.year           = :year
    """)
    List<LeaveApplication> findByEmployee_EmpIdAndStatusAndYear(
            @Param("empId") String empId,
            @Param("status") RequestStatus status,
            @Param("year") Integer year);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.year   = :year
          AND l.status = :status
    """)
    List<LeaveApplication> findByYearAndStatus(
            @Param("year") Integer year,
            @Param("status") RequestStatus status);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE YEAR(l.startDate)  = :year
          AND MONTH(l.startDate) = :month
          AND l.status           = :status
    """)
    List<LeaveApplication> findByYearAndMonthAndStatus(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("status") RequestStatus status);

    // ── Dashboard helpers ─────────────────────────────────────────

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employee.empId = :empId
          AND l.status         = 'APPROVED'
          AND l.startDate      > :currentDate
        ORDER BY l.startDate ASC
    """)
    List<LeaveApplication> findUpcomingLeaves(
            @Param("empId") String empId,
            @Param("currentDate") LocalDate currentDate);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employee.empId = :empId
          AND l.status IN ('APPROVED', 'REJECTED')
        ORDER BY l.createdAt DESC
    """)
    Page<LeaveApplication> findRecentLeaves(
            @Param("empId") String empId,
            Pageable pageable);

    @Query("""
        SELECT COUNT(la) FROM LeaveApplication la
        WHERE la.employee.empId = :empId
          AND la.year           = :year
          AND la.status         = :status
    """)
    Integer countByStatus(
            @Param("empId") String empId,
            @Param("year") Integer year,
            @Param("status") RequestStatus status);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.status     = 'APPROVED'
          AND l.startDate <= :date
          AND l.endDate   >= :date
    """)
    List<LeaveApplication> findApprovedLeavesOnDate(@Param("date") LocalDate date);

    @Query("""
        SELECT DISTINCT l.employee.empId FROM LeaveApplication l
        WHERE l.status = 'APPROVED'
          AND :currentDate BETWEEN l.startDate AND l.endDate
    """)
    List<String> findEmployeesCurrentlyOnLeave(@Param("currentDate") LocalDate currentDate);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.status     = 'APPROVED'
          AND :startDate  <= l.endDate
          AND :endDate    >= l.startDate
        ORDER BY l.startDate ASC
    """)
    List<LeaveApplication> findApprovedLeavesInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE  l.employee.empId = :empId
          AND  l.status         = :status
          AND  l.startDate     <= :date
          AND  l.endDate       >= :date
    """)
    Optional<LeaveApplication> findApprovedLeaveForEmployeeOnDate(
            @Param("empId")   String empId,
            @Param("date")    LocalDate date,
            @Param("status") RequestStatus status);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.currentApproverId = :managerId
          AND l.startDate        <= :weekEnd
          AND l.endDate          >= :weekStart
    """)
    List<LeaveApplication> findTeamLeavesForWeek(
            @Param("managerId") String managerId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd")   LocalDate weekEnd);

    @Query("""
        SELECT l.leaveType.leaveType,
               COUNT(l),
               SUM(l.days)
        FROM LeaveApplication l
        WHERE l.employee.empId   = :empId
          AND l.status           = 'APPROVED'
          AND YEAR(l.startDate)  = :year
          AND MONTH(l.startDate) = :month
        GROUP BY l.leaveType.leaveType
    """)
    List<Object[]> getMonthlyStats(
            @Param("empId") String empId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.employee.empId IN
              (SELECT e.empId FROM  e WHERE e.currentApproverId = :managerId)
          AND l.status = 'PENDING'
        ORDER BY l.createdAt ASC
    """)
    List<LeaveApplication> findPendingTeamRequests(@Param("managerId") String managerId);



    // These go into your existing LeaveApplicationRepository


    // Managers who approved leaves — returns String empIds now
    @Query("""
    SELECT DISTINCT l.approvedBy FROM LeaveApplication l
    WHERE l.status      = 'APPROVED'
      AND l.approvedBy IS NOT NULL
      AND l.year        = :year
""")
    List<String> findManagersWhoApprovedLeaves(@Param("year") Integer year);

    @Query("""
    SELECT l FROM LeaveApplication l
    WHERE l.approvedBy = :managerId
      AND l.year       = :year
    ORDER BY l.approvedAt DESC
""")
    List<LeaveApplication> findLeavesApprovedByManager(
            @Param("managerId") String managerId,
            @Param("year") Integer year);
}
