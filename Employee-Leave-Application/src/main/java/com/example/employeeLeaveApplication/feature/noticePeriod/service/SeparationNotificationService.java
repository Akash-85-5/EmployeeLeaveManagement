package com.example.employeeLeaveApplication.feature.noticePeriod.service;

import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.feature.noticePeriod.entity.EmployeeSeparation;
import com.example.employeeLeaveApplication.feature.notification.service.NotificationService;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.shared.enums.Channel;
import com.example.employeeLeaveApplication.shared.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeparationNotificationService {

    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    // ═══════════════════════════════════════════════════════
    // RESIGNATION FLOW
    // ═══════════════════════════════════════════════════════

    public void notifyResignationApprovedByManager(EmployeeSeparation s) {
        String context = s.getEmployeeName();

        notifyByRole(Role.CEO, s.getEmployeeId(), EventType.RESIGNATION_APPROVED, context);
        notifyByRole(Role.HR, s.getEmployeeId(), EventType.RESIGNATION_APPROVED, context);
        notifyByRole(Role.ADMIN, s.getEmployeeId(), EventType.RESIGNATION_APPROVED, context);
        notifyByRole(Role.TEAM_LEADER, s.getEmployeeId(), EventType.RESIGNATION_APPROVED, context);
    }

    public void notifyManagerResignationApprovedByHR(EmployeeSeparation s) {
        String context = s.getEmployeeName();

        notifyByRole(Role.CEO, s.getEmployeeId(), EventType.RESIGNATION_APPROVED, context);
        notifyByRole(Role.ADMIN, s.getEmployeeId(), EventType.RESIGNATION_APPROVED, context);
    }

    public void notifyHrResignationApprovedByCEO(EmployeeSeparation s) {
        notifyByRole(Role.ADMIN, s.getEmployeeId(),
                EventType.RESIGNATION_APPROVED,
                s.getEmployeeName());
    }

    // ═══════════════════════════════════════════════════════
    // TERMINATION FLOW
    // ═══════════════════════════════════════════════════════

    public void notifyTerminationInitiated(EmployeeSeparation s) {

        String context = s.getEmployeeName();

        // If HR initiated termination → notify direct manager
        if ("HR".equalsIgnoreCase(s.getInitiatorRole())) {
            notifyDirectManager(s.getEmployeeId(),
                    EventType.TERMINATION_INITIATED,
                    context);
        }

        notifyByRole(Role.CEO, s.getEmployeeId(),
                EventType.TERMINATION_INITIATED, context);

        notifyByRole(Role.ADMIN, s.getEmployeeId(),
                EventType.TERMINATION_INITIATED, context);

        notifyByRole(Role.TEAM_LEADER, s.getEmployeeId(),
                EventType.TERMINATION_INITIATED, context);
    }

    public void notifyTerminationApproved(EmployeeSeparation s) {

        String context = s.getEmployeeName();

        notifyByRole(Role.CEO, s.getEmployeeId(),
                EventType.TERMINATION_APPROVED, context);

        notifyByRole(Role.ADMIN, s.getEmployeeId(),
                EventType.TERMINATION_APPROVED, context);

        notifyByRole(Role.TEAM_LEADER, s.getEmployeeId(),
                EventType.TERMINATION_APPROVED, context);
    }

    // ═══════════════════════════════════════════════════════
    // DEATH IN SERVICE
    // ═══════════════════════════════════════════════════════

    public void notifyDeathInService(EmployeeSeparation s) {

        String context = s.getEmployeeName();

        notifyByRole(Role.CEO, s.getEmployeeId(),
                EventType.DEATH_IN_SERVICE, context);

        notifyByRole(Role.ADMIN, s.getEmployeeId(),
                EventType.DEATH_IN_SERVICE, context);
    }

    // ═══════════════════════════════════════════════════════
    // ABSCONDING FLOW
    // ═══════════════════════════════════════════════════════

    // Auto alert after 7 days absence
    public void notifyAbscondingAlert(Long employeeId) {

        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        String context = emp != null ? emp.getName()
                : "Employee #" + employeeId;

        notifyByRole(Role.HR, employeeId,
                EventType.ABSCONDING_ALERT, context);

        notifyByRole(Role.MANAGER, employeeId,
                EventType.ABSCONDING_ALERT, context);
    }

    // Confirmed absconding — called by EmployeeSeparationService
    public void notifyAbsconding(EmployeeSeparation s) {

        String context = s.getEmployeeName();

        notifyByRole(Role.CEO, s.getEmployeeId(),
                EventType.ABSCONDING_CONFIRMED, context);

        notifyByRole(Role.ADMIN, s.getEmployeeId(),
                EventType.ABSCONDING_CONFIRMED, context);
    }

    // Confirmed absconding (original method kept for internal use)
    public void notifyAbscondingConfirmed(EmployeeSeparation s) {

        String context = s.getEmployeeName();

        notifyByRole(Role.CEO, s.getEmployeeId(),
                EventType.ABSCONDING_CONFIRMED, context);

        notifyByRole(Role.ADMIN, s.getEmployeeId(),
                EventType.ABSCONDING_CONFIRMED, context);
    }

    // ═══════════════════════════════════════════════════════
    // NOTICE PERIOD
    // ═══════════════════════════════════════════════════════

    public void notifyNoticePeriodStarted(EmployeeSeparation s) {

        String context = s.getEmployeeName()
                + " | " + s.getNoticePeriodStart()
                + " to " + s.getNoticePeriodEnd();

        notifyByRole(Role.CFO, s.getEmployeeId(),
                EventType.NOTICE_PERIOD_STARTED, context);
    }

    public void notifyNoticePeriodCompleted(EmployeeSeparation s) {

        notifyByRole(Role.ADMIN, s.getEmployeeId(),
                EventType.NOTICE_PERIOD_COMPLETED,
                s.getEmployeeName());
    }

    // Called by EmployeeSeparationService
    public void notifyNoticeCompleted(EmployeeSeparation s) {
        notifyNoticePeriodCompleted(s);
    }

    // ═══════════════════════════════════════════════════════
    // EXIT & FINAL SETTLEMENT
    // ═══════════════════════════════════════════════════════

    public void notifyExitChecklistCompleted(EmployeeSeparation s) {

        notifyByRole(Role.CFO, s.getEmployeeId(),
                EventType.EXIT_CHECKLIST_COMPLETED,
                s.getEmployeeName());
    }

    // Called by EmployeeSeparationService
    public void notifyExitChecklistComplete(EmployeeSeparation s) {
        notifyExitChecklistCompleted(s);
    }

    public void notifyFinalPayslipGenerated(EmployeeSeparation s) {

        notifyByRole(Role.ADMIN, s.getEmployeeId(),
                EventType.FINAL_PAYSLIP_READY,
                s.getEmployeeName());
    }

    // Called by EmployeeSeparationService
    public void notifyPayslipGeneratedAndRelieve(EmployeeSeparation s) {
        notifyFinalPayslipGenerated(s);
    }

    // ═══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════

    private void notifyByRole(Role role,
                              Long relatedEmployeeId,
                              EventType eventType,
                              String context) {

        List<Employee> recipients = employeeRepository.findByRole(role);

        if (recipients == null || recipients.isEmpty()) {
            log.warn("No recipients found for role: {}", role);
            return;
        }

        for (Employee recipient : recipients) {

            notificationService.createNotification(
                    recipient.getId(),      // receiver
                    null,                   // fromEmail
                    recipient.getEmail(),   // toEmail
                    eventType,
                    role,
                    Channel.IN_APP,
                    context
            );
        }
    }

    private void notifyDirectManager(Long employeeId,
                                     EventType eventType,
                                     String context) {

        Employee emp = employeeRepository.findById(employeeId).orElse(null);

        if (emp == null || emp.getManagerId() == null) {
            return;
        }

        Employee manager =
                employeeRepository.findById(emp.getManagerId()).orElse(null);

        if (manager == null) {
            return;
        }

        notificationService.createNotification(
                manager.getId(),
                null,
                manager.getEmail(),
                eventType,
                Role.MANAGER,
                Channel.IN_APP,
                context
        );
    }
}