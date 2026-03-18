package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.AttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, Long> {

    // Used by scheduler — check if already processed for this employee + date
    Optional<AttendanceSummary> findByEmployeeIdAndAttendanceDate(
            Long employeeId, LocalDate attendanceDate);

    // All records for an employee in a date range (monthly view)
    List<AttendanceSummary> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long employeeId, LocalDate from, LocalDate to);

    // All records for a specific date (used by admin/HR daily report)
    List<AttendanceSummary> findByAttendanceDateOrderByEmployeeNameAsc(LocalDate date);

    // Count present days in a month for an employee
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

    // Check if a record already exists — scheduler uses this to avoid duplicates
    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);
}