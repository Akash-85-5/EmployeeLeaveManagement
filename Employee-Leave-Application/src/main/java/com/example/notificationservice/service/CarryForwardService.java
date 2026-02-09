package com.example.notificationservice.service;

import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.entity.LeaveAllocation;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.repository.EmployeeRepository;
import com.example.notificationservice.repository.LeaveAllocationRepository;
import com.example.notificationservice.repository.LeaveApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CarryForwardService {

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepo;
    private final LeaveApplicationRepository leaveApplicationRepository;

    private static final int MAX_CARRY_FORWARD = 10;
    private static final double BASE_ALLOCATION = 24.0;

    /**
     * ====================================================================
     * YEAR-END PROCESSING: Calculate and apply carry forward
     * Run this at year-end to set up next year's allocations
     * ====================================================================
     */
    @Transactional
    public void processYearEndCarryForward(Integer fromYear) {
//        log.info("[CARRY-FORWARD] Processing year-end: {} → {}", fromYear, fromYear + 1);

        List<Employee> employees = employeeRepository.findAll();
        int processedCount = 0;

        for (Employee employee : employees) {
            try {
                processEmployeeCarryForward(employee.getId(), fromYear);
                processedCount++;
            } catch (Exception e) {
                log.error("[CARRY-FORWARD] Failed for employee={}: {}",
                        employee.getId(), e.getMessage());
            }
        }

//        log.info("[CARRY-FORWARD] Completed: processed {} employees", processedCount);
    }

    /**
     * Process carry forward for single employee
     */
    @Transactional
    public void processEmployeeCarryForward(Long employeeId, Integer fromYear) {
        Integer toYear = fromYear + 1;

        // 1. Get total allocated for current year
        List<LeaveAllocation> currentAllocations = allocationRepo
                .findByEmployeeIdAndYear(employeeId, fromYear);

        double totalAllocated = currentAllocations.stream()
                .mapToDouble(a -> a.getAllocatedDays() + a.getCarriedForwardDays())
                .sum();

        // 2. Get total used (APPROVED only)
        Double totalUsed = leaveApplicationRepository.getTotalUsedDays(employeeId, LeaveStatus.APPROVED, fromYear);
        if (totalUsed == null) totalUsed = 0.0;

        // 3. Calculate remaining
        double remaining = totalAllocated - totalUsed;


        double carryForward = Math.min(
                Math.max(remaining, 0),
                MAX_CARRY_FORWARD
        );


//        log.info("[CARRY-FORWARD] Employee {}: allocated={}, used={}, remaining={}, carry={}",
//                employeeId, totalAllocated, totalUsed, remaining, carryForward);

        // 5. Create next year's allocations
        createNextYearAllocations(employeeId, toYear, carryForward);
    }

    /**
     * Create allocations for next year with carry forward
     */
    private void createNextYearAllocations(Long employeeId, Integer year, double carryForward) {
        // Standard categories
        String[] categories = {"VACATION", "SICK", "CASUAL", "PERSONAL"};
        double[] allocations = {8.0, 6.0, 6.0, 4.0};  // Total = 24

        for (int i = 0; i < categories.length; i++) {
            LeaveAllocation alloc = new LeaveAllocation();
            alloc.setEmployeeId(employeeId);
            alloc.setLeaveCategory(categories[i]);
            alloc.setYear(year);
            alloc.setAllocatedDays(allocations[i]);

            // Add carry forward to first category (VACATION)
            if (i == 0) {
                alloc.setCarriedForwardDays(carryForward);
            } else {
                alloc.setCarriedForwardDays(0.0);
            }

            allocationRepo.save(alloc);
        }

//        log.info("[CARRY-FORWARD] Created allocations for employee {} year {} with carry={}",
//                employeeId, year, carryForward);
    }
}