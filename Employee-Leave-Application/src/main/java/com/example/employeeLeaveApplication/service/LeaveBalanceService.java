package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.PolicyConstants;
import com.example.employeeLeaveApplication.dto.LeaveBalanceResponse;
import com.example.employeeLeaveApplication.dto.LeaveTypeBreakdown;
import com.example.employeeLeaveApplication.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.entity.CompOffBalance;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.repository.CompOffBalanceRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveAllocationRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.repository.LossOfPayRecordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(LeaveBalanceService.class);

    private final EmployeeRepository employeeRepository;
    private final CompOffService compOffService;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final CarryForwardBalanceRepository carryForwardBalanceRepository;
    private final CompOffBalanceRepository compOffBalanceRepository;
    private final LossOfPayRecordRepository lossOfPayRecordRepository;

    public LeaveBalanceResponse getBalance(Long employeeId, Integer year) {

        log.info("Getting balance for employee: {}, year: {}", employeeId, year);
        int currentYear  = LocalDate.now().getYear();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        // ✅ FIXED: findByEmployeeIdAndYear returns List, use stream().findFirst() before orElse()
        LeaveAllocation allocationn = allocationRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .stream().findFirst().orElse(null);
        double totalAllocated = allocationn != null ? allocationn.getAllocatedDays() : 0.0;

        List<LeaveApplication> approvedLeaves = leaveApplicationRepository
                .findByEmployeeIdAndStatusAndYear(employeeId, LeaveStatus.APPROVED, currentYear);

        List<LeaveAllocation> allocations = allocationRepository
                .findByEmployeeIdAndYear(employeeId, currentYear);

        // Group approved leaves by LeaveType for O(1) lookup inside the loop
        Map<LeaveType, List<LeaveApplication>> byType = approvedLeaves.stream()
                .collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();

        for (LeaveAllocation allocation : allocations) {
            LeaveType type = allocation.getLeaveCategory();
            double allocated = allocation.getAllocatedDays();

            List<LeaveApplication> typeLeaves = byType.getOrDefault(type, List.of());

            double used = typeLeaves.stream()
                    .mapToDouble(l -> l.getDays().doubleValue())
                    .sum();

            int halfDays = (int) typeLeaves.stream()
                    .filter(l -> l.getDays().compareTo(new BigDecimal("0.5")) == 0)
                    .count();

            breakdown.add(new LeaveTypeBreakdown(
                    type,
                    allocated,
                    used,
                    allocated - used,   // per-type remaining, not grand-total
                    halfDays
            ));
        }

        double totalUsed = approvedLeaves.stream()
                .filter(l -> l.getLeaveType() != LeaveType.COMP_OFF)
                .mapToDouble(l -> l.getDays().doubleValue())
                .sum();

        CompOffBalance compOffBalance = compOffBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year).orElse(null);
        double compOffEarned = 0.0;
        double compOffUsed = 0.0;
        double compOffAvailable = 0.0;
        if (compOffBalance != null) {
            compOffEarned = compOffBalance.getEarned();
            compOffUsed = compOffBalance.getUsed();
            compOffAvailable = compOffBalance.getBalance();
        }
        breakdown.add(new LeaveTypeBreakdown(LeaveType.COMP_OFF, compOffUsed, 0));

        CarryForwardBalance carryForward = carryForwardBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year).orElse(null);
        double carriedFromLastYear = carryForward != null ? carryForward.getRemaining() : 0.0;

        double yearlyBalance = totalAllocated - totalUsed;
        double eligibleToCarry = 0.0;
        if (yearlyBalance > 0) {
            eligibleToCarry = yearlyBalance <= PolicyConstants.CARRY_FORWARD_ELIGIBILITY_THRESHOLD
                    ? yearlyBalance
                    : PolicyConstants.MAX_CARRY_FORWARD;
        }

        int currentMonth = LocalDate.now().getMonthValue();
        Double currentMonthUsedDays = leaveApplicationRepository
                .getTotalApprovedDaysInMonth(employeeId, year, currentMonth);
        if (currentMonthUsedDays == null) currentMonthUsedDays = 0.0;
        boolean exceededMonthlyLimit = currentMonthUsedDays > PolicyConstants.MONTHLY_LIMIT;

        Double lopPercentage = lossOfPayRecordRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, year);
        if (lopPercentage == null) lopPercentage = 0.0;

        LeaveBalanceResponse response = new LeaveBalanceResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setTotalAllocated(totalAllocated);
        response.setTotalUsed(totalUsed);
        response.setTotalRemaining(yearlyBalance);
        response.setCompOffBalance(compOffAvailable);
        response.setCompOffEarned(compOffEarned);
        response.setCompOffUsed(compOffUsed);
        response.setLopPercentage(lopPercentage);
        response.setCarriedFromLastYear(carriedFromLastYear);
        response.setEligibleToCarry(eligibleToCarry);
        response.setCurrentMonthApproved(currentMonthUsedDays.intValue());
        response.setExceededMonthlyLimit(exceededMonthlyLimit);
        response.setBreakdown(breakdown);

        return response;
    }

    public boolean hasSufficientBalance(Long employeeId, Integer year,
                                        LeaveType leaveType, Double daysRequested) {
        if (leaveType == LeaveType.COMP_OFF) {
            CompOffBalance compOff = compOffBalanceRepository
                    .findByEmployeeIdAndYear(employeeId, year).orElse(null);
            if (compOff == null) return false;
            return compOff.getBalance() >= daysRequested;
        }

        // ✅ FIXED: findByEmployeeIdAndYear returns List, use stream().findFirst() before orElse()
        LeaveAllocation allocation = allocationRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .stream().findFirst().orElse(null);
        if (allocation == null) return false;

        Double totalUsed = leaveApplicationRepository
                .getTotalUsedDays(employeeId, LeaveStatus.APPROVED, year);
        if (totalUsed == null) totalUsed = 0.0;

        double remaining = allocation.getAllocatedDays() - totalUsed;
        return remaining >= daysRequested;
    }

    public Double getTotalLossOfPayPercentage(Long employeeId, Integer year) {
        Double total = lossOfPayRecordRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, year);
        return total != null ? total : 0.0;
    }

    @Transactional
    public void applyApprovedLeave(LeaveApplication leave) {
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            compOffService.useCompOff(leave.getEmployeeId(), leave.getDays(), leave.getId());
        }
    }

    @Transactional
    public void restoreApprovedLeave(LeaveApplication leave) {
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            compOffService.restoreCompOffBalance(leave.getEmployeeId(), leave.getDays());
        }
    }
}