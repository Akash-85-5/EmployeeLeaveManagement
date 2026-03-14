package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.ODRequest;
import com.example.employeeLeaveApplication.enums.ODStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ODRequestRepository extends JpaRepository<ODRequest, Long> {
    @Query("SELECT o FROM ODRequest o WHERE o.employeeId = :employeeId " +
            "AND o.status NOT IN (com.example.employeeLeaveApplication.enums.ODStatus.REJECTED, " +
            "com.example.employeeLeaveApplication.enums.ODStatus.CANCELLED) " +
            "AND ((o.fromDate <= :toDate) AND (o.toDate >= :fromDate))")
    List<ODRequest> findOverlappingODs(
            @Param("employeeId") Long employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    List<ODRequest> findByEmployeeId(Long employeeId);

    List<ODRequest> findByStatus(ODStatus status);

    @Query("""
SELECT o FROM ODRequest o
JOIN Employee e ON o.employeeId = e.id
WHERE e.teamLeaderId = :tlId
AND o.status = com.example.employeeLeaveApplication.enums.ODStatus.PENDING_TEAM_LEADER
""")
    List<ODRequest> findPendingForTeamLeader(Long tlId);

    @Query("""
SELECT o FROM ODRequest o
JOIN Employee e ON o.employeeId = e.id
WHERE e.managerId = :managerId
AND o.status = com.example.employeeLeaveApplication.enums.ODStatus.PENDING_MANAGER
""")
    List<ODRequest> findPendingForManager(Long managerId);
}