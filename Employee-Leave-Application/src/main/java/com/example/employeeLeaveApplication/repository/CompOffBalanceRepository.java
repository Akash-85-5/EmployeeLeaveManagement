package com.example.employeeLeaveApplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.employeeLeaveApplication.entity.CompOffBalance;

@Repository
public interface CompOffBalanceRepository extends JpaRepository<CompOffBalance, Long> {

    /**
     * Find by employee ID and year (lop_year column)
     */
    Optional<CompOffBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Find all comp-off balances for an employee
     */
    List<CompOffBalance> findByEmployeeId(Long employeeId);

    /**
     * Get total available comp-off balance across all years
     */
    @Query("SELECT SUM(c.balance) FROM CompOffBalance c WHERE c.employeeId = :employeeId")
    Double getTotalAvailableBalance(@Param("employeeId") Long employeeId);
}