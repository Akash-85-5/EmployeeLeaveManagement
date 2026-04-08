package com.emp_management.infrastructure.scheduler;

import com.emp_management.feature.leave.annual.service.LeaveAllocationService;
import com.emp_management.feature.leave.carryforward.service.CarryForwardBalanceService;
import com.emp_management.shared.exceptions.BadRequestException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

// ✅ NEW IMPORT — added LeaveAllocationService
// Reason: Combined carry forward + allocation in ONE scheduler

@Component
public class CarryForwardScheduler {

    // ===================== EXISTING =====================
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CarryForwardScheduler.class);

    private final CarryForwardBalanceService carryForwardBalanceService;
    private final LeaveAllocationService     leaveAllocationService;

    public CarryForwardScheduler(CarryForwardBalanceService carryForwardBalanceService,
                                 LeaveAllocationService leaveAllocationService) {
        this.carryForwardBalanceService = carryForwardBalanceService;
        this.leaveAllocationService     = leaveAllocationService;
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added new year allocation after carry forward
    // Reason: Both must happen together on Jan 1
    // ❌ REMOVED LeaveAllocationSchedule.yearEndProcess()
    //    to avoid carry forward running TWICE
    @Scheduled(cron = "0 0 0 1 1 *")
    public void processYearEndCarryForward() {

        // FIX: call LocalDate.now() ONCE.
        // Calling it twice risks getting year=2024 for previousYear
        // and year=2025 for currentYear if the clock ticks midnight between the two calls.
        LocalDate today       = LocalDate.now();
        int       currentYear = today.getYear();
        int       previousYear = currentYear - 1;

        log.info("[SCHEDULER] Year-End process started. previousYear={}, currentYear={}",
                previousYear, currentYear);

        runYearEndProcess(previousYear, currentYear);

        log.info("[SCHEDULER] Year-End process complete.");
    }

    // =========================================================================
    // Manual / admin trigger
    // =========================================================================

    /**
     * Admin-callable manual trigger for reruns or testing.
     *
     * FIX 1: visibility was package-private (no modifier).
     *         Any controller outside this package calling this method would
     *         get a compile-time error (or a proxying failure at runtime with Spring AOP).
     *         Changed to public.
     *
     * FIX 2: the old version only ran Step 1 (carry forward) and skipped Step 2
     *         (leave allocation), leaving the system half-processed after a manual trigger.
     *         Now delegates to the shared runYearEndProcess() so both steps always run.
     *
     * @param forYear the PREVIOUS year whose unused leave should be carried forward
     *                (e.g., pass 2024 to generate carry-forward rows for 2025).
     */
    public void triggerYearEndProcessing(Integer forYear) {
        int previousYear = forYear;
        int currentYear  = forYear + 1;

        log.info("[MANUAL-TRIGGER] Admin triggered. previousYear={}, currentYear={}",
                previousYear, currentYear);

        runYearEndProcess(previousYear, currentYear);

        log.info("[MANUAL-TRIGGER] Completed.");
    }

    // =========================================================================
    // Shared logic
    // =========================================================================

    /**
     * Core two-step logic shared by both the scheduled job and the manual trigger.
     *
     * Step 1 is mandatory: if it fails the whole process stops and the exception
     * propagates so the caller (or the scheduler framework) can log/alert on it.
     *
     * Step 2 is best-effort: failure is logged but does NOT roll back Step 1,
     * which has already been committed by its own @Transactional boundary.
     * An operator can re-run Step 2 independently if needed.
     */
    private void runYearEndProcess(int previousYear, int currentYear) {

        // ── Step 1: Carry forward (mandatory) ────────────────────────────────
        try {
            carryForwardBalanceService.processYearEndCarryForward(previousYear);
            log.info("[SCHEDULER] Step 1 OK — carry-forward done for {}", previousYear);
        } catch (Exception e) {
            log.error("[SCHEDULER] Step 1 FAILED — carry-forward for {}: {}",
                    previousYear, e.getMessage(), e);
            // FIX: wrap cleanly so the scheduler framework receives a RuntimeException
            // and can handle retries / dead-letter alerting if configured.
            throw new BadRequestException(
                    "Year-end carry-forward failed for year " + previousYear);
        }

        // ── Step 2: Allocate new-year leaves (best-effort) ───────────────────
        try {
            Map<String, Object> result =
                    leaveAllocationService.createBulkAllocationsForAllEmployees(currentYear);
            log.info("[SCHEDULER] Step 2 OK — allocation done for {}: success={}, skipped={}, failed={}",
                    currentYear,
                    result.get("success"),
                    result.get("skipped"),
                    result.get("failed"));
        } catch (Exception e) {
            // Step 1 is already committed. Log loudly but do not throw —
            // the process is partially successful and Step 2 can be manually retriggered.
            log.error("[SCHEDULER] Step 2 FAILED — allocation for {} did not complete: {}",
                    currentYear, e.getMessage(), e);
        }
    }
}