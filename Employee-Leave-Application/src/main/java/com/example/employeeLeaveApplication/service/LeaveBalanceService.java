package com.example.employeeLeaveApplication.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import static com.example.employeeLeaveApplication.enums.LeaveType.*;

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

    // ═══════════════════════════════════════════════════════════════
    // GET COMPLETE LEAVE BALANCE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get complete leave balance for employee
     *
     * Calculation Rules:
     * - Total Allocated = 24 days (VACATION:8 + SICK:4 + CASUAL:6 + PERSONAL:4)
     * - CompOff is NOT allocated, it's EARNED separately
     * - Carry Forward is stored in separate table
     * - LOP comes from monthly violations
     */
    public LeaveBalanceResponse getBalance(Long employeeId, Integer year) {

        log.info("📊 [BALANCE] Getting balance for employee: {}, year: {}", employeeId, year);

        // ═══════════════════════════════════════════════════════════
        // 1. GET EMPLOYEE
        // ═══════════════════════════════════════════════════════════

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        // ═══════════════════════════════════════════════════════════
        // 2. GET ALLOCATIONS (VACATION, SICK, CASUAL, PERSONAL only)
        // ═══════════════════════════════════════════════════════════

        List<LeaveAllocation> allocations =
                allocationRepository.findByEmployeeIdAndYear(employeeId, year);

        double totalAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        log.info("   Total Allocated: {} days", totalAllocated);

        // ═══════════════════════════════════════════════════════════
        // 3. GET APPROVED LEAVES (Calculate total used)
        // ═══════════════════════════════════════════════════════════

        List<LeaveApplication> approvedLeaves =
                leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                        employeeId, LeaveStatus.APPROVED, year);

        // Group by leave type for breakdown
        Map<LeaveType, List<LeaveApplication>> byType = approvedLeaves.stream()
                .collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        double totalUsed = approvedLeaves.stream()
                .filter(l -> l.getLeaveType() != LeaveType.COMP_OFF) // Don't count COMP_OFF in regular usage
                .mapToDouble(l -> l.getDays().doubleValue())
                .sum();

        log.info("   Total Used (excluding COMP_OFF): {} days", totalUsed);

        // ═══════════════════════════════════════════════════════════
        // 4. BUILD LEAVE TYPE BREAKDOWN
        // ═══════════════════════════════════════════════════════════

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();

        for (LeaveAllocation alloc : allocations) {

            LeaveType type = alloc.getLeaveCategory();
            double allocated = alloc.getAllocatedDays();

            // Calculate used for this type
            double used = byType.getOrDefault(type, List.of())
                    .stream()
                    .mapToDouble(l -> l.getDays().doubleValue())
                    .sum();

            // Count half days
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
        }

        // ═══════════════════════════════════════════════════════════
        // 5. GET COMP-OFF BALANCE (Earned, not allocated)
        // ═══════════════════════════════════════════════════════════

        CompOffBalance compOffBalance = compOffBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);

        double compOffEarned = 0.0;
        double compOffUsed = 0.0;
        double compOffAvailable = 0.0;

        if (compOffBalance != null) {
            compOffEarned = compOffBalance.getEarned();
            compOffUsed = compOffBalance.getUsed();
            compOffAvailable = compOffBalance.getBalance();
        }

        // Add COMP_OFF to breakdown
        breakdown.add(new LeaveTypeBreakdown(
                LeaveType.COMP_OFF,
                compOffEarned,
                compOffUsed,
                compOffAvailable,
                0
        ));

        log.info("   CompOff Balance: Earned={}, Used={}, Available={}",
                compOffEarned, compOffUsed, compOffAvailable);

        // ═══════════════════════════════════════════════════════════
        // 6. GET CARRY FORWARD BALANCE
        // ═══════════════════════════════════════════════════════════

        CarryForwardBalance carryForward = carryForwardBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);

        double carriedFromLastYear = 0.0;
        if (carryForward != null) {
            carriedFromLastYear = carryForward.getRemaining();
        }

        log.info("   Carry Forward: {} days", carriedFromLastYear);

        // ═══════════════════════════════════════════════════════════
        // 7. CALCULATE ELIGIBLE CARRY FORWARD FOR YEAR-END
        // Rule: If yearlyBalance <= 10, carry that amount (max 10)
        //       If yearlyBalance > 10, carry only 10
        // ═══════════════════════════════════════════════════════════

        double yearlyBalance = totalAllocated - totalUsed;
        double eligibleToCarry = 0.0;

        if (yearlyBalance > 0) {
            if (yearlyBalance <= PolicyConstants.CARRY_FORWARD_ELIGIBILITY_THRESHOLD) {
                eligibleToCarry = yearlyBalance;
            } else {
                eligibleToCarry = PolicyConstants.MAX_CARRY_FORWARD;
            }
        }

        log.info("   Yearly Balance: {}, Eligible to Carry: {}", yearlyBalance, eligibleToCarry);

        // ═══════════════════════════════════════════════════════════
        // 8. GET MONTHLY STATS
        // ═══════════════════════════════════════════════════════════

        int currentMonth = LocalDate.now().getMonthValue();
        Integer currentMonthApprovedCount = leaveApplicationRepository
                .countApprovedInMonth(employeeId, year, currentMonth);

        boolean exceededMonthlyLimit =
                currentMonthApprovedCount > PolicyConstants.MONTHLY_LIMIT;

        log.info("   Current Month Approved: {}, Exceeded Limit: {}",
                currentMonthApprovedCount, exceededMonthlyLimit);

        // ═══════════════════════════════════════════════════════════
        // 9. GET LOSS OF PAY PERCENTAGE
        // ═══════════════════════════════════════════════════════════

        Double lopPercentage = lossOfPayRecordRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, year);

        if (lopPercentage == null) {
            lopPercentage = 0.0;
        }

        log.info("   Loss of Pay: {}%", lopPercentage);

        // ═══════════════════════════════════════════════════════════
        // 10. BUILD RESPONSE
        // ═══════════════════════════════════════════════════════════

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
        response.setCurrentMonthApproved(currentMonthApprovedCount);
        response.setExceededMonthlyLimit(exceededMonthlyLimit);
        response.setBreakdown(breakdown);

        log.info("✅ [BALANCE] Balance calculation complete");

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // INITIALIZE ALLOCATIONS FOR NEW EMPLOYEE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initialize leave allocations for new employee
     * Creates allocations for VACATION, SICK, CASUAL, PERSONAL (Total: 24 days)
     * Does NOT create COMP_OFF allocation (it's earned)
     */
    @Transactional
    public void initializeAllocations(Long employeeId, Integer year) {

        log.info("🆕 [INIT] Initializing allocations for employee: {}, year: {}",
                employeeId, year);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee Not Found"));


        // Check if already exists
        List<LeaveAllocation> existing =
                allocationRepository.findByEmployeeIdAndYear(employeeId, year);

        if (existing.isEmpty()) {
            log.warn("   Allocations already exist for employee: {}, year: {}",
                    employeeId, year);
            return;
        }

        // Create allocations (NO COMP_OFF here!)
        List<LeaveAllocation> allocations = Arrays.asList(
                createAllocation(employeeId, year, VACATION,
                        PolicyConstants.VACATION_YEARLY_ALLOCATION),
                createAllocation(employeeId, year, SICK,
                        PolicyConstants.SICK_YEARLY_ALLOCATION),
                createAllocation(employeeId, year, CASUAL,
                        PolicyConstants.CASUAL_YEARLY_ALLOCATION),
                createAllocation(employeeId, year, PERSONAL,
                        PolicyConstants.PERSONAL_YEARLY_ALLOCATION)
        );

        allocationRepository.saveAll(allocations);

        log.info("✅ [INIT] Created {} allocations (Total: {} days)",
                allocations.size(), PolicyConstants.TOTAL_YEARLY_ALLOCATION);
    }

    private LeaveAllocation createAllocation(Long employeeId, Integer year,
                                             LeaveType category, Double days) {
        LeaveAllocation alloc = new LeaveAllocation();
        alloc.setEmployeeId(employeeId);
        alloc.setYear(year);
        alloc.setLeaveCategory(category);
        alloc.setAllocatedDays(days);
        return alloc;
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATE SUFFICIENT BALANCE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validate if employee has sufficient balance for leave type
     *
     * Special handling:
     * - COMP_OFF: Check comp-off balance, not allocation
     * - Others: Check allocation balance
     */
    public boolean hasSufficientBalance(Long employeeId, Integer year,
                                        LeaveType leaveType, Double daysRequested) {

        log.info("[VALIDATE] Checking balance: employee={}, type={}, days={}",
                employeeId, leaveType, daysRequested);

        if (leaveType == LeaveType.COMP_OFF) {
            CompOffBalance compOff = compOffBalanceRepository
                    .findByEmployeeIdAndYear(employeeId, year)
                    .orElse(null);

            if (compOff == null) {
                log.warn("   No comp-off balance found");
                return false;
            }
            boolean sufficient = compOff.getBalance() >= daysRequested;
            log.info("   CompOff balance: {}, Requested: {}, Sufficient: {}",
                    compOff.getBalance(), daysRequested, sufficient);
            return sufficient;

        } else {
            LeaveAllocation allocation = allocationRepository
                    .findByEmployeeIdAndYearAndLeaveCategory(
                            employeeId, year, leaveType.name())
                    .orElse(null);

            if (allocation == null) {
                log.warn("   No allocation found for type: {}", leaveType);
                return false;
            }

            // ✅ FIXED: Get used days for THIS specific leave type only
            Double used = leaveApplicationRepository.getTotalUsedDaysByType(
                    employeeId, LeaveStatus.APPROVED, year, leaveType);

            if (used == null) used = 0.0;

            double available = allocation.getAllocatedDays() - used;
            boolean sufficient = available >= daysRequested;

            log.info("   Allocated: {}, Used: {}, Available: {}, Requested: {}, Sufficient: {}",
                    allocation.getAllocatedDays(), used, available, daysRequested, sufficient);

            return sufficient;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET TOTAL LOSS OF PAY PERCENTAGE
    // ═══════════════════════════════════════════════════════════════

    public Double getTotalLossOfPayPercentage(Long employeeId, Integer year) {
        Double total = lossOfPayRecordRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, year);
        return total != null ? total : 0.0;
    }
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