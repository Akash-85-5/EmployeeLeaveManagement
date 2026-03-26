package com.example.employeeLeaveApplication.feature.attendance.repository;

import com.example.employeeLeaveApplication.feature.attendance.entity.BiometricLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BiometricLogRepository extends JpaRepository<BiometricLog, Long> {

    // ═══════════════════════════════════════════════════════════════
    // EXISTING — by employee_id (used by scheduler)
    // ═══════════════════════════════════════════════════════════════

    List<BiometricLog> findByEmployeeIdAndPunchDateOrderByPunchTimeAsc(
            Long employeeId, LocalDate punchDate);

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

    boolean existsByEmployeeIdAndPunchDate(Long employeeId, LocalDate date);

    List<BiometricLog> findByProcessedFalseAndPunchDateBefore(LocalDate date);

    @Query("""
        SELECT DISTINCT b.employeeId FROM BiometricLog b
        WHERE b.punchDate = :date
    """)
    List<Long> findEmployeesWhoSentPunchOn(@Param("date") LocalDate date);

    // ═══════════════════════════════════════════════════════════════
    // NEW — by emp_code (used during Excel import)
    // ═══════════════════════════════════════════════════════════════

    // All punches for emp_code on a date ordered by time — used during import
    List<BiometricLog> findByEmpCodeAndPunchDateOrderByPunchTimeOnlyAsc(
            String empCode, LocalDate punchDate);

    // Avoid duplicate punch insert during re-import
    boolean existsByEmpCodeAndPunchDateAndPunchTimeOnly(
            String empCode, LocalDate punchDate,
            java.time.LocalTime punchTimeOnly);

    // First punch by emp_code (IN time) — before employee_id is linked
    @Query("""
        SELECT b FROM BiometricLog b
        WHERE  b.empCode   = :empCode
          AND  b.punchDate = :date
        ORDER BY b.punchTimeOnly ASC
        LIMIT 1
    """)
    Optional<BiometricLog> findFirstPunchByEmpCode(
            @Param("empCode") String    empCode,
            @Param("date")    LocalDate date);

    // Last punch by emp_code (OUT time) — before employee_id is linked
    @Query("""
        SELECT b FROM BiometricLog b
        WHERE  b.empCode   = :empCode
          AND  b.punchDate = :date
        ORDER BY b.punchTimeOnly DESC
        LIMIT 1
    """)
    Optional<BiometricLog> findLastPunchByEmpCode(
            @Param("empCode") String    empCode,
            @Param("date")    LocalDate date);

    // All emp_codes that punched on a date — used by import scheduler
    @Query("""
        SELECT DISTINCT b.empCode FROM BiometricLog b
        WHERE b.punchDate = :date
    """)
    List<String> findEmpCodesWhoSentPunchOn(@Param("date") LocalDate date);

    // All unprocessed punches by emp_code
    List<BiometricLog> findByEmpCodeAndProcessedFalse(String empCode);

    // Backfill employee_id after employee table is populated
    @Modifying
    @Transactional
    @Query("""
        UPDATE BiometricLog b
        SET    b.employeeId = :employeeId
        WHERE  b.empCode    = :empCode
          AND  b.employeeId IS NULL
    """)
    int backfillEmployeeId(
            @Param("empCode")    String empCode,
            @Param("employeeId") Long   employeeId);
}