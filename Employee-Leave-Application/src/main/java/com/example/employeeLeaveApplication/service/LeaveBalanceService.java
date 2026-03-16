package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.PolicyConstants;
import com.example.employeeLeaveApplication.dto.LeaveBalanceResponse;
import com.example.employeeLeaveApplication.dto.LeaveTypeBreakdown;
import com.example.employeeLeaveApplication.entity.*;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns a unified leave balance summary for an employee.
 *
 * Leave types and their balance sources:
 *
 *  SICK         → flat allocation (12/year) minus approved sick leaves
 *  ANNUAL_LEAVE → AnnualLeaveMonthlyBalance (cumulative monthly)
 *  MATERNITY    → fixed 90 days; 0 remaining if already taken this year
 *  PATERNITY    → fixed 5 days; 0 remaining if already taken this year
 *  COMP_OFF     → CompOffBalance (earned - used)
 *  CARRY_FORWARD→ shown separately (read from CarryForwardBalance for current year)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(LeaveBalanceService.class);

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final CarryForwardBalanceRepository carryForwardBalanceRepository;
    private final CompOffBalanceRepository compOffBalanceRepository;
    private final AnnualLeaveBalanceService annualLeaveBalanceService;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;
    private final CompOffService compOffService;

    // ═══════════════════════════════════════════════════════════════
    // MAIN BALANCE RESPONSE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LeaveBalanceResponse getBalance(Long employeeId, Integer year) {
        log.info("[BALANCE] Fetching balance for employee={}, year={}", employeeId, year);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        int currentMonth = (year == LocalDate.now().getYear())
                ? LocalDate.now().getMonthValue() : 12;

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();

        // ── SICK ──────────────────────────────────────────────────
        double sickAllocated = getAllocated(employeeId, year, LeaveType.SICK);
        double sickUsed      = getUsed(employeeId, year, LeaveType.SICK);
        breakdown.add(new LeaveTypeBreakdown(
                LeaveType.SICK,
                (Double) sickAllocated,
                (Double) sickUsed,
                (Double)(sickAllocated - sickUsed),
                0));

        // ── ANNUAL_LEAVE (cumulative monthly) ─────────────────────
        double annualAvailable = annualLeaveBalanceService
                .getAvailableForMonth(employeeId, year, currentMonth);
        double annualUsed      = getUsed(employeeId, year, LeaveType.ANNUAL_LEAVE);
        double annualAllocated = PolicyConstants.ANNUAL_LEAVE_YEARLY_ALLOCATION;
        breakdown.add(new LeaveTypeBreakdown(
                LeaveType.ANNUAL_LEAVE,
                (Double) annualAllocated,
                (Double) annualUsed,
                (Double) annualAvailable,
                0));

        // ── MATERNITY (female only) ───────────────────────────────
        EmployeePersonalDetails details = personalDetailsRepository
                .findByEmployeeId(employeeId).orElse(null);
        if (details != null && details.getGender() == Gender.FEMALE) {
            double maternityAllocated = (double) PolicyConstants.MATERNITY_DAYS;
            double maternityUsed      = getUsed(employeeId, year, LeaveType.MATERNITY);
            double maternityRemaining = maternityUsed > 0 ? 0.0 : maternityAllocated;
            breakdown.add(new LeaveTypeBreakdown(
                    LeaveType.MATERNITY,
                    (Double) maternityAllocated,
                    (Double) maternityUsed,
                    (Double) maternityRemaining,
                    0));
        }

        // ── PATERNITY (male only) ─────────────────────────────────
        if (details != null && details.getGender() == Gender.MALE) {
            double paternityAllocated = (double) PolicyConstants.PATERNITY_DAYS;
            double paternityUsed      = getUsed(employeeId, year, LeaveType.PATERNITY);
            double paternityRemaining = paternityUsed > 0 ? 0.0 : paternityAllocated;
            breakdown.add(new LeaveTypeBreakdown(
                    LeaveType.PATERNITY,
                    (Double) paternityAllocated,
                    (Double) paternityUsed,
                    (Double) paternityRemaining,
                    0));
        }

        // ── COMP_OFF ──────────────────────────────────────────────
        CompOffBalance compOffBalance = compOffBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year).orElse(null);
        double compOffEarned    = compOffBalance != null ? compOffBalance.getEarned()  : 0.0;
        double compOffUsed      = compOffBalance != null ? compOffBalance.getUsed()    : 0.0;
        double compOffAvailable = compOffBalance != null ? compOffBalance.getBalance() : 0.0;
        breakdown.add(new LeaveTypeBreakdown(
                LeaveType.COMP_OFF,
                (Double) compOffEarned,
                (Double) compOffUsed,
                (Double) compOffAvailable,
                0));

        // ── CARRY FORWARD (from previous year, used in current year) ─
        CarryForwardBalance carryForward = carryForwardBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year).orElse(null);
        double cfRemaining = carryForward != null ? carryForward.getRemaining() : 0.0;

        // ── Build response ────────────────────────────────────────
        LeaveBalanceResponse response = new LeaveBalanceResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setBreakdown(breakdown);
        response.setCarriedFromLastYear(cfRemaining);
        response.setCompOffBalance(compOffAvailable);
        response.setCompOffEarned(compOffEarned);
        response.setCompOffUsed(compOffUsed);

        // For backward compatibility fields
        response.setTotalAllocated(sickAllocated + annualAllocated);
        response.setTotalUsed(sickUsed + annualUsed);
        response.setTotalRemaining((sickAllocated - sickUsed) + annualAvailable);

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // BALANCE CHECK (used in apply validation as fallback)
    // ═══════════════════════════════════════════════════════════════

    public boolean hasSufficientBalance(Long employeeId, Integer year,
                                        LeaveType leaveType, Double daysRequested) {
        return switch (leaveType) {
            case ANNUAL_LEAVE -> {
                int month = LocalDate.now().getMonthValue();
                double available = annualLeaveBalanceService
                        .getAvailableForMonth(employeeId, year, month);
                yield available >= daysRequested;
            }
            case SICK -> {
                double allocated = getAllocated(employeeId, year, LeaveType.SICK);
                double used      = getUsed(employeeId, year, LeaveType.SICK);
                yield (allocated - used) >= daysRequested;
            }
            case COMP_OFF -> {
                CompOffBalance co = compOffBalanceRepository
                        .findByEmployeeIdAndYear(employeeId, year).orElse(null);
                yield co != null && co.getBalance() >= daysRequested;
            }
            default -> true; // MATERNITY, PATERNITY checked separately
        };
    }

    // Called by LeaveApprovalService / LeaveApplicationService
    @Transactional
    public void applyApprovedLeave(LeaveApplication leave) {
        // Delegated to LeaveApplicationService.applyBalanceDeduction
        // This method kept for backward-compatible wiring
    }

    @Transactional
    public void restoreApprovedLeave(LeaveApplication leave) {
        // Delegated to LeaveApplicationService.restoreBalance
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private double getAllocated(Long employeeId, int year, LeaveType type) {
        return allocationRepository
                .findByEmployeeIdAndYear(employeeId, year).stream()
                .filter(a -> a.getLeaveCategory() == type)
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();
    }

    private double getUsed(Long employeeId, int year, LeaveType type) {
        Double used = leaveApplicationRepository
                .getTotalUsedDaysByType(employeeId, LeaveStatus.APPROVED, year, type);
        return used != null ? used : 0.0;
    }
}