package com.example.employeeLeaveApplication.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarryForwardService {

    private static final Logger log = LoggerFactory.getLogger(CarryForwardService.class);

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final CarryForwardBalanceRepository carryForwardRepository;

    @Transactional
    public void processYearEndCarryForward(Integer fromYear) {
        log.info("Processing year-end carry forward for all employees: {}", fromYear);

        List<Employee> employees = employeeRepository.findByActiveTrue();
        for (Employee employee : employees) {
            try {
                processEmployeeCarryForward(employee.getId(), fromYear);
            } catch (Exception e) {
                log.error("Failed carry forward for employee {}: {}", employee.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processEmployeeCarryForward(Long employeeId, Integer fromYear) {
        Integer toYear = fromYear + 1;

        // ✅ FIXED
        List<LeaveAllocation> allocations = allocationRepository
                .findByEmployeeIdAndYear(employeeId, fromYear);
        double totalAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        Double totalUsed = applicationRepository.getTotalUsedDays(
                employeeId, LeaveStatus.APPROVED, fromYear);
        if (totalUsed == null) totalUsed = 0.0;

        CarryForwardBalance existingCF = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, fromYear).orElse(null);
        double carriedIn = existingCF != null ? existingCF.getRemaining() : 0.0;

        double yearlyBalance = (totalAllocated + carriedIn) - totalUsed;

        double carryForward = 0.0;
        if (yearlyBalance > 0) {
            carryForward = yearlyBalance <= PolicyConstants.CARRY_FORWARD_ELIGIBILITY_THRESHOLD
                    ? yearlyBalance
                    : PolicyConstants.MAX_CARRY_FORWARD;
        }

        if (carryForward > 0) {
            storeCarryForward(employeeId, toYear, carryForward);
        } else {
            log.info("No balance to carry forward for employee {}", employeeId);
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
        log.info("Stored carry forward: {} days for employee {} year {}", amount, employeeId, year);
    }

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

    @Transactional(readOnly = true)
    public CarryForwardEligibilityResponse checkEligibility(Long employeeId, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        // Fetch allocations for the year
        List<LeaveAllocation> allocations = allocationRepository
                .findByEmployeeIdAndYear(employeeId, year);
        double yearlyAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        // Fetch used days
        Double totalUsed = applicationRepository.getTotalUsedDays(
                employeeId, LeaveStatus.APPROVED, year);
        if (totalUsed == null) totalUsed = 0.0;

        // ✅ FIX: include carry-in from previous year (same as processEmployeeCarryForward)
        CarryForwardBalance existingCF = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, year).orElse(null);
        double carriedIn = existingCF != null ? existingCF.getRemaining() : 0.0;

        // ✅ FIX: match the actual processing formula
        double balance        = (yearlyAllocated + carriedIn) - totalUsed;
        boolean eligible      = balance > 0;
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
        response.setCarriedIn(carriedIn);         // ✅ NEW field
        response.setTotalUsed(totalUsed);
        response.setBalance(balance);
        response.setEligible(eligible);
        response.setEligibleAmount(eligibleAmount);
        response.setReason(reason);

        return response;
    }

    @Transactional
    public double useCarryForward(Long employeeId, Integer year, double daysNeeded) {
        CarryForwardBalance balance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, year).orElse(null);

        if (balance == null || balance.getRemaining() <= 0) return 0.0;

        double daysUsed = Math.min(daysNeeded, balance.getRemaining());
        balance.setTotalUsed(balance.getTotalUsed() + daysUsed);
        balance.setRemaining(balance.getRemaining() - daysUsed);
        carryForwardRepository.save(balance);

        return daysUsed;
    }

    @Transactional
    public void restoreCarryForward(Long employeeId, Integer year, Double days) {
        CarryForwardBalance balance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, year).orElse(null);
        if (balance == null) return;

        balance.setTotalUsed(Math.max(balance.getTotalUsed() - days, 0.0));
        balance.setRemaining(balance.getTotalCarriedForward() - balance.getTotalUsed());
        carryForwardRepository.save(balance);
    }

    @Transactional(readOnly = true)
    public List<CarryForwardBalanceResponse> getAllBalances(Integer year) {
        List<CarryForwardBalance> balances = carryForwardRepository.findByYear(year);
        List<CarryForwardBalanceResponse> responses = new ArrayList<>();

        for (CarryForwardBalance balance : balances) {
            Employee employee = employeeRepository.findById(balance.getEmployeeId()).orElse(null);
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