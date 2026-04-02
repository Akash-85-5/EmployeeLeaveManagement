package com.emp_management.infrastructure.scheduler;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.repository.LeaveApplicationRepository;
import com.emp_management.feature.notification.entity.LeaveReminder;
import com.emp_management.feature.notification.repository.LeaveReminderRepository;
import com.emp_management.feature.notification.service.NotificationService;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.Channel;
import com.emp_management.shared.enums.EventType;
import com.emp_management.shared.enums.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;


@Service
public class LeaveReminderSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveReminderSchedulerService.class);

    // ── Thresholds ────────────────────────────────────────────────
    private static final int URGENCY_THRESHOLD_DAYS        = 7;
    private static final int URGENCY_INITIAL_DAYS          = 1;
    private static final int URGENCY_FOLLOW_UP_DAYS        = 2;
    private static final int MAX_REMINDERS_PER_APPROVER    = 3;

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveReminderRepository    leaveReminderRepository;
    private final EmployeeRepository         employeeRepository;
    private final NotificationService        notificationService;

    public LeaveReminderSchedulerService(
            LeaveApplicationRepository leaveApplicationRepository,
            LeaveReminderRepository leaveReminderRepository,
            EmployeeRepository employeeRepository,
            NotificationService notificationService) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveReminderRepository    = leaveReminderRepository;
        this.employeeRepository         = employeeRepository;
        this.notificationService        = notificationService;
    }

    @Scheduled(cron = "0 0 9 * * ?")   // 9 AM daily
    @Transactional
    public void sendPendingLeaveReminders() {
        logger.info("[SCHEDULER] Starting pending leave reminder job");

        List<LeaveApplication> pendingLeaves =
                leaveApplicationRepository.findByStatus(RequestStatus.PENDING);

        logger.info("[SCHEDULER] Found {} pending leave applications", pendingLeaves.size());

        for (LeaveApplication leave : pendingLeaves) {
            try {
                // Skip leaves that have already started — too late to remind
                if (leave.getStartDate().isBefore(LocalDate.now())) {
                    logger.warn("[SCHEDULER] Leave ID {} already started but still PENDING — skipping",
                            leave.getId());
                    continue;
                }
                processLeaveReminder(leave);
            } catch (Exception e) {
                logger.error("[SCHEDULER] Error processing leave ID {}", leave.getId(), e);
            }
        }

        logger.info("[SCHEDULER] Completed pending leave reminder job");
    }

    // ═══════════════════════════════════════════════════════════════
    // CORE PROCESSING
    // ═══════════════════════════════════════════════════════════════

    private void processLeaveReminder(LeaveApplication leave) {
        long daysUntilLeave   = ChronoUnit.DAYS.between(LocalDate.now(), leave.getStartDate());
        long daysSinceApplied = ChronoUnit.DAYS.between(
                leave.getCreatedAt().toLocalDate(), LocalDate.now());

        LeaveReminder reminder = leaveReminderRepository
                .findByLeaveApplicationId(leave.getId())
                .orElse(null);

        if (reminder == null) {
            processFirstReminder(leave, daysUntilLeave, daysSinceApplied);
        } else {
            processFollowUpOrEscalate(leave, reminder, daysUntilLeave);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FIRST REMINDER
    // ═══════════════════════════════════════════════════════════════

    private void processFirstReminder(LeaveApplication leave,
                                      long daysUntilLeave,
                                      long daysSinceApplied) {
        boolean shouldSend;

        if (daysUntilLeave <= URGENCY_THRESHOLD_DAYS) {
            shouldSend = daysSinceApplied >= URGENCY_INITIAL_DAYS;
        } else {
            shouldSend = daysSinceApplied >= calculateHalfDay(daysUntilLeave);
        }

        if (!shouldSend) {
            logger.debug("[SCHEDULER] Leave ID {}: First reminder not due yet", leave.getId());
            return;
        }

        sendReminderToCurrentApprover(leave, 1, daysUntilLeave);

        LeaveReminder newReminder = new LeaveReminder();
        newReminder.setLeaveApplicationId(leave.getId());
        newReminder.setReminderCount(1);
        newReminder.setReminderSentAt(LocalDateTime.now());
        // Track which approver level this reminder cycle is for
        newReminder.setApprovalLevelAtReminder(leave.getCurrentApprovalLevel());
        leaveReminderRepository.save(newReminder);

        logger.info("[SCHEDULER] Leave ID {}: Sent first reminder to {} (level: {})",
                leave.getId(), leave.getCurrentApproverId(), leave.getCurrentApprovalLevel());
    }

    // ═══════════════════════════════════════════════════════════════
    // FOLLOW-UP OR ESCALATE
    //
    // Two escalation scenarios:
    //
    // Case 1: First approver didn't respond after MAX_REMINDERS
    //         → escalate to second approver (if exists)
    //         → reset reminder count, update currentApproverId + level
    //
    // Case 2: Second approver (or escalated approver) didn't respond
    //         → escalate to their reportingId (if exists)
    //         → if no reportingId → auto-reject
    // ═══════════════════════════════════════════════════════════════

    private void processFollowUpOrEscalate(LeaveApplication leave,
                                           LeaveReminder reminder,
                                           long daysUntilLeave) {

        // ── Check if approver changed since last reminder cycle ───
        // This handles the case where first approver approved and
        // leave advanced to second approver between scheduler runs
        boolean approverLevelChanged = reminder.getApprovalLevelAtReminder() != null
                && reminder.getApprovalLevelAtReminder() != leave.getCurrentApprovalLevel();

        if (approverLevelChanged) {
            // Level advanced by approval — reset reminder cycle for new approver
            logger.info("[SCHEDULER] Leave ID {}: Approval level changed to {} — resetting reminder cycle",
                    leave.getId(), leave.getCurrentApprovalLevel());
            reminder.setReminderCount(0);
            reminder.setApprovalLevelAtReminder(leave.getCurrentApprovalLevel());
            leaveReminderRepository.save(reminder);
        }

        if (reminder.getReminderCount() >= MAX_REMINDERS_PER_APPROVER) {
            // Current approver exhausted — escalate
            escalate(leave, reminder);
            return;
        }

        // ── Check if it's time for next follow-up ─────────────────
        long daysSinceLastReminder = ChronoUnit.DAYS.between(
                reminder.getReminderSentAt().toLocalDate(), LocalDate.now());

        boolean shouldSend;
        if (daysUntilLeave <= URGENCY_THRESHOLD_DAYS) {
            shouldSend = daysSinceLastReminder >= URGENCY_FOLLOW_UP_DAYS;
        } else {
            shouldSend = daysSinceLastReminder >= calculateHalfDay(daysUntilLeave);
        }

        if (!shouldSend) {
            logger.debug("[SCHEDULER] Leave ID {}: Follow-up reminder not due yet", leave.getId());
            return;
        }

        int nextCount = reminder.getReminderCount() + 1;
        sendReminderToCurrentApprover(leave, nextCount, daysUntilLeave);

        reminder.setReminderCount(nextCount);
        reminder.setReminderSentAt(LocalDateTime.now());
        leaveReminderRepository.save(reminder);

        logger.info("[SCHEDULER] Leave ID {}: Sent follow-up reminder #{} to {} (level: {})",
                leave.getId(), nextCount, leave.getCurrentApproverId(),
                leave.getCurrentApprovalLevel());
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCALATION LOGIC
    //
    // FIRST_APPROVER timed out → move to SECOND_APPROVER
    // SECOND_APPROVER timed out → move to their reportingId
    // No reportingId → auto-reject
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void escalate(LeaveApplication leave, LeaveReminder reminder) {
        ApprovalLevel currentLevel = leave.getCurrentApprovalLevel();
        logger.info("[ESCALATION] Leave ID {}: Escalating from level {}", leave.getId(), currentLevel);

        if (currentLevel == ApprovalLevel.FIRST_APPROVER) {
            escalateFromFirstToSecond(leave, reminder);
        } else {
            // SECOND_APPROVER or higher — escalate up the chain
            escalateUpTheChain(leave, reminder);
        }
    }

    private void escalateFromFirstToSecond(LeaveApplication leave, LeaveReminder reminder) {
        String secondApproverId = leave.getSecondApproverId();

        if (secondApproverId == null) {
            // Only 1 level required and first approver timed out → auto-reject
            logger.warn("[ESCALATION] Leave ID {}: No second approver. Auto-rejecting.", leave.getId());
            autoReject(leave);
            return;
        }

        // Advance to second approver — reuse existing approval flow fields
        leave.setCurrentApproverId(secondApproverId);
        leave.setCurrentApprovalLevel(ApprovalLevel.SECOND_APPROVER);
        leave.setEscalated(true);
        leave.setEscalatedAt(LocalDateTime.now());
        leaveApplicationRepository.save(leave);

        // Reset reminder cycle for new approver
        reminder.setReminderCount(0);
        reminder.setReminderSentAt(LocalDateTime.now());
        reminder.setApprovalLevelAtReminder(ApprovalLevel.SECOND_APPROVER);
        leaveReminderRepository.save(reminder);

        // Notify the second approver
        notifyEscalatedApprover(leave, secondApproverId,
                "This leave was escalated to you because the previous approver did not respond.");

        logger.info("[ESCALATION] Leave ID {}: Escalated to second approver {}",
                leave.getId(), secondApproverId);
    }

    private void escalateUpTheChain(LeaveApplication leave, LeaveReminder reminder) {
        String currentApproverId = leave.getCurrentApproverId();

        Employee currentApprover = employeeRepository.findByEmpId(currentApproverId)
                .orElse(null);

        if (currentApprover == null) {
            logger.error("[ESCALATION] Leave ID {}: Current approver {} not found. Auto-rejecting.",
                    leave.getId(), currentApproverId);
            autoReject(leave);
            return;
        }

        String nextApproverId = currentApprover.getReportingId();

        if (nextApproverId == null) {
            // Top of chain — no one above. Auto-reject.
            logger.warn("[ESCALATION] Leave ID {}: Approver {} is top of chain. Auto-rejecting.",
                    leave.getId(), currentApproverId);
            autoReject(leave);
            return;
        }

        // Move to the next approver up the chain
        leave.setCurrentApproverId(nextApproverId);
        // Keep level as SECOND_APPROVER — it's still a higher-level decision
        leave.setEscalated(true);
        leave.setEscalatedAt(LocalDateTime.now());
        leaveApplicationRepository.save(leave);

        // Reset reminder cycle for new approver
        reminder.setReminderCount(0);
        reminder.setReminderSentAt(LocalDateTime.now());
        reminder.setApprovalLevelAtReminder(leave.getCurrentApprovalLevel());
        leaveReminderRepository.save(reminder);

        notifyEscalatedApprover(leave, nextApproverId,
                "This leave has been escalated to you as the previous approver did not respond.");

        logger.info("[ESCALATION] Leave ID {}: Escalated up chain to approver {}",
                leave.getId(), nextApproverId);
    }

    private void autoReject(LeaveApplication leave) {
        leave.setStatus(RequestStatus.REJECTED);
        leave.setApprovedBy("SYSTEM");
        leave.setApprovedRole("SYSTEM");
        leave.setApprovedAt(LocalDateTime.now());
        leave.setEscalated(false);
        leaveApplicationRepository.save(leave);

        // Notify the employee
        employeeRepository.findByEmpId(leave.getEmployeeId()).ifPresent(emp ->
                notificationService.createNotification(
                        emp.getEmpId(),
                        "info@wenxttech.com",
                        emp.getEmail(),
                        EventType.LEAVE_REJECTED,
                        Channel.EMAIL,
                        "Your " + leave.getLeaveType().getLeaveType() + " leave from "
                                + leave.getStartDate() + " to " + leave.getEndDate()
                                + " was auto-rejected because no approver responded in time.")
        );

        logger.info("[ESCALATION] Leave ID {}: Auto-rejected by system", leave.getId());
    }

    // ═══════════════════════════════════════════════════════════════
    // SEND REMINDER TO CURRENT APPROVER
    // ═══════════════════════════════════════════════════════════════

    private void sendReminderToCurrentApprover(LeaveApplication leave,
                                               int reminderCount,
                                               long daysUntilLeave) {
        String currentApproverId = leave.getCurrentApproverId();

        Employee approver = employeeRepository.findByEmpId(currentApproverId)
                .orElseThrow(() -> new RuntimeException(
                        "Approver not found: " + currentApproverId));

        Employee employee = employeeRepository.findByEmpId(leave.getEmployeeId())
                .orElseThrow(() -> new RuntimeException(
                        "Employee not found: " + leave.getEmployeeId()));

        String urgency = getUrgencyLabel(daysUntilLeave);

        String message = String.format(
                "%sREMINDER #%d: %s's %s leave (%s to %s) is still PENDING your decision. " +
                        "Leave starts in %d day(s). Please approve or reject.",
                urgency,
                reminderCount,
                employee.getName(),
                leave.getLeaveType().getLeaveType(),
                leave.getStartDate(),
                leave.getEndDate(),
                daysUntilLeave);

        notificationService.createNotification(
                approver.getEmpId(),
                "info@wenxttech.com",
                approver.getEmail(),
                EventType.PENDING_LEAVE_REMINDER,
                Channel.EMAIL,
                message);
    }

    private void notifyEscalatedApprover(LeaveApplication leave,
                                         String newApproverId,
                                         String escalationReason) {
        employeeRepository.findByEmpId(newApproverId).ifPresent(approver -> {
            employeeRepository.findByEmpId(leave.getEmployeeId()).ifPresent(emp -> {
                String message = String.format(
                        "ESCALATED: %s's %s leave (%s to %s) requires your decision. %s",
                        emp.getName(),
                        leave.getLeaveType().getLeaveType(),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        escalationReason);

                notificationService.createNotification(
                        approver.getEmpId(),
                        "info@wenxttech.com",
                        approver.getEmail(),
                        EventType.PENDING_LEAVE_REMINDER,
                        Channel.EMAIL,
                        message);
            });
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private long calculateHalfDay(long daysRemaining) {
        return Math.max(daysRemaining / 2, 1);
    }

    private String getUrgencyLabel(long daysUntilLeave) {
        if (daysUntilLeave <= 2) return "URGENT - ";
        if (daysUntilLeave <= 5) return "IMPORTANT - ";
        if (daysUntilLeave <= 7) return "NOTICE - ";
        return "";
    }
}
