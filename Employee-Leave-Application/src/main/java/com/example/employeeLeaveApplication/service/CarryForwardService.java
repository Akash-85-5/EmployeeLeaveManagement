package com.example.employeeLeaveApplication.service;

import java.util.ArrayList;
import java.util.List;

import com.example.employeeLeaveApplication.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.employeeLeaveApplication.component.PolicyConstants;
import com.example.employeeLeaveApplication.dto.CarryForwardBalanceResponse;
import com.example.employeeLeaveApplication.dto.CarryForwardEligibilityResponse;
import com.example.employeeLeaveApplication.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveAllocationRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CarryForwardService {

    private static final Logger log = LoggerFactory.getLogger(CarryForwardService.class);

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final CarryForwardBalanceRepository carryForwardRepository;

    // ═══════════════════════════════════════════════════════════════
    // YEAR-END CARRY FORWARD PROCESSING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Process year-end carry forward for ALL employees
     *
     * Rule:
     * - If yearlyBalance ≤ 10, carry forward that amount
     * - If yearlyBalance > 10, carry forward only 10 days
     */
    @Transactional
    public void processYearEndCarryForward(Integer fromYear) {

        log.info("🎆 [CARRY-FORWARD] Processing year-end for ALL employees: {}", fromYear);

        List<Employee> employees = employeeRepository.findByActiveTrue();

        for (Employee employee : employees) {
            try {
                processEmployeeCarryForward(employee.getId(), fromYear);
            } catch (Exception e) {
                log.error("❌ Failed to process carry forward for employee {}: {}",
                        employee.getId(), e.getMessage());
            }
        }

        log.info("✅ [CARRY-FORWARD] Year-end processing complete");
    }

    /**
     * Process carry forward for single employee
     */
    @Transactional
    public void processEmployeeCarryForward(Long employeeId, Integer fromYear) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();

        Long loggedInUserId = user.getUser().getId();

        if (!loggedInUserId.equals(employeeId)) {
            throw new AccessDeniedException("Forbidden");
        }

        Integer toYear = fromYear + 1;

        log.info("📊 [CARRY-FORWARD] Processing: employee={}, from={}, to={}",
                employeeId, fromYear, toYear);

        // Get allocations for current year
        List<LeaveAllocation> allocations = allocationRepository
                .findByEmployeeIdAndYear(employeeId, fromYear);

        double totalAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        // Get total used
        Double totalUsed = applicationRepository
                .getTotalUsedDays(employeeId, LeaveStatus.APPROVED, fromYear);

        if (totalUsed == null) totalUsed = 0.0;

        double yearlyBalance = totalAllocated - totalUsed;

        log.info("   Allocated: {}, Used: {}, Balance: {}",
                totalAllocated, totalUsed, yearlyBalance);

        // Calculate carry forward amount
        double carryForward = 0.0;

        if (yearlyBalance > 0) {
            if (yearlyBalance <= PolicyConstants.CARRY_FORWARD_ELIGIBILITY_THRESHOLD) {
                carryForward = yearlyBalance;
            } else {
                carryForward = PolicyConstants.MAX_CARRY_FORWARD;
            }
        }

        log.info("   Carry Forward Amount: {} days", carryForward);

        // Store carry forward for next year
        if (carryForward > 0) {
            storeCarryForward(employeeId, toYear, carryForward);
        } else {
            log.info("   No balance to carry forward");
        }
    }

    private void storeCarryForward(Long employeeId, Integer year, double amount) {

        CarryForwardBalance balance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(new CarryForwardBalance());

        balance.setEmployeeId(employeeId);
        balance.setYear(year);
        balance.setTotalCarriedForward(amount);
        balance.setTotalUsed(0.0);
        balance.setRemaining(amount);

        carryForwardRepository.save(balance);

        log.info("✅ Stored carry forward: {} days for year {}", amount, year);
    }

    // ═══════════════════════════════════════════════════════════════
    // GET CARRY FORWARD BALANCE
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CarryForwardBalanceResponse getBalance(Long employeeId, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        CarryForwardBalance balance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);

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

    // ═══════════════════════════════════════════════════════════════
    // CHECK ELIGIBILITY
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CarryForwardEligibilityResponse checkEligibility(Long employeeId, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        List<LeaveAllocation> allocations = allocationRepository
                .findByEmployeeIdAndYear(employeeId, year);

        double yearlyAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        Double totalUsed = applicationRepository
                .getTotalUsedDays(employeeId, LeaveStatus.APPROVED, year);

        if (totalUsed == null) totalUsed = 0.0;

        double balance = yearlyAllocated - totalUsed;

        boolean eligible = balance > 0;
        double eligibleAmount = 0.0;
        String reason;

        if (eligible) {
            if (balance <= PolicyConstants.CARRY_FORWARD_ELIGIBILITY_THRESHOLD) {
                eligibleAmount = balance;
                reason = String.format("Eligible to carry forward %.1f days", balance);
            } else {
                eligibleAmount = PolicyConstants.MAX_CARRY_FORWARD;
                reason = String.format("Balance %.1f days exceeds threshold. Max %.0f days allowed",
                        balance, PolicyConstants.MAX_CARRY_FORWARD);
            }
        } else {
            reason = "No remaining balance to carry forward";
        }

        CarryForwardEligibilityResponse response = new CarryForwardEligibilityResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setYearlyAllocated(yearlyAllocated);
        response.setTotalUsed(totalUsed);
        response.setBalance(balance);
        response.setEligible(eligible);
        response.setEligibleAmount(eligibleAmount);
        response.setReason(reason);

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // USE CARRY FORWARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public double useCarryForward(Long employeeId, Integer year, double daysNeeded) {

        log.info("📤 [CARRY-FORWARD] Using: employee={}, year={}, days={}",
                employeeId, year, daysNeeded);

        CarryForwardBalance balance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);

        if (balance == null || balance.getRemaining() <= 0) {
            log.warn("   No carry forward available");
            return 0.0;
        }

        double available = balance.getRemaining();
        double daysUsed = Math.min(daysNeeded, available);

        balance.setTotalUsed(balance.getTotalUsed() + daysUsed);
        balance.setRemaining(balance.getRemaining() - daysUsed);

        carryForwardRepository.save(balance);

        log.info("✅ [CARRY-FORWARD] Used {} days. Remaining: {}",
                daysUsed, balance.getRemaining());

        return daysUsed;
    }

    // ═══════════════════════════════════════════════════════════════
    // RESTORE CARRY FORWARD (When leave cancelled/rejected)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Restore carry forward days (when leave is cancelled/rejected)
     */
    @Transactional
    public void restoreCarryForward(Long employeeId, Integer year, Double days) {

        log.info("🔄 [CARRY-FORWARD] Restoring: employee={}, year={}, days={}",
                employeeId, year, days);

        CarryForwardBalance balance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);

        if (balance == null) {
            log.warn("   No carry forward balance found to restore");
            return;
        }

        balance.setTotalUsed(Math.max(balance.getTotalUsed() - days, 0.0));
        balance.setRemaining(balance.getTotalCarriedForward() - balance.getTotalUsed());

        carryForwardRepository.save(balance);

        log.info("✅ [CARRY-FORWARD] Restored {} days. New remaining: {}",
                days, balance.getRemaining());
    }

    // ═══════════════════════════════════════════════════════════════
    // GET ALL BALANCES (HR View)
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CarryForwardBalanceResponse> getAllBalances(Integer year) {

        List<CarryForwardBalance> balances = carryForwardRepository.findByYear(year);
        List<CarryForwardBalanceResponse> responses = new ArrayList<>();

        for (CarryForwardBalance balance : balances) {
            Employee employee = employeeRepository
                    .findById(balance.getEmployeeId())
                    .orElse(null);

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