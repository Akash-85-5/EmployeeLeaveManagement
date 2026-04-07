package com.emp_management.infrastructure.scheduler;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.workfromhome.entity.WfhReminder;
import com.emp_management.feature.workfromhome.entity.WfhRequest;
import com.emp_management.feature.workfromhome.repository.WfhReminderRepository;
import com.emp_management.feature.workfromhome.repository.WfhRepository;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.RequestStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WfhReminderSchedulerService {

    private static final int MAX_REMINDERS = 3;

    private final WfhRepository wfhRepository;
    private final WfhReminderRepository reminderRepository;
    private final EmployeeRepository employeeRepository;

    public WfhReminderSchedulerService(
            WfhRepository wfhRepository,
            WfhReminderRepository reminderRepository,
            EmployeeRepository employeeRepository) {
        this.wfhRepository = wfhRepository;
        this.reminderRepository = reminderRepository;
        this.employeeRepository = employeeRepository;
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void processWfhRequests() {

        List<WfhRequest> list = wfhRepository.findByStatus(RequestStatus.PENDING);

        for (WfhRequest wfh : list) {

            WfhReminder reminder = reminderRepository
                    .findByWfhRequestId(wfh.getId())
                    .orElse(null);

            if (reminder == null) {
                sendFirstReminder(wfh);
            } else if (reminder.getReminderCount() >= MAX_REMINDERS) {
                escalate(wfh, reminder);
            } else {
                sendFollowUp(reminder); // ✅ FIXED (removed unused wfh)
            }
        }
    }

    private void sendFirstReminder(WfhRequest wfh) {

        WfhReminder r = new WfhReminder();
        r.setWfhRequestId(wfh.getId());
        r.setReminderCount(1);
        r.setReminderSentAt(LocalDateTime.now());
        r.setApprovalLevelAtReminder(wfh.getCurrentApprovalLevel());

        reminderRepository.save(r);
    }

    // ✅ FIXED: removed unused parameter
    private void sendFollowUp(WfhReminder r) {

        r.setReminderCount(r.getReminderCount() + 1);
        r.setReminderSentAt(LocalDateTime.now());

        reminderRepository.save(r);
    }

    private void escalate(WfhRequest wfh, WfhReminder r) {

        if (wfh.getCurrentApprovalLevel() == ApprovalLevel.FIRST_APPROVER) {

            if (wfh.getSecondApproverId() == null) {
                autoReject(wfh);
                return;
            }

            wfh.setCurrentApproverId(wfh.getSecondApproverId());
            wfh.setCurrentApprovalLevel(ApprovalLevel.SECOND_APPROVER);
            wfh.setEscalated(true);
            wfh.setEscalatedAt(LocalDateTime.now());

        } else {

            Employee emp = employeeRepository
                    .findByEmpId(wfh.getCurrentApproverId())
                    .orElse(null);

            if (emp == null || emp.getReportingId() == null) {
                autoReject(wfh);
                return;
            }

            wfh.setCurrentApproverId(emp.getReportingId());
            wfh.setEscalated(true);
        }

        wfhRepository.save(wfh);

        r.setReminderCount(0);
        reminderRepository.save(r);
    }

    private void autoReject(WfhRequest wfh) {

        wfh.setStatus(RequestStatus.REJECTED);
        wfh.setApprovedBy("SYSTEM");
        wfh.setApprovedAt(LocalDateTime.now());

        wfhRepository.save(wfh);
    }
}