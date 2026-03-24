package com.example.employeeLeaveApplication.feature.od.repository;

import com.example.employeeLeaveApplication.feature.od.entity.ODRequest;
import com.example.employeeLeaveApplication.shared.enums.ODStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ODRequestRepository extends JpaRepository<ODRequest, Long> {

    @Query("""
        SELECT o FROM ODRequest o
        WHERE o.employeeId = :employeeId
        AND o.status NOT IN (com.example.employeeLeaveApplication.shared.enums.ODStatus.REJECTED,
                             com.example.employeeLeaveApplication.shared.enums.ODStatus.CANCELLED)
        AND (o.startDate <= :toDate AND o.endDate >= :fromDate)
        """)
    List<ODRequest> findOverlappingODs(Long employeeId, LocalDate fromDate, LocalDate toDate);

    List<ODRequest> findByEmployeeId(Long employeeId);

    List<ODRequest> findByStatus(ODStatus status);

    // All PENDING ODs where this manager is the current approver
    List<ODRequest> findByCurrentApproverIdAndStatus(Long currentApproverId, ODStatus status);

    List<ODRequest> findByEmployeeIdAndStatus(Long employeeId, ODStatus status);
}