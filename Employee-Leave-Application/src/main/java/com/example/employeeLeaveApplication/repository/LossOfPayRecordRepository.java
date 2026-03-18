package com.example.employeeLeaveApplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.employeeLeaveApplication.entity.LossOfPayRecord;

@Repository
public interface LossOfPayRecordRepository extends JpaRepository<LossOfPayRecord, Long> {

    /**
     * Find LOP record for specific employee, year, and month
     */
    Optional<LossOfPayRecord> findByEmployeeIdAndYearAndMonth(
            Long employeeId,
            Integer year,
            Integer month
    );

    /**
     * Get all LOP records for an employee ordered by year and month descending
     */
    List<LossOfPayRecord> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);

    /**
     * Get all LOP records for an employee in a specific year
     */
    List<LossOfPayRecord> findByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Get total LOP percentage for employee in a year
     */
    @Query("SELECT SUM(l.lossPercentage) FROM LossOfPayRecord l " +
            "WHERE l.employeeId = :employeeId AND l.year = :year")
    Double getTotalLossPercentageByEmployeeIdAndYear(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year
    );

    /**
     * Delete LOP record for specific employee, year, and month
     */
    void deleteByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);
}