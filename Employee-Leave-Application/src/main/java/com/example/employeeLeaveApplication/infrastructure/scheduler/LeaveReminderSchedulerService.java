package com.example.employeeLeaveApplication.infrastructure.scheduler;

import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
import com.example.employeeLeaveApplication.feature.notification.entity.LeaveReminder;
import com.example.employeeLeaveApplication.feature.notification.service.NotificationService;
import com.example.employeeLeaveApplication.shared.enums.Channel;
import com.example.employeeLeaveApplication.shared.enums.EventType;
import com.example.employeeLeaveApplication.shared.enums.LeaveStatus;
import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.feature.notification.repository.LeaveReminderRepository;
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

    private static final int URGENCY_MODE_THRESHOLD_DAYS = 7;

    private static final int URGENCY_INITIAL_REMINDER_DAYS = 1;
    private static final int URGENCY_FOLLOW_UP_REMINDER_DAYS = 2;

    private static final int MAX_REMINDERS = 3;

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveReminderRepository leaveReminderRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    public LeaveReminderSchedulerService(
            LeaveApplicationRepository leaveApplicationRepository,
            LeaveReminderRepository leaveReminderRepository,
            EmployeeRepository employeeRepository,
            NotificationService notificationService) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveReminderRepository = leaveReminderRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;

    }

//    @Scheduled(cron = "0 0 9 * * ?")
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void sendPendingLeaveReminders() {
        logger.info("Starting scheduled job: Sending pending leave reminders");

        List<LeaveApplication> pendingLeaves = leaveApplicationRepository
                .findByStatus(LeaveStatus.PENDING);

        logger.info("Found {} pending leave applications", pendingLeaves.size());

        for (LeaveApplication leave : pendingLeaves) {
            try {
                if (leave.getStartDate().isBefore(LocalDate.now())) {
                    logger.warn("Leave ID {} has already started/passed but still pending", leave.getId());
                    continue;
                }

                processLeaveReminder(leave);
            } catch (Exception e) {
                logger.error("Error processing reminder for leave ID: {}", leave.getId(), e);
            }
        }

        logger.info("Completed scheduled job: Sending pending leave reminders");
    }

    private void processLeaveReminder(LeaveApplication leave) {
        long daysUntilLeave = ChronoUnit.DAYS.between(LocalDate.now(), leave.getStartDate());

        long daysSinceApplied = ChronoUnit.DAYS.between(leave.getCreatedAt().toLocalDate(), LocalDate.now());

        logger.debug("Leave ID {}: Days until leave = {}, Days since applied = {}",
                leave.getId(), daysUntilLeave, daysSinceApplied);

        Optional<LeaveReminder> existingReminderOpt =
                leaveReminderRepository.findByLeaveApplicationId(leave.getId());

        if (existingReminderOpt.isPresent()) {
            processFollowUpReminder(leave, existingReminderOpt.get(), daysUntilLeave, daysSinceApplied);
        } else {
            processFirstReminder(leave, daysUntilLeave, daysSinceApplied);
        }
    }

    private void processFirstReminder(LeaveApplication leave, long daysUntilLeave, long daysSinceApplied) {
        logger.info("Leave ID {}: daysUntilLeave={}, daysSinceApplied={}, urgencyMode={}",
                leave.getId(), daysUntilLeave, daysSinceApplied,
                daysUntilLeave <= URGENCY_MODE_THRESHOLD_DAYS);

        boolean shouldSendReminder = false;

        if (daysUntilLeave <= URGENCY_MODE_THRESHOLD_DAYS) {
            if (daysSinceApplied >= URGENCY_INITIAL_REMINDER_DAYS) {
                shouldSendReminder = true;
                logger.info("Leave ID {}: URGENCY MODE - Sending first reminder", leave.getId());
            }
            else {
                logger.info("Leave ID {}: URGENCY MODE - Skipping, daysSinceApplied({}) < threshold({})",
                        leave.getId(), daysSinceApplied, URGENCY_INITIAL_REMINDER_DAYS);
            }
        }
        else {
            long reminderDay = calculateHalfDay(daysUntilLeave);
            if (daysSinceApplied >= reminderDay) {
                shouldSendReminder = true;
                logger.info("Leave ID {}: INTELLIGENT MODE - Sending first reminder at half-day ({} days)",
                        leave.getId(), reminderDay);
            }
            else {
                logger.info("Leave ID {}: INTELLIGENT MODE - Skipping, daysSinceApplied({}) < halfDay({})",
                        leave.getId(), daysSinceApplied, reminderDay);
            }
        }

        if (shouldSendReminder) {
            sendReminderToManager(leave, 1, daysUntilLeave);
            LeaveReminder newReminder = new LeaveReminder();
            newReminder.setLeaveApplicationId(leave.getId());
            newReminder.setReminderCount(1);
            leaveReminderRepository.save(newReminder);
        }
    }

    private void processFollowUpReminder(LeaveApplication leave, LeaveReminder existingReminder,
                                         long daysUntilLeave, long daysSinceApplied) {

        if (existingReminder.getReminderCount() >= MAX_REMINDERS) {
            escalateToHigher(leave);
            logger.info("Leave ID {}: Max reminders ({}) reached", leave.getId(), MAX_REMINDERS);
            return;
        }

        long daysSinceLastReminder = ChronoUnit.DAYS.between(
                existingReminder.getReminderSentAt().toLocalDate(),
                LocalDate.now()
        );

        boolean shouldSendReminder = false;

        if (daysUntilLeave <= URGENCY_MODE_THRESHOLD_DAYS) {
            if (daysSinceLastReminder >= URGENCY_FOLLOW_UP_REMINDER_DAYS) {
                shouldSendReminder = true;
                logger.info("Leave ID {}: URGENCY MODE - Sending follow-up reminder #{}",
                        leave.getId(), existingReminder.getReminderCount() + 1);
            }
        } else {
            long nextReminderDay = calculateHalfDay(daysUntilLeave);

            if (daysSinceLastReminder >= nextReminderDay) {
                shouldSendReminder = true;
                logger.info("Leave ID {}: INTELLIGENT MODE - Sending follow-up reminder #{} at half-day ({} days)",
                        leave.getId(), existingReminder.getReminderCount() + 1, nextReminderDay);
            }
        }

        if (shouldSendReminder) {
            sendReminderToManager(leave, existingReminder.getReminderCount() + 1, daysUntilLeave);

            existingReminder.setReminderCount(existingReminder.getReminderCount() + 1);
            existingReminder.setReminderSentAt(LocalDateTime.now());
            leaveReminderRepository.save(existingReminder);
        }
    }

    private long calculateHalfDay(long daysRemaining) {
        long halfDay = daysRemaining / 2;

        if (halfDay < 1) {
            halfDay = 1;
        }

        return halfDay;
    }

    private void sendReminderToManager(LeaveApplication leave, int reminderCount, long daysUntilLeave) {
        try {
            Employee employee = employeeRepository.findById(leave.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            Employee manager = employeeRepository.findById(employee.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Manager not found"));

            String urgencyLevel = getUrgencyLevel(daysUntilLeave);

            String context = String.format(
                    "%sREMINDER #%d: Employee %s's leave application from %s to %s is still pending. " +
                            "Leave starts in %d day(s). Please review and take action.",
                    urgencyLevel,
                    reminderCount,
                    employee.getName(),
                    leave.getStartDate(),
                    leave.getEndDate(),
                    daysUntilLeave
            );

            notificationService.createNotification(
                    manager.getId(),
                    "crazyyy1235@gmail.com",
                    manager.getEmail(),
                    EventType.PENDING_LEAVE_REMINDER,
                    manager.getRole(),
                    Channel.EMAIL,
                    context
            );

            logger.info("Sent reminder #{} to manager ID: {} for leave ID: {} (Leave starts in {} days)",
                    reminderCount, manager.getId(), leave.getId(), daysUntilLeave);

        } catch (Exception e) {
            logger.error("Failed to send reminder for leave ID: {}", leave.getId(), e);
            throw e;
        }
    }

    private String getUrgencyLevel(long daysUntilLeave) {
        if (daysUntilLeave <= 2) {
            return " URGENT - ";
        } else if (daysUntilLeave <= 5) {
            return " IMPORTANT - ";
        } else if (daysUntilLeave <= 7) {
            return " NOTICE - ";
        } else {
            return "";
        }
    }

    @Transactional
    public void escalateToHigher(LeaveApplication leave) {
        Employee manager = employeeRepository.findById(leave.getManagerId())
                .orElseThrow(() -> new RuntimeException("Manager not found: " + leave.getManagerId()));

        Long higherManagerId = manager.getManagerId();

        if (higherManagerId == null) {
            // Top-level manager — escalate to HR instead
            List<Employee> hrList = employeeRepository.findByRole(Role.HR);
            if (!hrList.isEmpty()) {
                higherManagerId = hrList.get(0).getId();
            }
        }

        leave.setEscalated(true);
        leave.setEscalatedAt(LocalDateTime.now());
        if (higherManagerId != null) {
            leave.setManagerId(higherManagerId);
        }

        leaveApplicationRepository.save(leave);
    }
}
