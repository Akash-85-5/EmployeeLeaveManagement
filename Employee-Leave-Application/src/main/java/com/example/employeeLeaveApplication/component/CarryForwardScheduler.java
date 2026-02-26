package com.example.employeeLeaveApplication.component;

import com.example.employeeLeaveApplication.service.CarryForwardService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CarryForwardScheduler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CarryForwardScheduler.class);

    private final CarryForwardService carryForwardService;

    /**
     * ✅ FIXED: Cron comment now matches the actual expression.
     * Runs at 00:00 on January 1st each year.
     * Processes carry-forward for the PREVIOUS year (e.g., on Jan 1 2026, processes 2025).
     *
     * Cron format: second minute hour day-of-month month day-of-week
     * '0 0 1 1 *' = At 00:00:00 on January 1st every year
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional  // ✅ ADDED: rollback if processing fails midway
    public void processYearEndCarryForward() {
        log.info("[SCHEDULER] Year-End Carry Forward processing started");
        try {
            int previousYear = LocalDate.now().getYear() - 1;
            log.info("[SCHEDULER] Processing carry forward for year: {}", previousYear);
            carryForwardService.processYearEndCarryForward(previousYear);
            log.info("[SCHEDULER] Year-End Carry Forward completed successfully");
        } catch (Exception e) {
            log.error("[SCHEDULER] Error during year-end carry forward: {}", e.getMessage(), e);
            // ✅ Re-throw so @Transactional rolls back partial changes
            throw new RuntimeException("Year-end carry forward failed", e);
        }
    }

    /**
     * ✅ FIXED: Changed from public to package-private.
     * Called only by AdminController via CarryForwardService directly.
     * Admin endpoints should call carryForwardService.processYearEndCarryForward(year) directly
     * instead of going through this scheduler method.
     *
     * If you need this callable from AdminController, inject CarryForwardService
     * into AdminController directly — don't call the scheduler.
     */
    void triggerYearEndProcessing(Integer forYear) {
        log.info("[MANUAL-TRIGGER] Admin triggered year-end processing for year: {}", forYear);
        try {
            carryForwardService.processYearEndCarryForward(forYear);
            log.info("[MANUAL-TRIGGER] Year-end processing completed for year: {}", forYear);
        } catch (Exception e) {
            log.error("[MANUAL-TRIGGER] Error: {}", e.getMessage(), e);
            throw new RuntimeException("Year-end processing failed: " + e.getMessage(), e);
        }
    }
}