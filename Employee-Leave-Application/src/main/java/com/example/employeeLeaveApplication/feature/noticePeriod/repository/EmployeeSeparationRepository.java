package com.example.employeeLeaveApplication.feature.noticePeriod.repository;

import com.example.employeeLeaveApplication.feature.noticePeriod.entity.EmployeeSeparation;
import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import com.example.employeeLeaveApplication.shared.enums.SeparationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeSeparationRepository extends JpaRepository<EmployeeSeparation, Long> {

    // ── Check if employee already has an active separation ────────
    // Prevents duplicate submissions
    @Query("""
        SELECT s FROM EmployeeSeparation s
        WHERE  s.employeeId = :empId
          AND  s.status NOT IN ('REJECTED', 'RELIEVED')
        ORDER BY s.createdAt DESC
    """)
    List<EmployeeSeparation> findActiveByEmployeeId(@Param("empId") Long employeeId);

    // ── Pending approvals per role ────────────────────────────────
    List<EmployeeSeparation> findByStatusOrderByCreatedAtAsc(SeparationStatus status);

    // ── Notice period scheduler queries ──────────────────────────

    // All employees currently serving notice period
    @Query("""
        SELECT s FROM EmployeeSeparation s
        WHERE  s.status = 'NOTICE_PERIOD'
        ORDER BY s.noticePeriodEnd ASC
    """)
    List<EmployeeSeparation> findAllInNoticePeriod();

    // Employees whose notice period end date has passed → mark NOTICE_COMPLETED
    @Query("""
        SELECT s FROM EmployeeSeparation s
        WHERE  s.status           = 'NOTICE_PERIOD'
          AND  s.noticePeriodEnd <= :today
    """)
    List<EmployeeSeparation> findNoticePeriodCompleted(@Param("today") LocalDate today);

    // ── Admin: employees ready for exit checklist ─────────────────
    @Query("""
        SELECT s FROM EmployeeSeparation s
        WHERE  s.status = 'NOTICE_COMPLETED'
        ORDER BY s.noticePeriodEnd ASC
    """)
    List<EmployeeSeparation> findReadyForExitChecklist();

    // ── CFO: employees whose exit checklist is done ───────────────
    @Query("""
        SELECT s FROM EmployeeSeparation s
        WHERE  s.status                = 'EXIT_CHECKLIST_DONE'
          AND  s.finalPayslipGenerated = false
        ORDER BY s.exitChecklistCompletedAt ASC
    """)
    List<EmployeeSeparation> findPayslipPending();

    // ── Absconding: employees absent 7+ consecutive days ─────────
    // Called by NoticePeriodScheduler every morning
    @Query("""
        SELECT DISTINCT a.employeeId
        FROM   AttendanceSummary a
        WHERE  a.attendanceStatus = 'ABSENT'
          AND  a.attendanceDate  >= :since
        GROUP  BY a.employeeId
        HAVING COUNT(a.id) >= 7
    """)
    List<Long> findPotentialAbsconders(@Param("since") LocalDate since);

    // ── Employee history ──────────────────────────────────────────
    List<EmployeeSeparation> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    // ── By type ───────────────────────────────────────────────────
    List<EmployeeSeparation> findBySeparationTypeOrderByCreatedAtDesc(SeparationType type);
}