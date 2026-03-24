package com.example.employeeLeaveApplication.feature.od.repository;

import com.example.employeeLeaveApplication.feature.od.entity.ODRequest;
import com.example.employeeLeaveApplication.shared.enums.ODStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ODRequestRepository extends JpaRepository<ODRequest, Long> {
    @Query("SELECT o FROM ODRequest o WHERE o.employeeId = :employeeId " +
            "AND o.status NOT IN (ODStatus.REJECTED, " + "ODStatus.CANCELLED) " +
            "AND ((o.startDate <= :toDate) AND (o.endDate >= :fromDate))")
    List<ODRequest> findOverlappingODs(Long employeeId,
                                       LocalDate fromDate,
                                       LocalDate toDate);

    List<ODRequest> findByEmployeeId(Long employeeId);

    List<ODRequest> findByStatus(ODStatus status);

    @Query("""
SELECT o FROM ODRequest o
JOIN Employee e ON o.employeeId = e.id
WHERE e.teamLeaderId = :tlId
AND o.status = ODStatus.PENDING_TEAM_LEADER
""")
    List<ODRequest> findPendingForTeamLeader(Long tlId);

    @Query("""
SELECT o FROM ODRequest o
JOIN Employee e ON o.employeeId = e.id
WHERE e.managerId = :managerId
AND o.status = ODStatus.PENDING_MANAGER
""")
    List<ODRequest> findPendingForManager(Long managerId);

    List<ODRequest> findByEmployeeIdAndStatus(Long employeeId, ODStatus status);
}