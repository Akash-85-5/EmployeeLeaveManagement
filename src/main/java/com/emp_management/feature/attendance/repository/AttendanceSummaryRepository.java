package com.emp_management.feature.attendance.repository;

import com.emp_management.feature.attendance.entity.AttendanceSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, Long> {

    // -------------------------------------------------------------------------
    // 1. STANDARD & PAGINATED METHODS (For UI Tables/Calendars)
    // -------------------------------------------------------------------------

    // Find specific record
    Optional<AttendanceSummary> findByEmployeeIdAndAttendanceDate(String employeeId, LocalDate attendanceDate);

    // Check existence
    boolean existsByEmployeeIdAndAttendanceDate(String employeeId, LocalDate date);

    // Get Daily View
    List<AttendanceSummary> findByAttendanceDateOrderByEmployeeNameAsc(LocalDate date);

    // Paginated Filtered (For "All Employees" page)
    @Query("""
                SELECT a FROM AttendanceSummary a
                WHERE (:status IS NULL OR TRIM(a.attendanceStatus) = TRIM(:status))
                  AND (:from IS NULL OR a.attendanceDate >= :from)
                  AND (:to IS NULL OR a.attendanceDate <= :to)
            """)
    Page<AttendanceSummary> findFilteredAttendance(
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    // Paginated Team View
    @Query("""
                SELECT a FROM AttendanceSummary a
                WHERE a.employeeId IN :empIds
                  AND (:status IS NULL OR TRIM(a.attendanceStatus) = TRIM(:status))
                  AND (:from IS NULL OR a.attendanceDate >= :from)
                  AND (:to IS NULL OR a.attendanceDate <= :to)
            """)
    Page<AttendanceSummary> findByEmployeeIdIn(
            @Param("empIds") List<String> empIds,
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    // Paginated Individual Range
    @Query("""
                SELECT a FROM AttendanceSummary a 
                WHERE a.employeeId = :empId 
                  AND (:from IS NULL OR a.attendanceDate >= :from)
                  AND (:to IS NULL OR a.attendanceDate <= :to)
            """)
    Page<AttendanceSummary> findByEmployeeIdAndDateRange(
            @Param("empId") String empId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    // -------------------------------------------------------------------------
    // 2. BULK / LIST-BASED METHODS (For Excel Export)
    // -------------------------------------------------------------------------

    // Used for Monthly Calendar View (Non-paginated)
    List<AttendanceSummary> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            String employeeId, LocalDate from, LocalDate to);

    // Used for Manager Team Export
    List<AttendanceSummary> findByEmployeeIdInAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            List<String> empIds, LocalDate from, LocalDate to);

    // Used for Admin Org-wide Export (Date range only)
    List<AttendanceSummary> findByAttendanceDateBetweenOrderByAttendanceDateAsc(
            LocalDate from, LocalDate to);

    // Used for Admin Filtered Export
    @Query("""
                SELECT a FROM AttendanceSummary a
                WHERE (:status IS NULL OR TRIM(a.attendanceStatus) = TRIM(:status))
                  AND (:from IS NULL OR a.attendanceDate >= :from)
                  AND (:to IS NULL OR a.attendanceDate <= :to)
            """)
    List<AttendanceSummary> findFilteredAttendanceList(
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}