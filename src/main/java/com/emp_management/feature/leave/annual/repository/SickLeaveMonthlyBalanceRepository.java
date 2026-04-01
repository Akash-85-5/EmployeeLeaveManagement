package com.emp_management.feature.leave.annual.repository;

import com.emp_management.feature.leave.annual.entity.SickLeaveMonthlyBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SickLeaveMonthlyBalanceRepository
        extends JpaRepository<SickLeaveMonthlyBalance, Long> {

    Optional<SickLeaveMonthlyBalance> findByEmployeeIdAndYearAndMonth(
            String employeeId, Integer year, Integer month);

    List<SickLeaveMonthlyBalance> findByEmployeeIdAndYearOrderByMonthAsc(
            String employeeId, Integer year);

    /**
     * Latest month record for an employee in a year.
     */
    @Query("""
        SELECT b FROM SickLeaveMonthlyBalance b
        WHERE b.employeeId = :empId
          AND b.year = :year
        ORDER BY b.month DESC
        LIMIT 1
    """)
    Optional<SickLeaveMonthlyBalance> findLatestByEmployeeIdAndYear(
            @Param("empId") String employeeId,
            @Param("year") Integer year);
}