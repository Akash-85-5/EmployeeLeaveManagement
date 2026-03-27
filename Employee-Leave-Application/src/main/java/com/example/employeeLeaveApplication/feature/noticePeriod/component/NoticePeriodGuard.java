package com.example.employeeLeaveApplication.feature.noticePeriod.component;

import com.example.employeeLeaveApplication.feature.noticePeriod.entity.EmployeeSeparation;
import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import com.example.employeeLeaveApplication.feature.noticePeriod.repository.EmployeeSeparationRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NoticePeriodGuard
 *
 * Blocks employees from applying for leave or WFH while in notice period.
 *
 * ══════════════════════════════════════════════════════
 * HOW TO ADD THIS TO EXISTING LEAVE SERVICE:
 * ══════════════════════════════════════════════════════
 * 1. Inject this bean into your LeaveApplicationService:
 *      private final NoticePeriodGuard noticePeriodGuard;
 *
 * 2. Add ONE line at the top of your applyLeave() method:
 *      noticePeriodGuard.guardLeave(employeeId);
 *
 * ══════════════════════════════════════════════════════
 * HOW TO ADD THIS TO EXISTING WFH SERVICE:
 * ══════════════════════════════════════════════════════
 * Same pattern — add ONE line at the top of your applyWfh() method:
 *      noticePeriodGuard.guardWfh(employeeId);
 */
@Component
public class NoticePeriodGuard {

    private final EmployeeSeparationRepository separationRepo;

    public NoticePeriodGuard(EmployeeSeparationRepository separationRepo) {
        this.separationRepo = separationRepo;
    }

    // Throws if employee is in notice period — call at start of leave apply
    public void guardLeave(Long employeeId) {
        if (isInNoticePeriod(employeeId)) {
            throw new RuntimeException(
                    "Leave application not allowed. Employee " + employeeId
                            + " is currently in notice period.");
        }
    }

    // Throws if employee is in notice period — call at start of WFH apply
    public void guardWfh(Long employeeId) {
        if (isInNoticePeriod(employeeId)) {
            throw new RuntimeException(
                    "WFH application not allowed. Employee " + employeeId
                            + " is currently in notice period.");
        }
    }

    // Returns true if employee has an active NOTICE_PERIOD record
    public boolean isInNoticePeriod(Long employeeId) {
        List<EmployeeSeparation> active = separationRepo.findActiveByEmployeeId(employeeId);
        return active.stream()
                .anyMatch(s -> s.getStatus() == SeparationStatus.NOTICE_PERIOD);
    }
}