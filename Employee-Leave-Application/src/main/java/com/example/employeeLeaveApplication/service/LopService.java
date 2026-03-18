package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.LopResponse;
import com.example.employeeLeaveApplication.dto.LopSummaryResponse;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LopRecord;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LopRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * All LOP business logic lives here.
 *
 * WHO GETS LOP:   EMPLOYEE, TEAM_LEADER, MANAGER, ADMIN
 * WHO DOES NOT:   HR, CFO
 *
 * VISIBILITY:
 *   EMPLOYEE    → own records only
 *   TEAM_LEADER → own + all EMPLOYEEs under their team
 *   MANAGER     → own + all EMPLOYEEs + all TEAM_LEADERs
 *   ADMIN       → everyone in the system
 *   HR          → ALL roles, sorted by highest LOP days
 *   CFO         → ALL roles, sorted by highest LOP days
 */
@Service
public class LopService {

    private final LopRecordRepository lopRepo;
    private final EmployeeRepository  employeeRepo;

    public LopService(LopRecordRepository lopRepo,
                      EmployeeRepository  employeeRepo) {
        this.lopRepo      = lopRepo;
        this.employeeRepo = employeeRepo;
    }

    // ── MONTHLY LOP detail ────────────────────────────────────────

    public List<LopResponse> getMonthlyLop(Long callerId, int year, int month) {
        Employee caller = getEmployee(callerId);
        Role     role   = caller.getRole();

        List<LopRecord> records = switch (role) {

            case EMPLOYEE ->
                    lopRepo.findMonthlyLopForEmployee(callerId, year, month);

            case TEAM_LEADER -> {
                List<Long> ids = getTeamMemberIds(callerId);
                ids.add(callerId);
                yield lopRepo.findMonthlyLopForTeam(ids, year, month);
            }

            case MANAGER -> {
                List<LopRecord> all =
                        lopRepo.findMonthlyLopForManager(year, month);
                all.addAll(lopRepo.findMonthlyLopForEmployee(callerId, year, month));
                yield all;
            }

            case ADMIN ->
                    lopRepo.findMonthlyLopForAdmin(year, month);

            // HR and CFO — all employees, highest LOP first
            case HR, CFO ->
                    lopRepo.findMonthlyLopAllHighFirst(year, month);
        };

        return toResponseList(records);
    }

    // ── YEARLY LOP detail ─────────────────────────────────────────

    public List<LopResponse> getYearlyLop(Long callerId, int year) {
        Employee caller = getEmployee(callerId);
        Role     role   = caller.getRole();

        List<LopRecord> records = switch (role) {

            case EMPLOYEE ->
                    lopRepo.findYearlyLopForEmployee(callerId, year);

            case TEAM_LEADER -> {
                List<Long> ids = getTeamMemberIds(callerId);
                ids.add(callerId);
                yield lopRepo.findYearlyLopForTeam(ids, year);
            }

            case MANAGER -> {
                List<LopRecord> all =
                        lopRepo.findYearlyLopForManager(year);
                all.addAll(lopRepo.findYearlyLopForEmployee(callerId, year));
                yield all;
            }

            case ADMIN ->
                    lopRepo.findYearlyLopForAdmin(year);

            // HR and CFO — all employees, highest LOP first
            case HR, CFO ->
                    lopRepo.findYearlyLopAllHighFirst(year);
        };

        return toResponseList(records);
    }

    // ── HR & CFO aggregated summary ───────────────────────────────

    // One row per employee with total LOP days, sorted highest first
    public List<LopSummaryResponse> getMonthlySummary(Long callerId,
                                                      int year, int month) {
        assertHrOrCfo(callerId);
        return lopRepo.findMonthlySummaryAllHighFirst(year, month)
                .stream()
                .map(LopSummaryResponse::from)
                .collect(Collectors.toList());
    }

    public List<LopSummaryResponse> getYearlySummary(Long callerId, int year) {
        assertHrOrCfo(callerId);
        return lopRepo.findYearlySummaryAllHighFirst(year)
                .stream()
                .map(LopSummaryResponse::from)
                .collect(Collectors.toList());
    }

    // ── Dashboard metric cards ────────────────────────────────────

    public Double getMyMonthlyLopTotal(Long employeeId, int year, int month) {
        return lopRepo.sumLopDaysForMonth(employeeId, year, month);
    }

    public Double getMyYearlyLopTotal(Long employeeId, int year) {
        return lopRepo.sumLopDaysForYear(employeeId, year);
    }

    // ── LOP Reversal — HR corrects a wrongly inserted LOP ─────────

    @Transactional
    public LopResponse reverseLop(Long lopId, Long reversedBy, String reason) {
        LopRecord lop = lopRepo.findById(lopId)
                .orElseThrow(() -> new RuntimeException("LOP record not found: " + lopId));

        if (lop.isReversed()) {
            throw new RuntimeException("LOP record is already reversed.");
        }

        lop.setReversed(true);
        lop.setReversedBy(reversedBy);
        lop.setReversedAt(LocalDateTime.now());
        lop.setReversalReason(reason);

        return LopResponse.from(lopRepo.save(lop));
    }

    // ── Manual insert — called by AttendanceScheduler only ────────

    @Transactional
    public LopRecord insertLop(Long employeeId, String employeeName,
                               String employeeRole, LocalDate lopDate,
                               Double lopDays, String reason,
                               Long attendanceSummaryId) {

        // Guard: HR and CFO never get LOP
        if ("HR".equals(employeeRole) || "CFO".equals(employeeRole)) {
            throw new IllegalArgumentException(
                    "HR and CFO are not eligible for LOP. Employee: " + employeeId);
        }

        // Guard: prevent duplicate LOP for same employee + date
        if (lopRepo.existsByEmployeeIdAndLopDateAndReversedFalse(
                employeeId, lopDate)) {
            throw new RuntimeException(
                    "LOP already exists for employee " + employeeId
                            + " on " + lopDate);
        }

        LopRecord lop = new LopRecord();
        lop.setEmployeeId(employeeId);
        lop.setEmployeeName(employeeName);
        lop.setEmployeeRole(employeeRole);
        lop.setLopDate(lopDate);
        lop.setLopDays(lopDays);
        lop.setLopReason(reason);
        lop.setAttendanceSummaryId(attendanceSummaryId);

        return lopRepo.save(lop);
    }

    // ── Private helpers ───────────────────────────────────────────

    private Employee getEmployee(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
    }

    private List<Long> getTeamMemberIds(Long teamLeaderId) {
        return employeeRepo.findByTeamLeaderId(teamLeaderId)
                .stream()
                .map(Employee::getId)
                .collect(Collectors.toList());
    }

    private void assertHrOrCfo(Long callerId) {
        Employee caller = getEmployee(callerId);
        Role role = caller.getRole();
        if (role != Role.HR && role != Role.CFO) {
            throw new RuntimeException(
                    "Summary report is only accessible by HR and CFO.");
        }
    }

    private List<LopResponse> toResponseList(List<LopRecord> records) {
        return records.stream()
                .map(LopResponse::from)
                .collect(Collectors.toList());
    }
}