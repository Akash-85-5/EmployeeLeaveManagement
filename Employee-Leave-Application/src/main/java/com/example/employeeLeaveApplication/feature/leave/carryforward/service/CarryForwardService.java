package com.example.employeeLeaveApplication.feature.leave.carryforward.service;

import com.example.employeeLeaveApplication.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.example.employeeLeaveApplication.feature.leave.annual.service.AnnualLeaveBalanceService;
import com.example.employeeLeaveApplication.shared.constants.PolicyConstants;
import com.example.employeeLeaveApplication.feature.leave.carryforward.dto.CarryForwardBalanceResponse;
import com.example.employeeLeaveApplication.feature.leave.carryforward.dto.CarryForwardEligibilityResponse;
import com.example.employeeLeaveApplication.feature.leave.carryforward.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages year-end carry-forward processing for ANNUAL_LEAVE.
 *
 * SICK, COMP_OFF, MATERNITY, PATERNITY are NOT carried forward.
 * Only ANNUAL_LEAVE unused balance (max 10 days) goes to next year.
 *
 * The actual carry-forward calculation is done inside
 * AnnualLeaveBalanceService.processYearEndCarryForward().
 * This service is the admin-facing entry point.
 */
@Service
@RequiredArgsConstructor
public class CarryForwardService {

    private static final Logger log = LoggerFactory.getLogger(CarryForwardService.class);

    private final EmployeeRepository employeeRepository;
    private final CarryForwardBalanceRepository carryForwardRepository;
    private final AnnualLeaveBalanceService annualLeaveBalanceService;

    // ── Process all employees for a given year ────────────────────
    @Transactional
    public void processYearEndCarryForward(Integer fromYear) {
        log.info("[CF] Processing year-end carry forward for year: {}", fromYear);
        List<Employee> employees = employeeRepository.findByActiveTrue();
        for (Employee employee : employees) {
            try {
                processEmployeeCarryForward(employee.getId(), fromYear);
            } catch (Exception e) {
                log.error("[CF] Failed for employee {}: {}", employee.getId(), e.getMessage());
            }
        }
    }

    // ── Process one employee ──────────────────────────────────────
    @Transactional
    public void processEmployeeCarryForward(Long employeeId, Integer fromYear) {
        annualLeaveBalanceService.processYearEndCarryForward(employeeId, fromYear);
    }

    // ── Get balance for employee/year ─────────────────────────────
    @Transactional(readOnly = true)
    public CarryForwardBalanceResponse getBalance(Long employeeId, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        CarryForwardBalance balance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, year).orElse(null);

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
    public CarryForwardEligibilityResponse checkEligibility(Long employeeId, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        // Get December remaining from annual leave monthly balance
        List<?> summary = annualLeaveBalanceService.getYearSummary(employeeId, year);
        double decRemaining = summary.isEmpty() ? 0.0
                : ((AnnualLeaveMonthlyBalance)
                summary.get(summary.size() - 1)).getRemainingDays();

        double eligibleAmount = Math.min(decRemaining, PolicyConstants.MAX_CARRY_FORWARD);
        boolean eligible = decRemaining > 0;

        String reason;
        if (!eligible) {
            reason = "No remaining ANNUAL_LEAVE balance to carry forward";
        } else if (decRemaining <= PolicyConstants.MAX_CARRY_FORWARD) {
            reason = String.format("Eligible to carry forward %.1f days (full remaining balance)", decRemaining);
        } else {
            reason = String.format("Balance %.1f days exceeds max. Only %.0f days will be carried forward.",
                    decRemaining, PolicyConstants.MAX_CARRY_FORWARD);
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
                    .findById(balance.getEmployeeId()).orElse(null);
            if (employee == null) continue;

            CarryForwardBalanceResponse response = new CarryForwardBalanceResponse();
            response.setEmployeeId(balance.getEmployeeId());
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