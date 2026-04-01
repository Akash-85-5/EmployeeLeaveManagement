package com.emp_management.feature.leave.compoff.repository;


import com.emp_management.feature.leave.compoff.entity.CompOff;
import com.emp_management.shared.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CompOffRepository extends JpaRepository<CompOff, Long> {

    boolean existsByEmployeeIdAndWorkedDate(String employeeId, LocalDate workedDate);

    @Query("SELECT SUM(c.days) FROM CompOff c WHERE c.employeeId = :employeeId AND c.status = :status")
    BigDecimal sumDaysByEmployeeAndStatus(@Param("employeeId") String employeeId, @Param("status") RequestStatus status);

    List<CompOff> findByEmployeeIdAndStatusOrderByWorkedDateAsc(String employeeId, RequestStatus status);


    // 🔄 Find the exact Comp-Off records linked to a specific leave application for reversal
    List<CompOff> findByUsedLeaveApplicationId(Long applicationId);


    Page<CompOff> findByreportingIdAndStatus(String managerId,RequestStatus status, Pageable pageable);

    Page<CompOff> findByEmployeeIdAndStatus(String employeeId, RequestStatus status, Pageable pageable);

    Page<CompOff> findByEmployeeId(String  employeeId, Pageable pageable);

    Page<CompOff> findByEmployeeIdAndYear(String employeeId, Integer year, Pageable pageable);

    List<CompOff> findListByEmployeeIdAndStatus(String  employeeId, RequestStatus status);

    @Query("""
    SELECT c
    FROM CompOff c
    JOIN Employee e ON e.id = c.employeeId
    WHERE e.reportingId = :managerId
    AND c.status = :status
""")
    Page<CompOff> findPendingByManager(
            String  managerId,
            RequestStatus status,
            Pageable pageable
    );

    /**
     * Get earned CompOff (status = EARNED)
     */
    @Query("SELECT SUM(c.days) FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'EARNED'")
    BigDecimal getTotalEarned(@Param("employeeId") String employeeId);

    /**
     * Get used CompOff (status = USED)
     */
    @Query("SELECT SUM(c.days) FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'USED'")
    BigDecimal getTotalUsed(@Param("employeeId") String employeeId);

    /**
     * Get pending CompOff approvals (status = PENDING)
     */
    @Query("SELECT COUNT(c) FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'PENDING'")
    Integer countPendingApprovals(@Param("employeeId") String employeeId);

    /**
     * Find earned CompOff in a year (for year-end processing)
     */
    @Query("SELECT c FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'EARNED' " +
            "AND YEAR(c.workedDate) = :year " +
            "ORDER BY c.workedDate DESC")
    List<CompOff> findEarnedInYear(@Param("employeeId") String employeeId, @Param("year") Integer year);

    /**
     * Find all CompOff records for an employee (all statuses) ordered by date
     */
    @Query("SELECT c FROM CompOff c " +
            "WHERE c.employeeId = :employeeId " +
            "ORDER BY c.workedDate DESC")
    List<CompOff> findAllByEmployeeId(@Param("employeeId") String  employeeId);

    /**
     * Find pending CompOff for employee (needs approval)
     */
    @Query("SELECT c FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'PENDING' " +
            "ORDER BY c.workedDate DESC")
    List<CompOff> findPendingByEmployeeId(@Param("employeeId") String employeeId);

    /**
     * Get detailed CompOff history with status breakdown
     */
    @Query("SELECT c FROM CompOff c " +
            "WHERE c.employeeId = :employeeId " +
            "ORDER BY c.workedDate DESC")
    List<CompOff> getCompOffHistory(@Param("employeeId") String  employeeId);
}
