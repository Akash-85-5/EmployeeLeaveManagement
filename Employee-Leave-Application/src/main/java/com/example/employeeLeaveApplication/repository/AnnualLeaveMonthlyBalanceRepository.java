package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.AnnualLeaveMonthlyBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnnualLeaveMonthlyBalanceRepository
        extends JpaRepository<AnnualLeaveMonthlyBalance, Long> {

    Optional<AnnualLeaveMonthlyBalance> findByEmployeeIdAndYearAndMonth(
            Long employeeId, Integer year, Integer month);

    List<AnnualLeaveMonthlyBalance> findByEmployeeIdAndYearOrderByMonthAsc(
            Long employeeId, Integer year);

    /**
     * Returns the latest month record for an employee in a year.
     * Used to find the last initialized month so we know where to continue.
     */
    @Query("""
        SELECT b FROM AnnualLeaveMonthlyBalance b
        WHERE b.employeeId = :empId
          AND b.year = :year
        ORDER BY b.month DESC
        LIMIT 1
    """)
    Optional<AnnualLeaveMonthlyBalance> findLatestByEmployeeIdAndYear(
            @Param("empId") Long employeeId,
            @Param("year") Integer year);

    /**
     * Sum of all used days in a year for carry-forward calculation.
     */
    @Query("""
        SELECT COALESCE(SUM(b.usedDays), 0)
        FROM AnnualLeaveMonthlyBalance b
        WHERE b.employeeId = :empId
          AND b.year = :year
    """)
    Double getTotalUsedDaysForYear(
            @Param("empId") Long employeeId,
            @Param("year") Integer year);
}