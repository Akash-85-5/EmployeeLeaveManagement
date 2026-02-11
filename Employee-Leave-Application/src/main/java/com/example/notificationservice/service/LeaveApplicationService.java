package com.example.notificationservice.service;

import com.example.notificationservice.component.HolidayChecker;
import com.example.notificationservice.dto.LeaveResponse;
import com.example.notificationservice.entity.CompOff;
import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.enums.*;
import com.example.notificationservice.exceptions.BadRequestException;
import com.example.notificationservice.repository.CompOffRepository;
import com.example.notificationservice.repository.EmployeeRepository;
import com.example.notificationservice.repository.LeaveApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;
    private final HolidayChecker holidayChecker;
    private final CompOffService compOffService;
    private final CompOffRepository compOffRepository;

    public LeaveApplicationService(
            LeaveApplicationRepository leaveApplicationRepository,
            NotificationService notificationService,
            EmployeeRepository employeeRepository,
            HolidayChecker holidayChecker,
            CompOffService compOffService,
            CompOffRepository compOffRepository
    ) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.notificationService = notificationService;
        this.employeeRepository = employeeRepository;
        this.holidayChecker = holidayChecker;
        this.compOffService = compOffService;
        this.compOffRepository = compOffRepository;
    }

    // =========================================================
    // APPLY LEAVE (Employee / Manager / Admin)
    // =========================================================
    @Transactional
    public LeaveResponse applyLeave(LeaveApplication leave, boolean confirmLossOfPay) {

        // 1️⃣ Validate dates
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        // 2️⃣ Check for overlapping leaves
        checkLeaveOverlap(leave);

        // 3️⃣ Calculate leave days (with half-day support)
        BigDecimal calculatedDays = calculateLeaveDuration(leave);

        // 4️⃣ Check leave balance
        String warning = checkBalanceAndGetWarning(leave, calculatedDays);
        if (warning != null && !confirmLossOfPay) {
            return new LeaveResponse(null, warning);
        }

        // 5️⃣ Process attachments
        processAttachments(leave);

        // 6️⃣ Set leave days and status
        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.PENDING);

        // 7️⃣ Save leave
        LeaveApplication savedLeave = leaveApplicationRepository.save(leave);

        // 8️⃣ Handle Comp-Off leaves
        if (leave.getLeaveType() == LeaveType.COMP_OFF && warning == null) {
            compOffService.useCompOff(
                    leave.getEmployeeId(),
                    calculatedDays,
                    savedLeave.getId()
            );
        }

        // 9️⃣ Notify next approver safely
        notifyNextApproverSafe(savedLeave);

        return new LeaveResponse(savedLeave, null);
    }

    // =========================================================
    // SAFE ROLE-BASED NOTIFICATIONS
    // =========================================================
    private void notifyNextApproverSafe(LeaveApplication leave) {
        Employee applicant = employeeRepository.findById(leave.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Employee nextApprover = null;

        if (applicant.getRole() == Role.EMPLOYEE) {
            if (applicant.getManagerId() != null) {
                nextApprover = employeeRepository.findById(applicant.getManagerId()).orElse(null);
            }
        } else if (applicant.getRole() == Role.MANAGER || applicant.getRole() == Role.ADMIN) {
            nextApprover = employeeRepository.findByRole(Role.HR).orElse(null);
        }

        if (nextApprover != null) {
            notificationService.createNotification(
                    nextApprover.getId(),
                    nextApprover.getEmail(),
                    EventType.LEAVE_APPLIED,
                    nextApprover.getRole(),
                    Channel.EMAIL,
                    applicant.getName() +
                            " applied leave from " +
                            leave.getStartDate() +
                            " to " +
                            leave.getEndDate()
            );
        }
    }

    // =========================================================
    // CHECK FOR OVERLAPPING LEAVES
    // =========================================================
    private void checkLeaveOverlap(LeaveApplication leave) {
        List<LeaveApplication> overlaps =
                leaveApplicationRepository.findOverlappingLeaves(
                        leave.getEmployeeId(),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        LeaveStatus.PENDING,
                        LeaveStatus.APPROVED
                );

        if (!overlaps.isEmpty()) {
            throw new BadRequestException("Leave dates overlap with an existing leave");
        }
    }

    public List<LeaveApplication> getLeavesByEmployee(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeId(employeeId);
    }

    // =========================================================
    // CANCELLATION
    // =========================================================
    @Transactional
    public void cancelAdminLeave(Long applicationId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave not found"));
        performCancellation(leave);
    }

    @Transactional
    public void cancelEmployeeLeave(Long applicationId, Long employeeId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave not found"));

        if (!leave.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Unauthorized cancellation");
        }

        if (leave.getStatus() == LeaveStatus.REJECTED || leave.getStatus() == LeaveStatus.CANCELLED) {
            throw new BadRequestException("Leave already finalized");
        }

        if (leave.getStatus() == LeaveStatus.APPROVED) {
            throw new BadRequestException("Approved leaves cannot be cancelled");
        }

        performCancellation(leave);
    }

    private void performCancellation(LeaveApplication leave) {
        // Reverse Comp-Off if used
        if (leave.getLeaveType() == LeaveType.COMP_OFF &&
                (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.PENDING)) {

            List<CompOff> credits = compOffRepository.findByUsedLeaveApplicationId(leave.getId());

            for (CompOff credit : credits) {
                credit.setStatus(CompOffStatus.EARNED);
                credit.setUsedLeaveApplicationId(null);
                compOffRepository.save(credit);
            }
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leaveApplicationRepository.save(leave);
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private String checkBalanceAndGetWarning(LeaveApplication leave, BigDecimal calculatedDays) {
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            BigDecimal available = compOffService.getAvailableCompOffDays(leave.getEmployeeId());
            if (available.compareTo(calculatedDays) < 0) {
                return "Insufficient CompOff balance";
            }
        }
        return null;
    }

    private void processAttachments(LeaveApplication leave) {
        if (leave.getAttachments() != null) {
            leave.getAttachments().forEach(a -> {
                // Only store the filename, no IP or port
                a.setFileUrl(a.getFileUrl());
                a.setLeaveApplication(leave);
            });
        }
    }

    public BigDecimal calculateLeaveDuration(LeaveApplication leave) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = leave.getStartDate();

        while (!date.isAfter(leave.getEndDate())) {
            if (!holidayChecker.isNonWorkingDay(date)) {
                total = total.add(getLeaveDayIncrement(leave, date));
            }
            date = date.plusDays(1);
        }

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("All days are non-working");
        }

        return total;
    }

    // Helper for half-day logic
    private BigDecimal getLeaveDayIncrement(LeaveApplication leave, LocalDate date) {
        if (leave.getLeaveType() == LeaveType.HALF_DAY ||
                (leave.getHalfDayType() != null && date.equals(leave.getEndDate()))) {
            return new BigDecimal("0.5");
        }
        return BigDecimal.ONE;
    }
}
