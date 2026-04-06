package com.emp_management.infrastructure.scheduler;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.notification.service.NotificationService;
import com.emp_management.feature.od.entity.ODRequest;
import com.emp_management.feature.od.repository.ODRequestRepository;
import com.emp_management.feature.od.service.ODService;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.Channel;
import com.emp_management.shared.enums.EventType;
import com.emp_management.shared.enums.RequestStatus;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job that sends reminders for PENDING OD requests and escalates
 * when an approver has not acted within the configured threshold.
 *
 * The escalation logic lives in ODService.escalate() — this scheduler only
 * decides WHEN to trigger it, mirroring how LeaveReminderSchedulerService
 * delegates to LeaveApplicationRepository + LeaveApplication state.
 *
 * Reminder state is tracked directly on ODRequest using simple counters
 * (no separate LeaveReminder-style entity needed; the state is small enough
 * to inline). If you add an ODReminder entity later, the pattern is identical
 * to LeaveReminderSchedulerService.
 */
@Service
public class ODReminderSchedulerService {

    private static final Logger logger =
            LoggerFactory.getLogger(ODReminderSchedulerService.class);

    // ── Thresholds (same values as LeaveReminderSchedulerService) ─
    private static final int URGENCY_THRESHOLD_DAYS     = 7;
    private static final int URGENCY_INITIAL_DAYS       = 1;
    private static final int URGENCY_FOLLOW_UP_DAYS     = 2;
    private static final int MAX_REMINDERS_PER_APPROVER = 3;

    private final ODRequestRepository odRepository;
    private final ODService           odService;
    private final EmployeeRepository  employeeRepository;
    private final NotificationService notificationService;

    public ODReminderSchedulerService(ODRequestRepository odRepository,
                                      ODService odService,
                                      EmployeeRepository employeeRepository,
                                      NotificationService notificationService) {
        this.odRepository      = odRepository;
        this.odService         = odService;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
    }

    // ── Run daily at 9 AM (same cron as leave scheduler) ─────────

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendPendingODReminders() {
        logger.info("[OD-SCHEDULER] Starting pending OD reminder job");

        List<ODRequest> pendingODs = odRepository.findByStatus(RequestStatus.PENDING);
        logger.info("[OD-SCHEDULER] Found {} pending OD requests", pendingODs.size());

        for (ODRequest od : pendingODs) {
            try {
                // OD start date has already passed — still remind (retroactive ODs)
                processODReminder(od);
            } catch (Exception e) {
                logger.error("[OD-SCHEDULER] Error processing OD ID {}", od.getId(), e);
            }
        }

        logger.info("[OD-SCHEDULER] Completed pending OD reminder job");
    }

    // ═══════════════════════════════════════════════════════════════
    // CORE PROCESSING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reminder state is stored as two fields on ODRequest itself:
     *   - reminderCount      : how many reminders sent to the current approver
     *   - lastReminderSentAt : when the last reminder was sent
     *
     * NOTE: If you prefer a separate ODReminder entity (like LeaveReminder),
     * the logic below maps 1-to-1 to LeaveReminderSchedulerService — just
     * replace the field access with entity access.
     */
    private void processODReminder(ODRequest od) {
        long daysUntilStart   = ChronoUnit.DAYS.between(LocalDate.now(), od.getStartDate());
        long daysSinceApplied = ChronoUnit.DAYS.between(
                od.getCreatedAt().toLocalDate(), LocalDate.now());

        int  reminderCount      = od.getReminderCount() != null ? od.getReminderCount() : 0;
        LocalDateTime lastSent  = od.getLastReminderSentAt();

        if (reminderCount == 0) {
            // ── First reminder ────────────────────────────────────
            boolean shouldSend;
            if (Math.abs(daysUntilStart) <= URGENCY_THRESHOLD_DAYS) {
                shouldSend = daysSinceApplied >= URGENCY_INITIAL_DAYS;
            } else {
                shouldSend = daysSinceApplied >= calculateHalfDay(Math.abs(daysUntilStart));
            }

            if (!shouldSend) return;

            sendReminderToCurrentApprover(od, 1, daysUntilStart);
            od.setReminderCount(1);
            od.setLastReminderSentAt(LocalDateTime.now());
            od.setReminderLevelSnapshot(od.getCurrentApprovalLevel());
            odRepository.save(od);

            logger.info("[OD-SCHEDULER] OD ID {}: Sent first reminder to {} (level: {})",
                    od.getId(), od.getCurrentApproverId(), od.getCurrentApprovalLevel());

        } else {
            // ── Follow-up or escalate ─────────────────────────────

            // Approver level changed between runs (previous approver acted) — reset cycle
            if (od.getReminderLevelSnapshot() != null
                    && od.getReminderLevelSnapshot() != od.getCurrentApprovalLevel()) {
                logger.info("[OD-SCHEDULER] OD ID {}: Approval level changed — resetting reminder cycle",
                        od.getId());
                od.setReminderCount(0);
                od.setReminderLevelSnapshot(od.getCurrentApprovalLevel());
                odRepository.save(od);
                return;
            }

            if (reminderCount >= MAX_REMINDERS_PER_APPROVER) {
                // Escalate — delegation to ODService keeps escalation logic centralised
                logger.info("[OD-SCHEDULER] OD ID {}: Max reminders reached — escalating", od.getId());
                odService.escalate(od);
                // Reset reminder state for new approver
                od.setReminderCount(0);
                od.setLastReminderSentAt(LocalDateTime.now());
                od.setReminderLevelSnapshot(od.getCurrentApprovalLevel());
                odRepository.save(od);
                return;
            }

            // Follow-up timing
            long daysSinceLastReminder = lastSent != null
                    ? ChronoUnit.DAYS.between(lastSent.toLocalDate(), LocalDate.now())
                    : Long.MAX_VALUE;

            boolean shouldSend;
            if (Math.abs(daysUntilStart) <= URGENCY_THRESHOLD_DAYS) {
                shouldSend = daysSinceLastReminder >= URGENCY_FOLLOW_UP_DAYS;
            } else {
                shouldSend = daysSinceLastReminder >= calculateHalfDay(Math.abs(daysUntilStart));
            }

            if (!shouldSend) return;

            int nextCount = reminderCount + 1;
            sendReminderToCurrentApprover(od, nextCount, daysUntilStart);
            od.setReminderCount(nextCount);
            od.setLastReminderSentAt(LocalDateTime.now());
            odRepository.save(od);

            logger.info("[OD-SCHEDULER] OD ID {}: Sent follow-up reminder #{} to {} (level: {})",
                    od.getId(), nextCount, od.getCurrentApproverId(),
                    od.getCurrentApprovalLevel());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SEND REMINDER
    // ═══════════════════════════════════════════════════════════════

    private void sendReminderToCurrentApprover(ODRequest od,
                                               int reminderCount,
                                               long daysUntilStart) {
        Employee approver = employeeRepository.findByEmpId(od.getCurrentApproverId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Approver not found: " + od.getCurrentApproverId()));

        Employee employee = employeeRepository.findByEmpId(od.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee not found: " + od.getEmployeeId()));

        String urgency = getUrgencyLabel(Math.abs(daysUntilStart));

        String message = String.format(
                "%sREMINDER #%d: %s's OD request (%s to %s) is still PENDING your decision. " +
                        "Please approve or reject promptly.",
                urgency,
                reminderCount,
                employee.getName(),
                od.getStartDate(),
                od.getEndDate());

        notificationService.createNotification(
                approver.getEmpId(),
                "info@yourcompany.com",
                approver.getEmail(),
                EventType.PENDING_LEAVE_REMINDER,  // reuse existing event type
                Channel.EMAIL,
                message);
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private long calculateHalfDay(long days) {
        return Math.max(days / 2, 1);
    }

    private String getUrgencyLabel(long days) {
        if (days <= 2) return "URGENT - ";
        if (days <= 5) return "IMPORTANT - ";
        if (days <= 7) return "NOTICE - ";
        return "";
    }
}