package com.emp_management.feature.od.repository;

import com.emp_management.feature.od.entity.ODRequest;
import com.emp_management.shared.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ODRequestRepository extends JpaRepository<ODRequest, Long> {

    // ── Basic finders ─────────────────────────────────────────────

    List<ODRequest> findByEmployee_EmpId(String empId);

    Page<ODRequest> findByEmployee_EmpId(String empId, Pageable pageable);

    List<ODRequest> findByEmployee_EmpIdAndStatus(String empId, RequestStatus status);

    List<ODRequest> findByStatus(RequestStatus status);

    List<ODRequest> findByCurrentApproverId(String approverId);

    List<ODRequest> findByCurrentApproverIdAndStatus(String approverId, RequestStatus status);

    List<ODRequest> findByEscalatedTrueAndStatus(RequestStatus status);

    List<ODRequest> findByEscalatedTrue();

    // ── Pending that have not been escalated yet, created before a cutoff ──
    List<ODRequest> findByStatusAndCreatedAtBeforeAndEscalatedFalse(
            RequestStatus status, LocalDateTime createdAt);

    // ── Overlap check (mirrors LeaveApplicationRepository) ────────
    @Query("""
        SELECT o FROM ODRequest o
        WHERE o.employee.empId = :empId
          AND o.status IN ('PENDING', 'APPROVED')
          AND o.startDate <= :endDate
          AND o.endDate   >= :startDate
    """)
    List<ODRequest> findOverlappingODs(
            @Param("empId") String empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ── Total approved OD days for a given employee + year ────────
    @Query("""
        SELECT COALESCE(SUM(o.days), 0)
        FROM ODRequest o
        WHERE o.employee.empId = :empId
          AND o.status         = 'APPROVED'
          AND o.year           = :year
    """)
    Double getTotalApprovedDays(
            @Param("empId") String empId,
            @Param("year") Integer year);
}