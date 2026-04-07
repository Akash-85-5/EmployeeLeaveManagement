package com.example.employeeLeaveApplication.feature.attendance.repository;

import com.example.employeeLeaveApplication.feature.attendance.entity.AttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, Long> {

    // ═══════════════════════════════════════════════════════════════
    // EXISTING — by employee_id (used by scheduler & API)
    // ═══════════════════════════════════════════════════════════════

    Optional<AttendanceSummary> findByEmployeeIdAndAttendanceDate(
            Long employeeId, LocalDate attendanceDate);

    List<AttendanceSummary> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long employeeId, LocalDate from, LocalDate to);

    List<AttendanceSummary> findByAttendanceDateOrderByEmployeeNameAsc(LocalDate date);

    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    @Query("""
        SELECT COUNT(a) FROM AttendanceSummary a
        WHERE  a.employeeId       = :empId
          AND  a.attendanceStatus = 'PRESENT'
          AND  a.attendanceDate   BETWEEN :from AND :to
    """)
    long countPresentDays(
            @Param("empId") Long      employeeId,
            @Param("from")  LocalDate from,
            @Param("to")    LocalDate to);

    // ═══════════════════════════════════════════════════════════════
    // NEW — by emp_code (used during Excel import & biometric sync)
    // ═══════════════════════════════════════════════════════════════

    // Check duplicate before inserting Excel row
    Optional<AttendanceSummary> findByEmpCodeAndAttendanceDate(
            String empCode, LocalDate attendanceDate);

    // Avoid duplicate insert during Excel import
    boolean existsByEmpCodeAndAttendanceDate(
            String empCode, LocalDate attendanceDate);

    // Monthly view by emp_code (before employee_id is linked)
    List<AttendanceSummary> findByEmpCodeAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            String empCode, LocalDate from, LocalDate to);

    // All records for an emp_code (used to backfill employee_id)
    List<AttendanceSummary> findByEmpCode(String empCode);

    // Count present days by emp_code for a date range
    @Query("""
        SELECT COUNT(a) FROM AttendanceSummary a
        WHERE  a.empCode          = :empCode
          AND  a.attendanceStatus IN ('PRESENT', 'HALF_DAY')
          AND  a.attendanceDate   BETWEEN :from AND :to
    """)
    long countPresentDaysByEmpCode(
            @Param("empCode") String    empCode,
            @Param("from")    LocalDate from,
            @Param("to")      LocalDate to);

    // Count absent days by emp_code — used for LOP calculation
    @Query("""
        SELECT COUNT(a) FROM AttendanceSummary a
        WHERE  a.empCode          = :empCode
          AND  a.attendanceStatus = 'ABSENT'
          AND  a.attendanceDate   BETWEEN :from AND :to
    """)
    long countAbsentDaysByEmpCode(
            @Param("empCode") String    empCode,
            @Param("from")    LocalDate from,
            @Param("to")      LocalDate to);

    // Backfill employee_id after employee table is populated
    @Modifying
    @Transactional
    @Query("""
        UPDATE AttendanceSummary a
        SET    a.employeeId = :employeeId
        WHERE  a.empCode    = :empCode
          AND  a.employeeId IS NULL
    """)
    int backfillEmployeeId(
            @Param("empCode")    String empCode,
            @Param("employeeId") Long   employeeId);
}