package com.example.employeeLeaveApplication.feature.leave.annual.service;

import com.example.employeeLeaveApplication.feature.holiday.utils.HolidayChecker;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveAttachmentRepository;
import com.example.employeeLeaveApplication.feature.leave.compoff.repository.CompOffRepository;
import com.example.employeeLeaveApplication.feature.notification.service.NotificationService;
import com.example.employeeLeaveApplication.feature.leave.compoff.service.CompOffService;
import com.example.employeeLeaveApplication.feature.separation.service.SeparationService;
import com.example.employeeLeaveApplication.shared.constants.PolicyConstants;
import com.example.employeeLeaveApplication.feature.leave.annual.dto.LeaveResponse;
import com.example.employeeLeaveApplication.feature.leave.compoff.entity.CompOff;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.employee.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.shared.enums.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveApplicationService {

    private final LeaveApplicationRepository   leaveApplicationRepository;
    private final NotificationService           notificationService;
    private final EmployeeRepository            employeeRepository;
    private final HolidayChecker               holidayChecker;
    private final CompOffService               compOffService;
    private final CompOffRepository            compOffRepository;
    private final LeaveAttachmentRepository    leaveAttachmentRepository;
    private final AnnualLeaveBalanceService    annualLeaveBalanceService;
    private final SickLeaveBalanceService      sickLeaveBalanceService;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;

    private final SeparationService separationService;
//    @Value("${aserver.ip}")
//    private String serverIp;
//
//    @Value("${server.port}")
//    private String serverPort;

    public LeaveApplicationService(
            LeaveApplicationRepository leaveApplicationRepository,
            NotificationService notificationService,
            EmployeeRepository employeeRepository,
            HolidayChecker holidayChecker,
            CompOffService compOffService,
            CompOffRepository compOffRepository,
            LeaveAttachmentRepository leaveAttachmentRepository,
            AnnualLeaveBalanceService annualLeaveBalanceService,
            SickLeaveBalanceService sickLeaveBalanceService,
            EmployeePersonalDetailsRepository personalDetailsRepository,
            SeparationService separationService) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.notificationService         = notificationService;
        this.employeeRepository          = employeeRepository;
        this.holidayChecker              = holidayChecker;
        this.compOffService              = compOffService;
        this.compOffRepository           = compOffRepository;
        this.leaveAttachmentRepository   = leaveAttachmentRepository;
        this.annualLeaveBalanceService   = annualLeaveBalanceService;
        this.sickLeaveBalanceService     = sickLeaveBalanceService;
        this.personalDetailsRepository   = personalDetailsRepository;
        this.separationService            = separationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // APPLY LEAVE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LeaveResponse applyLeave(LeaveApplication leave) {
        leave.setYear(leave.getStartDate().getYear());

        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        checkLeaveOverlap(leave);
        checkHolidaysInRange(leave);

        BigDecimal calculatedDays = calculateLeaveDuration(leave);
        leave.setDays(calculatedDays);

        Employee employee = employeeRepository.findById(leave.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        validateLeaveTypeAndBalance(leave, employee, calculatedDays);
        setupApprovalChain(leave, employee);

        // No manager at all → auto-approve
        if (leave.getRequiredApprovalLevels() == 0) {
            leave.setStatus(LeaveStatus.APPROVED);
            leave.setApprovedBy(employee.getId());
            leave.setApprovedRole(employee.getRole());
            leave.setApprovedAt(LocalDateTime.now());
            LeaveApplication saved = leaveApplicationRepository.save(leave);
            applyBalanceDeduction(saved);
            return new LeaveResponse(saved, null);
        }

        leave.setStatus(LeaveStatus.PENDING);
        LeaveApplication saved = leaveApplicationRepository.save(leave);
        notifyFirstApprover(saved, employee);
        return new LeaveResponse(saved, null);
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVAL CHAIN SETUP
    //
    // Level 1 (stored as TEAM_LEADER): employee's direct managerId
    // Level 2 (stored as MANAGER):     level-1 approver's managerId
    //
    // Rules:
    //   - If employee has no managerId → auto-approve (0 levels)
    //   - If level-1 approver has no managerId → 1 level only
    //   - Otherwise → exactly 2 levels (always stop at 2)
    // ═══════════════════════════════════════════════════════════════

    private void setupApprovalChain(LeaveApplication leave, Employee employee) {
        Long firstApproverId = employee.getManagerId();

        if (firstApproverId == null) {
            // Top of hierarchy — auto-approve
            leave.setFirstApproverId(null);
            leave.setSecondApproverId(null);
            leave.setCurrentApprovalLevel(null);
            leave.setRequiredApprovalLevels(0);
            return;
        }

        // Level 1: employee's manager → stored as teamLeaderId, level = TEAM_LEADER
        leave.setFirstApproverId(firstApproverId);
        leave.setCurrentApproverId(firstApproverId);
        leave.setCurrentApprovalLevel(ApprovalLevel.FIRST_APPROVER);

        Employee firstApprover = employeeRepository.findById(firstApproverId)
                .orElseThrow(() -> new RuntimeException(
                        "First approver not found: " + firstApproverId));

        Long secondApproverId = firstApprover.getManagerId();

        if (secondApproverId == null) {
            // Level-1 approver is top of chain → only 1 level
            leave.setSecondApproverId(null);
            leave.setRequiredApprovalLevels(1);
        } else {
            // Level 2: first approver's manager → stored as managerId, level = MANAGER
            // Always stop here — never go deeper
            leave.setSecondApproverId(secondApproverId);
            leave.setRequiredApprovalLevels(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATE LEAVE TYPE AND BALANCE
    // ═══════════════════════════════════════════════════════════════

    private void validateLeaveTypeAndBalance(LeaveApplication leave,
                                             Employee employee,
                                             BigDecimal days) {
        LeaveType type = leave.getLeaveType();
        int year  = leave.getStartDate().getYear();
        int month = leave.getStartDate().getMonthValue();

        switch (leave.getLeaveType()) {
            case SICK         -> validateSickLeave(leave, days, year, month);
            case ANNUAL_LEAVE -> validateAnnualLeave(leave, days, year, month);
            case MATERNITY    -> validateMaternity(leave, employee, days);
            case PATERNITY    -> validatePaternity(leave, employee, days);
            case COMP_OFF     -> validateCompOff(leave, days);
            default -> throw new BadRequestException(
                    "Unsupported leave type: " + leave.getLeaveType());
        }
    }

    /** SICK: cumulative monthly (1.0/month), resets every year. */
    private void validateSickLeave(LeaveApplication leave, BigDecimal days,
                                   int year, int month) {
        double available = sickLeaveBalanceService
                .getAvailableForMonth(leave.getEmployeeId(), year, month);

        if (days.doubleValue() > available) {
            throw new BadRequestException(
                    "Insufficient SICK leave balance for "
                            + java.time.Month.of(month).name() + " " + year
                            + ". Available (cumulative): " + String.format("%.1f", available)
                            + ", Requested: " + days);
        }
    }

    /** ANNUAL_LEAVE: cumulative monthly (1.5/month), carry-forward at year-end. */
    private void validateAnnualLeave(LeaveApplication leave, BigDecimal days,
                                     int year, int month) {
        double available = annualLeaveBalanceService
                .getAvailableForMonth(leave.getEmployeeId(), year, month);

        if (days.doubleValue() > available) {
            throw new BadRequestException(
                    "Insufficient ANNUAL_LEAVE balance for "
                            + java.time.Month.of(month).name() + " " + year
                            + ". Available (cumulative): " + String.format("%.1f", available)
                            + ", Requested: " + days);
        }
    }

    /** MATERNITY: 90 days, FEMALE only, one-time per year. */
    private void validateMaternity(LeaveApplication leave, Employee employee, BigDecimal days) {
        EmployeePersonalDetails details = personalDetailsRepository
                .findByEmployeeId(employee.getId()).orElse(null);

        if (details == null || details.getGender() != Gender.FEMALE) {
            throw new BadRequestException("MATERNITY leave is only available for female employees.");
        }
        if (days.intValue() > PolicyConstants.MATERNITY_DAYS) {
            throw new BadRequestException(
                    "MATERNITY leave cannot exceed " + PolicyConstants.MATERNITY_DAYS + " days.");
        }
        Double used = leaveApplicationRepository.getTotalUsedDaysByType(
                employee.getId(), LeaveStatus.APPROVED,
                leave.getStartDate().getYear(), LeaveType.MATERNITY);
        if (used != null && used > 0) {
            throw new BadRequestException("MATERNITY leave has already been taken this year.");
        }
    }

    /** PATERNITY: 5 days, MALE only, one-time per year. */
    private void validatePaternity(LeaveApplication leave, Employee employee, BigDecimal days) {
        EmployeePersonalDetails details = personalDetailsRepository
                .findByEmployeeId(employee.getId()).orElse(null);

        if (details == null || details.getGender() != Gender.MALE) {
            throw new BadRequestException("PATERNITY leave is only available for male employees.");
        }
        if (days.intValue() > PolicyConstants.PATERNITY_DAYS) {
            throw new BadRequestException(
                    "PATERNITY leave cannot exceed " + PolicyConstants.PATERNITY_DAYS + " days.");
        }
        Double used = leaveApplicationRepository.getTotalUsedDaysByType(
                employee.getId(), LeaveStatus.APPROVED,
                leave.getStartDate().getYear(), LeaveType.PATERNITY);
        if (used != null && used > 0) {
            throw new BadRequestException("PATERNITY leave has already been taken this year.");
        }
    }

    /** COMP_OFF: balance check against earned comp-off days. */
    private void validateCompOff(LeaveApplication leave, BigDecimal days) {
        BigDecimal available = compOffService.getAvailableCompOffDays(leave.getEmployeeId());
        if (available.compareTo(days) < 0) {
            throw new BadRequestException(
                    "Insufficient Comp-Off balance. Available: " + available
                            + ", Requested: " + days);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BALANCE DEDUCTION / RESTORE
    // ═══════════════════════════════════════════════════════════════

    public void applyBalanceDeduction(LeaveApplication leave) {
        double days = leave.getDays().doubleValue();
        int year    = leave.getStartDate().getYear();
        int month   = leave.getStartDate().getMonthValue();
        Long empId  = leave.getEmployeeId();

        switch (leave.getLeaveType()) {
            case ANNUAL_LEAVE ->
                    annualLeaveBalanceService.deductLeave(empId, year, month, days);
            case SICK ->
                    sickLeaveBalanceService.deductLeave(empId, year, month, days);
            case COMP_OFF ->
                    compOffService.useCompOff(empId, leave.getDays(), leave.getId());
            case MATERNITY, PATERNITY -> { /* no balance table */ }
        }
        if (separationService.isInNoticePeriod(empId)) {

            // Half-day should also extend notice
            int extensionDays = (int) Math.ceil(days);

            separationService.extendNoticePeriod(empId, extensionDays);
        }
    }

    public void restoreBalance(LeaveApplication leave) {
        double days = leave.getDays().doubleValue();
        int year    = leave.getStartDate().getYear();
        int month   = leave.getStartDate().getMonthValue();
        Long empId  = leave.getEmployeeId();

        switch (leave.getLeaveType()) {
            case ANNUAL_LEAVE ->
                    annualLeaveBalanceService.restoreLeave(empId, year, month, days);
            case SICK ->
                    sickLeaveBalanceService.restoreLeave(empId, year, month, days);
            case COMP_OFF -> {
                List<CompOff> linked = compOffRepository.findByUsedLeaveApplicationId(leave.getId());
                BigDecimal restored = BigDecimal.ZERO;
                for (CompOff c : linked) {
                    c.setStatus(CompOffStatus.EARNED);
                    c.setUsedLeaveApplicationId(null);
                    compOffRepository.save(c);
                    restored = restored.add(c.getDays());
                }
                if (restored.compareTo(BigDecimal.ZERO) > 0) {
                    compOffService.restoreCompOffBalance(empId, restored);
                }
            }
            case MATERNITY, PATERNITY -> { /* no balance table */ }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL LEAVE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void cancelEmployeeLeave(Long applicationId, Long employeeId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException(
                        "Leave application not found with ID: " + applicationId));

        if (!leave.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Unauthorized: You cannot cancel another employee's leave.");
        }

        if (leave.getStatus() == LeaveStatus.REJECTED
                || leave.getStatus() == LeaveStatus.CANCELLED) {
            throw new BadRequestException("Leave is already finalized as " + leave.getStatus());
        }

        // Restore balance only if was already approved
        if (leave.getStatus() == LeaveStatus.APPROVED) {
            restoreBalance(leave);
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leaveApplicationRepository.save(leave);
    }

    // ═══════════════════════════════════════════════════════════════
    // LEAVE DURATION (unchanged logic — do not modify)
    // ═══════════════════════════════════════════════════════════════

    public BigDecimal calculateLeaveDuration(LeaveApplication leave) {
        LocalDate startDate  = leave.getStartDate();
        LocalDate endDate    = leave.getEndDate();

        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date cannot be before start date.");
        }

        boolean sameDay     = startDate.isEqual(endDate);
        boolean startIsHalf = leave.getStartDateHalfDayType() != null;
        boolean endIsHalf   = leave.getEndDateHalfDayType()   != null;

        if (sameDay) {
            return (startIsHalf || endIsHalf) ? new BigDecimal("0.5") : BigDecimal.ONE;
        }

        long workingDays = holidayChecker.countWorkingDays(startDate, endDate);

        if (workingDays <= 0) {
            throw new BadRequestException(
                    "No working days found in the selected date range. "
                            + "The range falls entirely on weekends or public holidays.");
        }

        BigDecimal days = BigDecimal.valueOf(workingDays);

        if (startIsHalf && !holidayChecker.isNonWorkingDay(startDate)) {
            days = days.subtract(new BigDecimal("0.5"));
        }
        if (endIsHalf && !holidayChecker.isNonWorkingDay(endDate)) {
            days = days.subtract(new BigDecimal("0.5"));
        }

        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Calculated leave duration is zero or negative. "
                            + "Check your dates and half-day selections.");
        }

        return days;
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════

    public LeaveApplication getLeaveById(Long id) {
        return leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(
                        "Leave application not found with ID: " + id));
    }

    public List<LeaveApplication> getLeavesByEmployee(Long employeeId, Pageable pageable) {
        return leaveApplicationRepository.findByEmployeeId(employeeId);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE LEAVE (before approval)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LeaveResponse updateLeave(Long id, Long employeeId,
                                     LocalDate startDate, LocalDate endDate,
                                     String reason,
                                     String startDateHalfDayType,
                                     String endDateHalfDayType) {
        LeaveApplication leave = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(
                        "Leave application not found with ID: " + id));

        if (!leave.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Unauthorized: You can only update your own leaves");
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException(
                    "Can only update PENDING leaves. Current status: " + leave.getStatus());
        }

        if (startDate != null) leave.setStartDate(startDate);
        if (endDate   != null) leave.setEndDate(endDate);
        if (reason    != null && !reason.isEmpty()) leave.setReason(reason);

        if (startDateHalfDayType != null) {
            leave.setStartDateHalfDayType(
                    startDateHalfDayType.isBlank() ? null
                            : HalfDayType.valueOf(startDateHalfDayType.toUpperCase()));
        }
        if (endDateHalfDayType != null) {
            leave.setEndDateHalfDayType(
                    endDateHalfDayType.isBlank() ? null
                            : HalfDayType.valueOf(endDateHalfDayType.toUpperCase()));
        }

        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        BigDecimal calculatedDays = calculateLeaveDuration(leave);
        leave.setDays(calculatedDays);

        return new LeaveResponse(leaveApplicationRepository.save(leave), null);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        LeaveAttachment attachment = leaveAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BadRequestException(
                        "Attachment not found with ID: " + attachmentId));
        leaveAttachmentRepository.delete(attachment);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void checkHolidaysInRange(LeaveApplication leave) {
        LocalDate startDate = leave.getStartDate();
        LocalDate endDate   = leave.getEndDate();

        if (startDate.isEqual(endDate)) {
            String reason = holidayChecker.getNonWorkingDayReason(startDate);
            if (reason != null) {
                throw new BadRequestException(
                        "Cannot apply leave on " + startDate
                                + " — it is a non-working day: " + reason);
            }
            return;
        }
        long workingDays = holidayChecker.countWorkingDays(startDate, endDate);
        if (workingDays == 0) {
            throw new BadRequestException(
                    "The selected date range (" + startDate + " to " + endDate
                            + ") contains no working days.");
        }
    }

    private void checkLeaveOverlap(LeaveApplication leave) {
        List<LeaveApplication> overlaps = leaveApplicationRepository.findOverlappingLeaves(
                leave.getEmployeeId(), leave.getStartDate(), leave.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new BadRequestException("Leave dates overlap with an existing leave");
        }
    }

    private void notifyFirstApprover(LeaveApplication leave, Employee employee) {
        if (leave.getFirstApproverId() == null) return;

        employeeRepository.findById(leave.getFirstApproverId()).ifPresent(approver ->
                notificationService.createNotification(
                        approver.getId(), employee.getEmail(), approver.getEmail(),
                        EventType.LEAVE_APPLIED, approver.getRole(), Channel.EMAIL,
                        employee.getName() + " applied for "
                                + leave.getLeaveType().name() + " leave from "
                                + leave.getStartDate() + " to " + leave.getEndDate()
                                + ". Awaiting your approval.")
        );
    }
}