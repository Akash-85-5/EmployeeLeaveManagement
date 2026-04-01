package com.emp_management.infrastructure.scheduler;


import com.emp_management.feature.leave.annual.service.LeaveAllocationService;
import com.emp_management.feature.leave.carryforward.service.CarryForwardService;
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

    // ===================== EXISTING =====================
    private final CarryForwardService carryForwardService;

    // ✅ NEW FIELD
    // Reason: Allocate new year after carry forward
    private final LeaveAllocationService leaveAllocationService;

    public CarryForwardScheduler(CarryForwardService carryForwardService, LeaveAllocationService leaveAllocationService) {
        this.carryForwardService = carryForwardService;
        this.leaveAllocationService = leaveAllocationService;
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added new year allocation after carry forward
    // Reason: Both must happen together on Jan 1
    // ❌ REMOVED LeaveAllocationSchedule.yearEndProcess()
    //    to avoid carry forward running TWICE
    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional
    public void processYearEndCarryForward() {

        int previousYear = LocalDate.now().getYear() - 1;
        int currentYear  = LocalDate.now().getYear();

        log.info("[SCHEDULER] Year-End process started");
        log.info("[SCHEDULER] Previous Year: {}", previousYear);
        log.info("[SCHEDULER] New Year: {}", currentYear);

        // ===================== EXISTING =====================
        // Step 1: Carry forward for previous year
        try {
            carryForwardService
                    .processYearEndCarryForward(previousYear);
            log.info("[SCHEDULER] ✅ Carry forward done for {}",
                    previousYear);
        } catch (Exception e) {
            log.error("[SCHEDULER] ❌ Carry forward failed: {}",
                    e.getMessage(), e);
            throw new RuntimeException(
                    "Year-end carry forward failed", e);
        }

        // ✅ NEW: Step 2 — Allocate new year for all employees
        // Reason: Was in LeaveAllocationSchedule but caused
        //         carry forward to run twice
        try {
            Map<String, Object> result = leaveAllocationService
                    .createBulkAllocationsForAllEmployees(currentYear);
            log.info("[SCHEDULER] ✅ Allocation done for {}: " +
                            "success={}, skipped={}, failed={}",
                    currentYear,
                    result.get("success"),
                    result.get("skipped"),
                    result.get("failed"));
        } catch (Exception e) {
            log.error("[SCHEDULER] ❌ Allocation failed: {}",
                    e.getMessage());
        }

        log.info("[SCHEDULER] ✅ Year-End process complete");
    }

    // ===================== EXISTING =====================
    void triggerYearEndProcessing(Integer forYear) {
        log.info("[MANUAL-TRIGGER] Admin triggered for year: {}",
                forYear);
        try {
            carryForwardService.processYearEndCarryForward(forYear);
            log.info("[MANUAL-TRIGGER] Completed for year: {}",
                    forYear);
        } catch (Exception e) {
            log.error("[MANUAL-TRIGGER] Error: {}", e.getMessage());
            throw new RuntimeException(
                    "Year-end processing failed: " + e.getMessage(), e);
        }
    }
}