package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LopRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LopRecordRepository extends JpaRepository<LopRecord, Long> {

    // ── EMPLOYEE — own records only ───────────────────────────────

    List<LopRecord> findByEmployeeIdAndReversedFalseOrderByLopDateDesc(Long employeeId);

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.employeeId = :empId
          AND  l.lopYear    = :year
          AND  l.lopMonth   = :month
          AND  l.reversed   = false
        ORDER BY l.lopDate ASC
    """)
    List<LopRecord> findMonthlyLopForEmployee(
            @Param("empId")  Long employeeId,
            @Param("year")   int  year,
            @Param("month")  int  month);

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.employeeId = :empId
          AND  l.lopYear    = :year
          AND  l.reversed   = false
        ORDER BY l.lopDate ASC
    """)
    List<LopRecord> findYearlyLopForEmployee(
            @Param("empId") Long employeeId,
            @Param("year")  int  year);

    // ── TEAM LEADER — own team employees ─────────────────────────

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.employeeId IN :empIds
          AND  l.lopYear    = :year
          AND  l.lopMonth   = :month
          AND  l.reversed   = false
        ORDER BY l.employeeName ASC, l.lopDate ASC
    """)
    List<LopRecord> findMonthlyLopForTeam(
            @Param("empIds") List<Long> employeeIds,
            @Param("year")   int  year,
            @Param("month")  int  month);

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.employeeId IN :empIds
          AND  l.lopYear    = :year
          AND  l.reversed   = false
        ORDER BY l.employeeName ASC, l.lopDate ASC
    """)
    List<LopRecord> findYearlyLopForTeam(
            @Param("empIds") List<Long> employeeIds,
            @Param("year")   int  year);

    // ── MANAGER — all EMPLOYEE + TEAM_LEADER roles ────────────────

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.employeeRole IN ('EMPLOYEE', 'TEAM_LEADER')
          AND  l.lopYear      = :year
          AND  l.lopMonth     = :month
          AND  l.reversed     = false
        ORDER BY l.employeeName ASC
    """)
    List<LopRecord> findMonthlyLopForManager(
            @Param("year")  int year,
            @Param("month") int month);

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.employeeRole IN ('EMPLOYEE', 'TEAM_LEADER')
          AND  l.lopYear   = :year
          AND  l.reversed  = false
        ORDER BY l.employeeName ASC
    """)
    List<LopRecord> findYearlyLopForManager(@Param("year") int year);

    // ── ADMIN — all records system-wide ──────────────────────────

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.lopYear   = :year
          AND  l.lopMonth  = :month
          AND  l.reversed  = false
        ORDER BY l.employeeName ASC
    """)
    List<LopRecord> findMonthlyLopForAdmin(
            @Param("year")  int year,
            @Param("month") int month);

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.lopYear  = :year
          AND  l.reversed = false
        ORDER BY l.employeeName ASC
    """)
    List<LopRecord> findYearlyLopForAdmin(@Param("year") int year);

    // ── HR & CFO — all roles, highest LOP days first ──────────────

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.lopYear   = :year
          AND  l.lopMonth  = :month
          AND  l.reversed  = false
        ORDER BY l.lopDays DESC, l.employeeName ASC
    """)
    List<LopRecord> findMonthlyLopAllHighFirst(
            @Param("year")  int year,
            @Param("month") int month);

    @Query("""
        SELECT l FROM LopRecord l
        WHERE  l.lopYear  = :year
          AND  l.reversed = false
        ORDER BY l.lopDays DESC, l.employeeName ASC
    """)
    List<LopRecord> findYearlyLopAllHighFirst(@Param("year") int year);

    // ── HR & CFO — aggregated summary (one row per employee) ──────
    // Returns: [employeeId, employeeName, employeeRole, totalLopDays]

    @Query("""
        SELECT   l.employeeId,
                 l.employeeName,
                 l.employeeRole,
                 SUM(l.lopDays) AS totalLop
        FROM     LopRecord l
        WHERE    l.lopYear   = :year
          AND    l.lopMonth  = :month
          AND    l.reversed  = false
        GROUP BY l.employeeId, l.employeeName, l.employeeRole
        ORDER BY totalLop DESC
    """)
    List<Object[]> findMonthlySummaryAllHighFirst(
            @Param("year")  int year,
            @Param("month") int month);

    @Query("""
        SELECT   l.employeeId,
                 l.employeeName,
                 l.employeeRole,
                 SUM(l.lopDays) AS totalLop
        FROM     LopRecord l
        WHERE    l.lopYear  = :year
          AND    l.reversed = false
        GROUP BY l.employeeId, l.employeeName, l.employeeRole
        ORDER BY totalLop DESC
    """)
    List<Object[]> findYearlySummaryAllHighFirst(@Param("year") int year);

    // ── Scheduler guard — prevent duplicate LOP ───────────────────

    boolean existsByEmployeeIdAndLopDateAndReversedFalse(
            Long employeeId, LocalDate lopDate);

    // ── Dashboard metric cards ────────────────────────────────────

    @Query("""
        SELECT COALESCE(SUM(l.lopDays), 0)
        FROM   LopRecord l
        WHERE  l.employeeId = :empId
          AND  l.lopYear    = :year
          AND  l.lopMonth   = :month
          AND  l.reversed   = false
    """)
    Double sumLopDaysForMonth(
            @Param("empId")  Long employeeId,
            @Param("year")   int  year,
            @Param("month")  int  month);

    @Query("""
        SELECT COALESCE(SUM(l.lopDays), 0)
        FROM   LopRecord l
        WHERE  l.employeeId = :empId
          AND  l.lopYear    = :year
          AND  l.reversed   = false
    """)
    Double sumLopDaysForYear(
            @Param("empId") Long employeeId,
            @Param("year")  int  year);

}