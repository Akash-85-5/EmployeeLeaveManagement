package com.emp_management.feature.leave.annual.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.leave.annual.entity.LeaveAllocation;
import com.emp_management.feature.leave.annual.repository.LeaveAllocationRepository;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.feature.leave.annual.repository.LeaveTypeRepository;
import com.emp_management.shared.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeaveAllocationService {

    private static final Logger log = LoggerFactory.getLogger(LeaveAllocationService.class);

    private final LeaveAllocationRepository leaveAllocationRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveAllocationService(LeaveAllocationRepository leaveAllocationRepository,
                                  EmployeeRepository employeeRepository,
                                  LeaveTypeRepository leaveTypeRepository) {
        this.leaveAllocationRepository = leaveAllocationRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // ── Create one allocation manually ────────────────────────────
//    public LeaveAllocation createEmployeeAllocation(LeaveAllocation leaveAllocation) {
//        Optional<LeaveAllocation> existing = leaveAllocationRepository
//                .findByEmployeeIdAndYearAndLeaveCategory(
//                        leaveAllocation.getEmployee().getEmpId(),
//                        leaveAllocation.getYear(),
//                        leaveAllocation.getLeaveCategory());
//        if (existing.isPresent()) {
//            throw new BadRequestException(
//                    "Allocation already exists for this employee, year and leave type");
//        }
//        return leaveAllocationRepository.save(leaveAllocation);
//    }

    public List<LeaveAllocation> getEmployeeAllocations(String employeeId, Integer year) {
        if (year == null) year = Year.now().getValue();
        return leaveAllocationRepository.findByEmployee_EmpIdAndYear(employeeId, year);
    }

    // ── Update specific allocation by ID ──────────────────────────
    @Transactional
    public LeaveAllocation updateAllocation(Long id, LeaveAllocation allocation) {
        LeaveAllocation existing = leaveAllocationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(
                        "Leave allocation not found with ID: " + id));
        if (allocation.getAllocatedDays() != null) {
            if (allocation.getAllocatedDays() < 0)
                throw new BadRequestException("Allocated days cannot be negative");
            existing.setAllocatedDays(allocation.getAllocatedDays());
        }
        if (allocation.getLeaveCategory() != null)
            existing.setLeaveCategory(allocation.getLeaveCategory());
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
    private List<LeaveAllocation> buildAllocations(Employee emp, Integer year) {
        List<LeaveType> autoTypes = leaveTypeRepository.findAllByAutoAllocateTrue();

        if (autoTypes.isEmpty()) {
            throw new IllegalStateException(
                    "No leave types with auto_allocate=true found. " +
                            "Please seed the leave_type table correctly.");
        }

        return autoTypes.stream()
                .map(leaveType -> {
                    LeaveAllocation alloc = new LeaveAllocation();
                    alloc.setEmployee(emp);
                    alloc.setLeaveCategory(leaveType);
                    alloc.setYear(year);
                    alloc.setAllocatedDays(leaveType.getAllocatedDays());
                    return alloc;
                })
                .collect(Collectors.toList());
    }

    // ── Create bulk allocations for ONE employee ──────────────────
    @Transactional
    public List<LeaveAllocation> createBulkAllocations(String employeeId, Integer year) {
        if (year == null) year = Year.now().getValue();

        Employee emp = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Employee not found with ID: " + employeeId));

        List<LeaveAllocation> existing =
                leaveAllocationRepository.findByEmployee_EmpIdAndYear(employeeId, year);

        if (!existing.isEmpty()) {
            throw new BadRequestException(
                    "Allocations already exist for employee " + employeeId + " in year " + year);
        }

        return leaveAllocationRepository.saveAll(buildAllocations(emp, year));
    }

    // ── Called when a new employee is created ─────────────────────
    @Transactional
    public void allocateForNewEmployee(String employeeId) {
        createBulkAllocations(employeeId, Year.now().getValue());
    }

    // ── Allocate ALL employees at once ────────────────────────────
    @Transactional
    public Map<String, Object> createBulkAllocationsForAllEmployees(Integer year) {
        if (year == null) year = Year.now().getValue();

        // Validate auto-allocatable types exist before looping employees
        List<LeaveType> autoTypes = leaveTypeRepository.findAllByAutoAllocateTrue();
        if (autoTypes.isEmpty()) {
            throw new IllegalStateException(
                    "No leave types with auto_allocate=true found in the database.");
        }

        List<Employee> allEmployees = employeeRepository.findAll();
        List<String> success = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed  = new ArrayList<>();

        for (Employee emp : allEmployees) {
            try {
                List<LeaveAllocation> existing =
                        leaveAllocationRepository.findByEmployee_EmpIdAndYear(emp.getEmpId(), year);

                if (!existing.isEmpty()) {
                    skipped.add(emp.getEmpId() + " (" + emp.getName() + ") — already allocated");
                    continue;
                }

                leaveAllocationRepository.saveAll(buildAllocations(emp, year));
                success.add(emp.getEmpId() + " (" + emp.getName() + ") — allocated");

            } catch (Exception e) {
                log.error("Failed to allocate for {}: {}", emp.getEmpId(), e.getMessage(), e);
                failed.add(emp.getEmpId() + " (" + emp.getName() + ") — FAILED: " + e.getMessage());
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