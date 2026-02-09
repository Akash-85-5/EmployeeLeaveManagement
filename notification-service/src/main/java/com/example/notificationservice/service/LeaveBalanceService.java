package com.example.notificationservice.service;


import com.example.notificationservice.repository.EmployeeRepository;
import com.example.notificationservice.repository.LeaveApplicationRepository;
import com.example.notificationservice.repository.LossOfPayRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.notificationservice.dto.LeaveBalanceResponse;
import com.example.notificationservice.dto.LeaveTypeBreakdown;
import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.entity.LeaveAllocation;
import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.enums.LeaveType;
import com.example.notificationservice.repository.LeaveAllocationRepository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LeaveBalanceService {

    private static final int MAX_CARRY_FORWARD = 10;

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final CompOffService compOffService;
    private final LossOfPayRecordRepository lossOfPayRepository;


    public LeaveBalanceService(EmployeeRepository employeeRepository,
                               LeaveAllocationRepository allocationRepository,
                               LeaveApplicationRepository leaveApplicationRepository,
                               CompOffService compOffService,
                               LossOfPayRecordRepository lossOfPayRepository) {
        this.employeeRepository = employeeRepository;
        this.allocationRepository = allocationRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.compOffService = compOffService;
        this.lossOfPayRepository=lossOfPayRepository;
    }

    public LeaveBalanceResponse getBalance(Long employeeId, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // 1️⃣ Allocations
        List<LeaveAllocation> allocations =
                allocationRepository.findByEmployeeIdAndYear(employeeId, year);

        // 2️⃣ Approved leaves
        List<LeaveApplication> approvedLeaves =
                leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                        employeeId, LeaveStatus.APPROVED, year
                );

        // 3️⃣ Group by leave type
        Map<LeaveType, List<LeaveApplication>> byType =
                approvedLeaves.stream()
                        .collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();
        double totalAllocated = 0;
        double totalUsed = 0;

        for (LeaveAllocation alloc : allocations) {

            LeaveType type = LeaveType.valueOf(alloc.getLeaveCategory());
            double allocated = alloc.getAllocatedDays() + alloc.getCarriedForwardDays();

            double used = byType.getOrDefault(type, List.of())
                    .stream()
                    .map(LeaveApplication::getDays)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            long halfDays = byType.getOrDefault(type, List.of())
                    .stream()
                    .filter(l -> l.getDays().compareTo(new BigDecimal("0.5")) == 0)
                    .count();

            breakdown.add(new LeaveTypeBreakdown(
                    type,
                    allocated,
                    used,
                    allocated - used,
                    (int) halfDays
            ));

            totalAllocated += allocated;
            totalUsed += used;
        }

        // 4️⃣ Comp-off
        BigDecimal compOffBalance = compOffService.getAvailableCompOffDays(employeeId);

        breakdown.add(new LeaveTypeBreakdown(
                LeaveType.COMP_OFF,
                compOffBalance.doubleValue(),
                0.0,
                compOffBalance.doubleValue(),
                0
        ));

        // 5️⃣ Carry forward
        double remaining = totalAllocated - totalUsed;
        double eligibleCarry = remaining >= MAX_CARRY_FORWARD ? MAX_CARRY_FORWARD : 0;

        // 6️⃣ Monthly stats
        int currentMonth = LocalDate.now().getMonthValue();
        long currentMonthApproved = approvedLeaves.stream()
                .filter(l -> l.getStartDate().getMonthValue() == currentMonth)
                .count();

        Double lopPercentage = lossOfPayRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, currentMonth)
                .map(lop -> lop.getLopPercentage())
                .orElse(0.0);


        // 7️⃣ Response
        LeaveBalanceResponse response = new LeaveBalanceResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setTotalAllocated(totalAllocated);
        response.setTotalUsed(totalUsed);
        response.setTotalRemaining(remaining);
        response.setCompOffBalance(compOffBalance.doubleValue());
        response.setEligibleToCarry(eligibleCarry);
        response.setCurrentMonthApproved((int) currentMonthApproved);
        response.setExceededMonthlyLimit(currentMonthApproved > 2);
        response.setLopPercentage(lopPercentage);
        response.setBreakdown(breakdown);
        response.setTotalWorkingDays(employee.getTotalWorkingDays());

        return response;
    }

    // 🔹 CALLED FROM APPROVAL SERVICE
    @Transactional
    public void applyApprovedLeave(LeaveApplication leave) {

        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            compOffService.useCompOff(
                    leave.getEmployeeId(),
                    leave.getDays(),
                    leave.getId()
            );
        }
    }
}


