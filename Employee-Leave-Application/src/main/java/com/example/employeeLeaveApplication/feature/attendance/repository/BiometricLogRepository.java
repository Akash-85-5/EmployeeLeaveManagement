package com.example.employeeLeaveApplication.feature.attendance.repository;

import com.example.employeeLeaveApplication.feature.attendance.entity.BiometricLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BiometricLogRepository extends JpaRepository<BiometricLog, Long> {

    // All punches for an employee on a date — used by scheduler
    List<BiometricLog> findByEmployeeIdAndPunchDateOrderByPunchTimeAsc(
            Long employeeId, LocalDate punchDate);

    // First punch of the day (IN)
    @Query("""
        SELECT b FROM BiometricLog b
        WHERE  b.employeeId = :empId
          AND  b.punchDate  = :date
        ORDER BY b.punchTime ASC
        LIMIT 1
    """)
    Optional<BiometricLog> findFirstPunch(
            @Param("empId") Long employeeId,
            @Param("date")  LocalDate date);

    // Last punch of the day (OUT)
    @Query("""
        SELECT b FROM BiometricLog b
        WHERE  b.employeeId = :empId
          AND  b.punchDate  = :date
        ORDER BY b.punchTime DESC
        LIMIT 1
    """)
    Optional<BiometricLog> findLastPunch(
            @Param("empId") Long employeeId,
            @Param("date")  LocalDate date);

    // Whether ANY punch exists for employee on date
    boolean existsByEmployeeIdAndPunchDate(Long employeeId, LocalDate date);

    // All unprocessed records — used by scheduler
    List<BiometricLog> findByProcessedFalseAndPunchDateBefore(LocalDate date);

    // Distinct employee IDs that punched on a given date
    @Query("""
        SELECT DISTINCT b.employeeId FROM BiometricLog b
        WHERE b.punchDate = :date
    """)
    List<Long> findEmployeesWhoSentPunchOn(@Param("date") LocalDate date);
}