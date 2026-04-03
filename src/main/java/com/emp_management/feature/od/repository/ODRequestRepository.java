package com.emp_management.feature.od.repository;

import com.emp_management.feature.od.entity.ODRequest;
import com.emp_management.shared.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ODRequestRepository extends JpaRepository<ODRequest, Long> {

    // ── Basic finders ─────────────────────────────────────────────

    List<ODRequest> findByEmployee_EmpId(String empCode);

    List<ODRequest> findByEmployee_EmpIdAndStatus(String empCode, RequestStatus status);

    List<ODRequest> findByCurrentApproverIdAndStatus(String approverId, RequestStatus status);

    List<ODRequest> findByStatus(RequestStatus status);

    List<ODRequest> findByEscalatedTrueAndStatus(RequestStatus status);

    List<ODRequest> findByEscalatedTrue();

    // ── Escalation: find pending ODs not yet escalated, created before a threshold ──
    List<ODRequest> findByStatusAndCreatedAtBeforeAndEscalatedFalse(
            RequestStatus status, LocalDateTime createdAt);

    // ── Concept #8: Overlap check ─────────────────────────────────
    /**
     * Returns all non-rejected, non-cancelled OD requests that overlap with the given date range.
     * If result is non-empty, reject the new OD creation.
     */
    @Query("""
        SELECT od FROM ODRequest od
        WHERE od.employee.empId = :empCode
          AND od.status IN ('PENDING', 'APPROVED')
          AND od.startDate <= :endDate
          AND od.endDate   >= :startDate
    """)
    List<ODRequest> findOverlappingODs(@Param("empCode") String empCode,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    // ── Concept #10: Block OD on dates with approved leave ────────
    /**
     * Returns true if the employee has any APPROVED leave that overlaps with the given date range.
     */
    @Query("""
        SELECT CASE WHEN COUNT(la) > 0 THEN TRUE ELSE FALSE END
        FROM LeaveApplication la
        WHERE la.employee.empId = :empCode
          AND la.status = com.emp_management.shared.enums.RequestStatus.APPROVED
          AND la.startDate <= :endDate
          AND la.endDate   >= :startDate
    """)
    Boolean hasApprovedLeaveInRange(@Param("empCode") String empCode,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
}