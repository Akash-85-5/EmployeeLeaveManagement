package com.emp_management.feature.attendance.repository;

import com.emp_management.feature.attendance.entity.AttendanceSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, Long> {

    Optional<AttendanceSummary> findByEmployeeIdAndAttendanceDate(
            String employeeId, LocalDate attendanceDate);

    List<AttendanceSummary> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            String employeeId, LocalDate from, LocalDate to);

    List<AttendanceSummary> findByAttendanceDateOrderByEmployeeNameAsc(LocalDate date);

    boolean existsByEmployeeIdAndAttendanceDate(String employeeId, LocalDate date);

    @Query("""
        SELECT a FROM AttendanceSummary a
        WHERE (:status IS NULL OR a.attendanceStatus = :status)
          AND (:from IS NULL OR a.attendanceDate >= :from)
          AND (:to IS NULL OR a.attendanceDate <= :to)
    """)
    Page<AttendanceSummary> findFilteredAttendance(
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}