package com.emp_management.feature.leave.carryforward.service;


import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.service.AnnualLeaveBalanceService;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardBalanceResponse;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardEligibilityResponse;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin-facing entry point for carry-forward operations.
 *
 * Year-end flow:
 *   1. Read each employee's December ANNUAL_LEAVE remaining balance
 *      via AnnualLeaveBalanceService.
 *   2. Apply PolicyConstants.MAX_CARRY_FORWARD cap.
 *   3. Delegate persistence to CarryForwardLeaveService.processYearEndCarryForward().
 *
 * Leave application lifecycle (apply / approve / reject / cancel) is
 * handled entirely by CarryForwardLeaveService.
 *
 * Policy (from PolicyConstants):
 *   - Only ANNUAL_LEAVE carries forward.
 *   - Max carry-forward = PolicyConstants.MAX_CARRY_FORWARD (10 days).
 *   - SICK, COMP_OFF, MATERNITY, PATERNITY are NOT carried forward.
 *   - Carry-forward balance is SEPARATE from the 2.5/month combined limit.
 */
@Service
public class CarryForwardService {

    private static final Logger log = LoggerFactory.getLogger(CarryForwardService.class);

    private final EmployeeRepository employeeRepository;
    private final CarryForwardBalanceRepository carryForwardRepository;
    private final AnnualLeaveBalanceService annualLeaveBalanceService;
    private final CarryForwardLeaveService      carryForwardLeaveService;   // ✅ NEW

    public CarryForwardService(CarryForwardLeaveService carryForwardLeaveService, AnnualLeaveBalanceService annualLeaveBalanceService, CarryForwardBalanceRepository carryForwardRepository, EmployeeRepository employeeRepository) {
        this.carryForwardLeaveService = carryForwardLeaveService;
        this.annualLeaveBalanceService = annualLeaveBalanceService;
        this.carryForwardRepository = carryForwardRepository;
        this.employeeRepository = employeeRepository;
    }
// ── Process all employees for a given year ────────────────────

    Long MAX_CARRY_FORWARD = 10L;
    /**
     * Triggers year-end carry-forward for every active employee.
     * Safe to run multiple times (upsert semantics inside CarryForwardLeaveService).
     *
     * @param fromYear  the year whose December balance is the source
     *                  (carry amount goes into fromYear + 1)
     */
    @Transactional
    public void processYearEndCarryForward(Integer fromYear) {
        log.info("[CF] Processing year-end carry forward for year: {}", fromYear);
        List<Employee> employees = employeeRepository.findByActiveTrue();
        for (Employee employee : employees) {
            try {
                processEmployeeCarryForward(employee.getEmpId(), fromYear);
            } catch (Exception e) {
                log.error("[CF] Failed for employee {}: {}", employee.getEmpId(), e.getMessage());
            }
        }
    }

    // ── Process one employee ──────────────────────────────────────

    /**
     * 1. Reads December ANNUAL_LEAVE remaining balance from AnnualLeaveBalanceService.
     * 2. Applies PolicyConstants.MAX_CARRY_FORWARD cap.
     * 3. Calls CarryForwardLeaveService to persist the balance for (fromYear + 1).
     */
    @Transactional
    public void processEmployeeCarryForward(String employeeId, Integer fromYear) {

        // ── Step 1: Get December remaining ANNUAL_LEAVE balance ───
        List<?> summary = annualLeaveBalanceService.getYearSummary(employeeId, fromYear);

        double decRemaining = 0.0;
        if (!summary.isEmpty()) {
            // getYearSummary returns List<AnnualLeaveMonthlyBalance>
            AnnualLeaveMonthlyBalance decBalance =
                    (AnnualLeaveMonthlyBalance) summary.get(summary.size() - 1);
            decRemaining = decBalance.getRemainingDays();
        }

        log.info("[CF] employee={}, fromYear={}, decemberRemaining={}", employeeId, fromYear, decRemaining);

        // ── Step 2: Apply cap ─────────────────────────────────────
        // Max carry-forward = PolicyConstants.MAX_CARRY_FORWARD (10 days)
        // If remaining ≤ 10 → carry all; if > 10 → carry only 10
        double carryAmount = Math.min(decRemaining, MAX_CARRY_FORWARD);

        if (carryAmount <= 0) {
            log.info("[CF] No carry forward for employee {} (remaining={})", employeeId, decRemaining);
            return;
        }

        // ── Step 3: Persist via CarryForwardLeaveService ──────────
        carryForwardLeaveService.processYearEndCarryForward(employeeId, fromYear, carryAmount);

        log.info("[CF] Carry forward complete: employee={}, carried={} days into {}",
                employeeId, carryAmount, fromYear + 1);
    }

    // ── Get balance for employee/year ─────────────────────────────

    @Transactional(readOnly = true)
    public CarryForwardBalanceResponse getBalance(String  employeeId, Integer year) {
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        CarryForwardBalance balance = carryForwardRepository
                .findByEmployee_EmpIdAndYear(employeeId, year).orElse(null);

        CarryForwardBalanceResponse response = new CarryForwardBalanceResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);

        if (balance != null) {
            response.setTotalCarriedForward(balance.getTotalCarriedForward());
            response.setTotalUsed(balance.getTotalUsed());
            response.setRemaining(balance.getRemaining());
        } else {
            response.setTotalCarriedForward(0.0);
            response.setTotalUsed(0.0);
            response.setRemaining(0.0);
        }
        return response;
    }

    // ── Check eligibility for carry-forward ───────────────────────

    @Transactional(readOnly = true)
    public CarryForwardEligibilityResponse checkEligibility(String  employeeId, Integer year) {
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        // Get December remaining from annual leave monthly balance
        List<?> summary = annualLeaveBalanceService.getYearSummary(employeeId, year);
        double decRemaining = summary.isEmpty() ? 0.0
                : ((AnnualLeaveMonthlyBalance) summary.get(summary.size() - 1)).getRemainingDays();

        double eligibleAmount = Math.min(decRemaining, MAX_CARRY_FORWARD);
        boolean eligible      = decRemaining > 0;

        String reason;
        if (!eligible) {
            reason = "No remaining ANNUAL_LEAVE balance to carry forward";
        } else if (decRemaining <= MAX_CARRY_FORWARD) {
            reason = String.format(
                    "Eligible to carry forward %.1f days (full remaining balance)", decRemaining);
        } else {
            reason = String.format(
                    "Balance %.1f days exceeds max. Only %.0f days will be carried forward.",
                    decRemaining, MAX_CARRY_FORWARD);
        }

        CarryForwardEligibilityResponse response = new CarryForwardEligibilityResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setBalance(decRemaining);
        response.setEligible(eligible);
        response.setEligibleAmount(eligibleAmount);
        response.setReason(reason);

        return response;
    }

    // ── Get all balances for a year ───────────────────────────────

    @Transactional(readOnly = true)
    public List<CarryForwardBalanceResponse> getAllBalances(Integer year) {
        List<CarryForwardBalance> balances = carryForwardRepository.findByYear(year);
        List<CarryForwardBalanceResponse> responses = new ArrayList<>();

        for (CarryForwardBalance balance : balances) {
            Employee employee = employeeRepository
                    .findByEmpId(balance.getEmployee().getEmpId()).orElse(null);
            if (employee == null) continue;

            CarryForwardBalanceResponse response = new CarryForwardBalanceResponse();
            response.setEmployeeId(balance.getEmployee().getEmpId());
            response.setEmployeeName(employee.getName());
            response.setYear(year);
            response.setTotalCarriedForward(balance.getTotalCarriedForward());
            response.setTotalUsed(balance.getTotalUsed());
            response.setRemaining(balance.getRemaining());
            responses.add(response);
        }
        return responses;
    }
}