package com.example.employeeLeaveApplication.feature.noticePeriod.component;

import com.example.employeeLeaveApplication.feature.noticePeriod.entity.EmployeeSeparation;
import com.example.employeeLeaveApplication.feature.noticePeriod.service.EmployeeSeparationService;
import com.example.employeeLeaveApplication.feature.noticePeriod.service.SeparationNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * NoticePeriodScheduler
 *
 * Runs every day at 7:30 AM — after the AttendanceScheduler (7:00 AM)
 * has already processed biometric data and inserted LOP records.
 *
 * Task 1 — Notice period extension (7:30 AM):
 *   For every employee in NOTICE_PERIOD:
 *     - Count new LOP absences since notice start
 *     - Extend noticePeriodEnd by 1 day per new LOP day
 *     - If noticePeriodEnd <= today → mark NOTICE_COMPLETED → notify Admin
 *
 * Task 2 — Absconding alert (8:00 AM):
 *   Find employees absent 7+ consecutive days without approved leave.
 *   Notify HR and Manager so they can verify and file the absconding form.
 */
@Component
public class NoticePeriodScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoticePeriodScheduler.class);

    private final EmployeeSeparationService     separationService;
    private final SeparationNotificationService notificationService;

    public NoticePeriodScheduler(EmployeeSeparationService     separationService,
                                 SeparationNotificationService notificationService) {
        this.separationService   = separationService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 30 7 * * ?")
    @Transactional
    public void processNoticePeriods() {

        LocalDate today = LocalDate.now();
        log.info("=== NoticePeriodScheduler START — {} ===", today);

        List<EmployeeSeparation> activeNotice = separationService.getAllInNoticePeriod();

        for (EmployeeSeparation s : activeNotice) {
            try {
                // Extend for any new LOP absences
                separationService.extendNoticePeriodForLop(s);

                // Re-fetch after potential extension
                if (!s.getNoticePeriodEnd().isAfter(today)) {
                    separationService.markNoticeCompleted(s);
                }
            } catch (Exception ex) {
                log.error("Error processing notice period for employee {}: {}",
                        s.getEmployeeId(), ex.getMessage());
            }
        }

        log.info("=== NoticePeriodScheduler DONE — {} ===", today);
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional(readOnly = true)
    public void checkAbsconders() {

        log.info("=== AbscondingCheck START — {} ===", LocalDate.now());

        List<Long> potentialAbsconders = separationService.getPotentialAbsconders();

        for (Long employeeId : potentialAbsconders) {
            try {
                notificationService.notifyAbscondingAlert(employeeId);
            } catch (Exception ex) {
                log.error("Error sending absconding alert for employee {}: {}",
                        employeeId, ex.getMessage());
            }
        }

        log.info("=== AbscondingCheck DONE — {} potential absconders ===",
                potentialAbsconders.size());
    }
}