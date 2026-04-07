package com.emp_management.infrastructure.scheduler;

import com.emp_management.feature.leave.annual.service.LeaveAllocationService;
import com.emp_management.feature.leave.carryforward.service.CarryForwardBalanceService; // ✅ CHANGED IMPORT
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Year-end scheduler — runs on Jan 1 every year.
 *
 * Step 1: CarryForwardBalanceService.processYearEndCarryForward(previousYear)
 *         → Reads unused annual leave for all employees
 *         → Caps at LeaveType "CARRY_FORWARD".allocatedDays (admin-configurable)
 *         → Writes CarryForwardBalance records for the new year
 *
 * Step 2: LeaveAllocationService.createBulkAllocationsForAllEmployees(currentYear)
 *         → Allocates ANNUAL_LEAVE + SICK for the new year for all employees
 *
 * ✅ CHANGED: CarryForwardService → CarryForwardBalanceService
 *    Reason: The old CarryForwardService belonged to the old separate module
 *            which has been removed. processYearEndCarryForward() now lives
 *            in CarryForwardBalanceService, which is the correct location
 *            in the unified deploy module pattern.
 */
@Component
public class CarryForwardScheduler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CarryForwardScheduler.class);

    // ✅ CHANGED: was CarryForwardService (old separate module)
    //            now CarryForwardBalanceService (unified deploy module)
    private final CarryForwardBalanceService carryForwardBalanceService;
    private final LeaveAllocationService     leaveAllocationService;

    public CarryForwardScheduler(CarryForwardBalanceService carryForwardBalanceService,
                                 LeaveAllocationService leaveAllocationService) {
        this.carryForwardBalanceService = carryForwardBalanceService;
        this.leaveAllocationService     = leaveAllocationService;
    }

    /**
     * Fires at midnight on January 1st every year.
     * Cron: second=0, minute=0, hour=0, dayOfMonth=1, month=1, dayOfWeek=*
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional
    public void processYearEndCarryForward() {

        int previousYear = LocalDate.now().getYear() - 1;
        int currentYear  = LocalDate.now().getYear();

        log.info("[SCHEDULER] Year-End process started");
        log.info("[SCHEDULER] Previous Year: {}", previousYear);
        log.info("[SCHEDULER] New Year: {}", currentYear);

        // ── Step 1: Carry forward for previous year ───────────────
        // Reads unused annual leave for all employees.
        // Caps at LeaveType "CARRY_FORWARD".allocatedDays (DB-configured by admin).
        // Writes carry_forward_balance rows for currentYear.
        try {
            carryForwardBalanceService.processYearEndCarryForward(previousYear); // ✅ CHANGED
            log.info("[SCHEDULER] ✅ Carry forward done for {}", previousYear);
        } catch (Exception e) {
            log.error("[SCHEDULER] ❌ Carry forward failed: {}", e.getMessage(), e);
            throw new RuntimeException("Year-end carry forward failed", e);
        }

        // ── Step 2: Allocate new year leaves for all employees ────
        // Allocates ANNUAL_LEAVE and SICK for currentYear.
        // Must run AFTER carry forward so January annual balance
        // can correctly seed from the carry-forward record written in Step 1.
        try {
            Map<String, Object> result =
                    leaveAllocationService.createBulkAllocationsForAllEmployees(currentYear);
            log.info("[SCHEDULER] ✅ Allocation done for {}: success={}, skipped={}, failed={}",
                    currentYear,
                    result.get("success"),
                    result.get("skipped"),
                    result.get("failed"));
        } catch (Exception e) {
            log.error("[SCHEDULER] ❌ Allocation failed: {}", e.getMessage());
        }

        log.info("[SCHEDULER] ✅ Year-End process complete");
    }

    /**
     * Manual trigger — called by admin controller for testing or re-runs.
     * ✅ CHANGED: now delegates to CarryForwardBalanceService (same as scheduled job).
     */
    void triggerYearEndProcessing(Integer forYear) {
        log.info("[MANUAL-TRIGGER] Admin triggered for year: {}", forYear);
        try {
            carryForwardBalanceService.processYearEndCarryForward(forYear); // ✅ CHANGED
            log.info("[MANUAL-TRIGGER] Completed for year: {}", forYear);
        } catch (Exception e) {
            log.error("[MANUAL-TRIGGER] Error: {}", e.getMessage());
            throw new RuntimeException("Year-end processing failed: " + e.getMessage(), e);
        }
    }
}