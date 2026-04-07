package com.example.employeeLeaveApplication.feature.leave.annual.service;

import com.example.employeeLeaveApplication.shared.constants.PolicyConstants;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveAllocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LeaveAllocationService {

    private static final Logger log = LoggerFactory.getLogger(LeaveAllocationService.class);

    /**
     * Only SICK and ANNUAL_LEAVE have fixed yearly allocations.
     * MATERNITY / PATERNITY are one-time, not stored here.
     * COMP_OFF is earned dynamically.
     * CARRY_FORWARD is handled by CarryForwardBalance entity.
     */
    private static final LeaveType[] DEFAULT_CATEGORIES = {
            LeaveType.SICK,
            LeaveType.ANNUAL_LEAVE
    };
    private static final double[] DEFAULT_DAYS = {
            PolicyConstants.SICK_LEAVE_YEARLY_ALLOCATION,       // 12
            PolicyConstants.ANNUAL_LEAVE_YEARLY_ALLOCATION // 18
    };

    private final LeaveAllocationRepository leaveAllocationRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveAllocationService(LeaveAllocationRepository leaveAllocationRepository,
                                  EmployeeRepository employeeRepository) {
        this.leaveAllocationRepository = leaveAllocationRepository;
        this.employeeRepository = employeeRepository;
    }

    // ── Create one allocation manually ────────────────────────────
    public LeaveAllocation createEmployeeAllocation(LeaveAllocation leaveAllocation) {
        Optional<LeaveAllocation> existing = leaveAllocationRepository
                .findByEmployeeIdAndYearAndLeaveCategory(
                        leaveAllocation.getEmployeeId(),
                        leaveAllocation.getYear(),
                        leaveAllocation.getLeaveCategory());

        if (existing.isPresent()) {
            throw new BadRequestException(
                    "Allocation already exists for this employee, year and leave type");
        }
        return leaveAllocationRepository.save(leaveAllocation);
    }

    // ── Get allocations for specific employee + year ───────────────
    public List<LeaveAllocation> getEmployeeAllocations(Long employeeId, Integer year) {
        if (year == null) year = Year.now().getValue();
        return leaveAllocationRepository.findByEmployeeIdAndYear(employeeId, year);
    }

    // ── Update specific allocation by ID ──────────────────────────
    @Transactional
    public LeaveAllocation updateAllocation(Long id, LeaveAllocation allocation) {
        LeaveAllocation existing = leaveAllocationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(
                        "Leave allocation not found with ID: " + id));

        if (allocation.getAllocatedDays() != null) {
            if (allocation.getAllocatedDays() < 0) {
                throw new BadRequestException("Allocated days cannot be negative");
            }
            existing.setAllocatedDays(allocation.getAllocatedDays());
        }
        if (allocation.getLeaveCategory() != null) {
            existing.setLeaveCategory(allocation.getLeaveCategory());
        }
        return leaveAllocationRepository.save(existing);
    }

    // ── Delete specific allocation by ID ──────────────────────────
    @Transactional
    public void deleteAllocation(Long id) {
        LeaveAllocation allocation = leaveAllocationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(
                        "Leave allocation not found with ID: " + id));
        leaveAllocationRepository.delete(allocation);
    }

    // ── Get all allocations for a year ────────────────────────────
    public List<LeaveAllocation> getAllocationsByYear(Integer year) {
        if (year == null) year = Year.now().getValue();
        return leaveAllocationRepository.findByYear(year);
    }

    // ── Build default allocations for one employee ────────────────
    private List<LeaveAllocation> buildAllocations(Long employeeId, Integer year) {
        List<LeaveAllocation> result = new ArrayList<>();
        for (int i = 0; i < DEFAULT_CATEGORIES.length; i++) {
            LeaveAllocation alloc = new LeaveAllocation();
            alloc.setEmployeeId(employeeId);
            alloc.setLeaveCategory(DEFAULT_CATEGORIES[i]);
            alloc.setYear(year);
            alloc.setAllocatedDays(DEFAULT_DAYS[i]);
            result.add(alloc);
        }
        return result;
    }

    // ── Create bulk allocations for ONE employee ──────────────────
    @Transactional
    public List<LeaveAllocation> createBulkAllocations(Long employeeId, Integer year) {
        if (year == null) year = Year.now().getValue();

        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Employee not found with ID: " + employeeId));

        List<LeaveAllocation> existing =
                leaveAllocationRepository.findByEmployeeIdAndYear(employeeId, year);

        if (!existing.isEmpty()) {
            throw new BadRequestException(
                    "Allocations already exist for employee " + employeeId + " in year " + year);
        }

        return leaveAllocationRepository.saveAll(buildAllocations(employeeId, year));
    }

    // ── Called when a new employee is created ─────────────────────
    @Transactional
    public void allocateForNewEmployee(Long employeeId) {
        createBulkAllocations(employeeId, Year.now().getValue());
    }

    // ── Allocate ALL employees at once ────────────────────────────
    @Transactional
    public Map<String, Object> createBulkAllocationsForAllEmployees(Integer year) {
        if (year == null) year = Year.now().getValue();

        List<Employee> allEmployees = employeeRepository.findAll();
        List<String> success = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed  = new ArrayList<>();

        for (Employee emp : allEmployees) {
            try {
                List<LeaveAllocation> existing =
                        leaveAllocationRepository.findByEmployeeIdAndYear(emp.getId(), year);

                if (!existing.isEmpty()) {
                    skipped.add("Employee " + emp.getId() + " (" + emp.getName()
                            + ") — already allocated for " + year);
                    continue;
                }

                leaveAllocationRepository.saveAll(buildAllocations(emp.getId(), year));
                success.add("Employee " + emp.getId() + " (" + emp.getName()
                        + ") — allocated successfully for " + year);

            } catch (Exception e) {
                log.error("Failed to allocate for employee {}: {}", emp.getId(), e.getMessage(), e);
                failed.add("Employee " + emp.getId() + " (" + emp.getName()
                        + ") — FAILED: " + e.getMessage());
            }
        }

        return Map.of(
                "year",    year,
                "total",   allEmployees.size(),
                "success", success.size(),
                "skipped", skipped.size(),
                "failed",  failed.size(),
                "details", Map.of("success", success, "skipped", skipped, "failed", failed)
        );
    }
}