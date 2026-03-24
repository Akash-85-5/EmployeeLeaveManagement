package com.example.employeeLeaveApplication.feature.access.repository;

import com.example.employeeLeaveApplication.feature.access.entity.AccessRequest;
import com.example.employeeLeaveApplication.shared.enums.AccessRequestStatus;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    /**
     * Find requests by employee ID
     */
    List<AccessRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    /**
     * Find requests by employee and type (VPN or BIOMETRIC)
     */
    Optional<AccessRequest> findByEmployeeIdAndAccessType(Long employeeId, LeaveType type);

    /**
     * Find all SUBMITTED requests (waiting for manager review)
     */
    List<AccessRequest> findByStatusOrderBySubmittedAtAsc(AccessRequestStatus status);

    /**
     * Find submitted requests for a specific manager
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "WHERE ar.status = 'SUBMITTED' AND ar.reportingId = :reportingId " +
            "ORDER BY ar.submittedAt ASC")
    List<AccessRequest> findPendingForManager(@Param("reportingId") Long reportingId);

    /**
     * Find manager-approved requests waiting for admin (MANAGER_APPROVED status)
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "WHERE ar.status = 'MANAGER_APPROVED' " +
            "ORDER BY ar.managerDecisionAt ASC")
    List<AccessRequest> findManagerApprovedRequests();

    /**
     * Find all requests by manager ID (for manager's dashboard)
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "WHERE ar.reportingId = :reportingId " +
            "ORDER BY ar.createdAt DESC")
    List<AccessRequest> findByreportingId(@Param("reportingId") Long reportingId);

    /**
     * Count pending requests for a manager
     */
    @Query("SELECT COUNT(ar) FROM AccessRequest ar " +
            "WHERE ar.status = 'SUBMITTED' AND ar.reportingId = :reportingId")
    Long countPendingForManager(@Param("reportingId") Long reportingId);

    /**
     * Count pending admin approvals (manager approved, waiting for admin)
     */
    @Query("SELECT COUNT(ar) FROM AccessRequest ar " +
            "WHERE ar.status = 'MANAGER_APPROVED'")
    Long countPendingAdminApprovals();
}