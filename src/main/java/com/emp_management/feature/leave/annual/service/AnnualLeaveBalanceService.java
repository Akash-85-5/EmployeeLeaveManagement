package com.emp_management.feature.leave.annual.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.feature.leave.annual.repository.AnnualLeaveMonthlyBalanceRepository;
import com.emp_management.feature.leave.annual.repository.LeaveTypeRepository;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Manages the cumulative ANNUAL leave monthly balance.
 *
 * RULES:
 *  - Each month, (allocatedDays / 12) days are accrued.
 *  - Only the CURRENT month's record is lazily created on first access.
 *  - Prior months are NOT backfilled — new employees joining mid-year
 *    start with a single month's accrual, not all months up to today.
 *  - Jan: available = monthlyRate + carry_forward_remaining (from previous year)
 *  - Feb onwards: available = previous month's remaining + monthlyRate
 *  - Dec year-end: unused remaining → carry forward (max 10 days).
 */
@Service
public class AnnualLeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(AnnualLeaveBalanceService.class);

    private static final double MAX_CARRY_FORWARD = 10.0;

    private final AnnualLeaveMonthlyBalanceRepository monthlyBalanceRepo;
    private final CarryForwardBalanceRepository       carryForwardRepo;
    private final EmployeeRepository                  employeeRepository;
    private final LeaveTypeRepository                 leaveTypeRepository;

    public AnnualLeaveBalanceService(AnnualLeaveMonthlyBalanceRepository monthlyBalanceRepo,
                                     CarryForwardBalanceRepository carryForwardRepo,
                                     EmployeeRepository employeeRepository,
                                     LeaveTypeRepository leaveTypeRepository) {
        this.monthlyBalanceRepo = monthlyBalanceRepo;
        this.carryForwardRepo   = carryForwardRepo;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the remaining (available) days for the given month.
     * Initializes the current month's record if it doesn't exist yet.
     */
    @Transactional
    public double getAvailableForMonth(String employeeId, int year, int month) {
        ensureCurrentMonthInitialized(employeeId, year, month);
        return getOrThrow(employeeId, year, month).getRemainingDays();
    }

    /**
     * Called during employee onboarding (personal details submission)
     * and on dashboard load to ensure the current month's record exists.
     */
    @Transactional
    public void initializeForCurrentMonth(String employeeId, int year, int month) {
        ensureCurrentMonthInitialized(employeeId, year, month);
    }

    /**
     * Deducts leave days from the given month's balance.
     */
    @Transactional
    public void deductLeave(String employeeId, int year, int month, double days) {
        ensureCurrentMonthInitialized(employeeId, year, month);
        AnnualLeaveMonthlyBalance record = getOrThrow(employeeId, year, month);

        double newUsed      = record.getUsedDays() + days;
        double newRemaining = record.getAvailableDays() - newUsed;

        if (newRemaining < 0) {
            throw new BadRequestException(
                    "Insufficient ANNUAL leave balance. Available: "
                            + record.getRemainingDays() + ", Requested: " + days);
        }

        record.setUsedDays(newUsed);
        record.setRemainingDays(newRemaining);
        monthlyBalanceRepo.save(record);

        log.info("[ANNUAL] Deducted {} days for employee {} month {}/{}. Remaining: {}",
                days, employeeId, month, year, newRemaining);
    }

    /**
     * Restores leave days back to the given month's balance (on cancellation/rejection).
     */
    @Transactional
    public void restoreLeave(String employeeId, int year, int month, double days) {
        monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .ifPresent(record -> {
                    double newUsed      = Math.max(record.getUsedDays() - days, 0.0);
                    double newRemaining = record.getAvailableDays() - newUsed;
                    record.setUsedDays(newUsed);
                    record.setRemainingDays(newRemaining);
                    monthlyBalanceRepo.save(record);
                    log.info("[ANNUAL] Restored {} days for employee {} month {}/{}. Remaining: {}",
                            days, employeeId, month, year, newRemaining);
                });
    }

    /**
     * Returns all monthly balance records for the given year.
     * Only initializes the current month — does not backfill past months.
     */
    @Transactional
    public List<AnnualLeaveMonthlyBalance> getYearSummary(String employeeId, int year) {
        int currentMonth = (year == LocalDate.now().getYear())
                ? LocalDate.now().getMonthValue() : 12;
        ensureCurrentMonthInitialized(employeeId, year, currentMonth);
        return monthlyBalanceRepo.findByEmployeeIdAndYearOrderByMonthAsc(employeeId, year);
    }

    /**
     * Year-end job: carries forward unused annual leave (max 10 days) to the next year.
     * Should be triggered by a scheduler at the end of December.
     */
    @Transactional
    public void processYearEndCarryForward(String employeeId, int fromYear) {
        // Ensure December record exists before reading it
        ensureCurrentMonthInitialized(employeeId, fromYear, 12);

        double remaining = monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, fromYear, 12)
                .map(AnnualLeaveMonthlyBalance::getRemainingDays)
                .orElse(0.0);

        double carryForward = Math.min(remaining, MAX_CARRY_FORWARD);
        if (carryForward <= 0) {
            log.info("[CF] No carry forward for employee {} from year {}", employeeId, fromYear);
            return;
        }

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee not found: " + employeeId));

        int toYear = fromYear + 1;
        CarryForwardBalance cf = carryForwardRepo
                .findByEmployee_EmpIdAndYear(employeeId, toYear)
                .orElse(new CarryForwardBalance());

        cf.setEmployee(employee);
        cf.setYear(toYear);
        cf.setTotalCarriedForward(carryForward);
        cf.setTotalUsed(0.0);
        cf.setRemaining(carryForward);
        carryForwardRepo.save(cf);

        log.info("[CF] Carried forward {} days for employee {} to year {}",
                carryForward, employeeId, toYear);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Creates the monthly record ONLY for the given month if it doesn't exist.
     * Does NOT loop through prior months — new employees joining mid-year
     * correctly get only the current month's single accrual, not a backfill.
     *
     * Cumulative carry-forward still works naturally:
     *  - When the next month's record is created, it reads the previous
     *    month's remainingDays (if that record exists) and adds the accrual.
     *  - If the previous month record doesn't exist (new employee), it defaults to 0.
     */
    private void ensureCurrentMonthInitialized(String employeeId, int year, int month) {
        if (monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .isPresent()) {
            return; // already initialized — nothing to do
        }

        LeaveType leaveType   = getAnnualLeaveType();
        double    monthlyRate = getMonthlyAccrualRate(leaveType);
        createMonthRecord(employeeId, year, month, monthlyRate);
    }

    private void createMonthRecord(String employeeId, int year, int month, double monthlyRate) {
        double previousRemaining = getPreviousRemaining(employeeId, year, month);
        double available         = previousRemaining + monthlyRate;

        AnnualLeaveMonthlyBalance record = new AnnualLeaveMonthlyBalance();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setAvailableDays(available);
        record.setUsedDays(0.0);
        record.setRemainingDays(available);
        monthlyBalanceRepo.save(record);

        log.info("[ANNUAL] Created {}/{} for employee {}. Rate: {}/month, PrevRemaining: {}, Available: {}",
                month, year, employeeId, monthlyRate, previousRemaining, available);
    }

    /**
     * For January: seeds from carry-forward balance of the previous year.
     * For other months: reads the previous month's remaining days.
     * If the previous month record doesn't exist (new employee joining mid-year),
     * returns 0.0 — so they only get the current month's accrual, not a backfill.
     */
    private double getPreviousRemaining(String employeeId, int year, int month) {
        if (month == 1) {
            double cf = carryForwardRepo
                    .findByEmployee_EmpIdAndYear(employeeId, year)
                    .map(CarryForwardBalance::getRemaining)
                    .orElse(0.0);
            log.info("[ANNUAL] Jan init for employee {}: carry-forward = {}", employeeId, cf);
            return cf;
        }
        return monthlyBalanceRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month - 1)
                .map(AnnualLeaveMonthlyBalance::getRemainingDays)
                .orElse(0.0); // 0.0 = new employee joining mid-year, no prior record
    }

    private LeaveType getAnnualLeaveType() {
        return leaveTypeRepository.findByLeaveType("ANNUAL")
                .orElseThrow(() -> new BadRequestException(
                        "LeaveType 'ANNUAL' not found in DB. Please seed the leave_type table."));
    }

    private double getMonthlyAccrualRate(LeaveType leaveType) {
        return leaveType.getAllocatedDays() / 12.0;
    }

    private AnnualLeaveMonthlyBalance getOrThrow(String employeeId, int year, int month) {
        return monthlyBalanceRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new BadRequestException(
                        "Annual leave balance not found for employee "
                                + employeeId + " " + year + "/" + month));
    }
}