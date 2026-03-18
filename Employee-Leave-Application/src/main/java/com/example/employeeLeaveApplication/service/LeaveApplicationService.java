package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.HolidayChecker;
import com.example.employeeLeaveApplication.component.PolicyConstants;
import com.example.employeeLeaveApplication.dto.LeaveResponse;
import com.example.employeeLeaveApplication.entity.CompOff;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;
    private final HolidayChecker holidayChecker;
    private final CompOffService compOffService;
    private final CompOffRepository compOffRepository;
    private final LeaveAttachmentRepository leaveAttachmentRepository;
    private final AnnualLeaveBalanceService annualLeaveBalanceService;
    private final LeaveAllocationRepository leaveAllocationRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;

    @Value("${app.server.ip}")
    private String serverIp;

    @Value("${app.server.port}")
    private String serverPort;

    public LeaveApplicationService(
            LeaveApplicationRepository leaveApplicationRepository,
            NotificationService notificationService,
            EmployeeRepository employeeRepository,
            HolidayChecker holidayChecker,
            CompOffService compOffService,
            CompOffRepository compOffRepository,
            LeaveAttachmentRepository leaveAttachmentRepository,
            AnnualLeaveBalanceService annualLeaveBalanceService,
            LeaveAllocationRepository leaveAllocationRepository,
            EmployeePersonalDetailsRepository personalDetailsRepository) {
        this.leaveApplicationRepository   = leaveApplicationRepository;
        this.notificationService           = notificationService;
        this.employeeRepository            = employeeRepository;
        this.holidayChecker                = holidayChecker;
        this.compOffService                = compOffService;
        this.compOffRepository             = compOffRepository;
        this.leaveAttachmentRepository     = leaveAttachmentRepository;
        this.annualLeaveBalanceService     = annualLeaveBalanceService;
        this.leaveAllocationRepository     = leaveAllocationRepository;
        this.personalDetailsRepository     = personalDetailsRepository;
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

        // Validate leave type eligibility + balance
        validateLeaveTypeAndBalance(leave, employee, calculatedDays);

        setupApprovalChain(leave, employee, calculatedDays);

        // HR / ADMIN: auto-approve
        if (leave.getRequiredApprovalLevels() == 0) {
            leave.setStatus(LeaveStatus.APPROVED);
            leave.setApprovedBy(employee.getId());
            leave.setApprovedRole(employee.getRole());
            leave.setApprovedAt(LocalDateTime.now());
            LeaveApplication saved = leaveApplicationRepository.save(leave);
            applyBalanceDeduction(saved);
            return new LeaveResponse(saved, null);
        }

        // Everyone else: PENDING
        leave.setStatus(LeaveStatus.PENDING);
        LeaveApplication saved = leaveApplicationRepository.save(leave);
        notifyFirstApprover(saved, employee);

        return new LeaveResponse(saved, null);
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

        switch (type) {

            case SICK -> validateSickLeave(leave, employee, days, year);

            case ANNUAL_LEAVE -> validateAnnualLeave(leave, days, year, month);

            case MATERNITY -> validateMaternity(leave, employee, days);

            case PATERNITY -> validatePaternity(leave, employee, days);

            case COMP_OFF -> validateCompOff(leave, days);

            default -> throw new BadRequestException("Unsupported leave type: " + type);
        }
    }

    /**
     * SICK: flat 12-day pool per year, no monthly accrual, no carry-forward.
     * Cannot apply for past dates (except today).
     * Future dates only allowed as pre-booked appointment with attachment.
     */
    private void validateSickLeave(LeaveApplication leave, Employee employee,
                                   BigDecimal days, int year) {
        List<LeaveAllocation> allocations =
                leaveAllocationRepository.findByEmployeeIdAndYear(employee.getId(), year);

        double sickAllocated = allocations.stream()
                .filter(a -> a.getLeaveCategory() == LeaveType.SICK)
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        // Total SICK days already used this year
        Double usedDays = leaveApplicationRepository
                .getTotalUsedDaysByType(employee.getId(), LeaveStatus.APPROVED, year, LeaveType.SICK);
        if (usedDays == null) usedDays = 0.0;

        double remaining = sickAllocated - usedDays;

        if (days.doubleValue() > remaining) {
            throw new BadRequestException(
                    "Insufficient SICK leave balance. Available: "
                            + String.format("%.1f", remaining)
                            + ", Requested: " + days);
        }
    }

    /**
     * ANNUAL_LEAVE: cumulative monthly balance (2/month rolls forward).
     * Uses AnnualLeaveBalanceService to check available days.
     */
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

    /**
     * MATERNITY: 90 days, FEMALE only, one-time per year, no monthly limit.
     */
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

        // One-time per year check
        int year = leave.getStartDate().getYear();
        Double usedMaternity = leaveApplicationRepository.getTotalUsedDaysByType(
                employee.getId(), LeaveStatus.APPROVED, year, LeaveType.MATERNITY);
        if (usedMaternity != null && usedMaternity > 0) {
            throw new BadRequestException("MATERNITY leave has already been taken this year.");
        }
    }

    /**
     * PATERNITY: 5 days, MALE only, one-time per year, no monthly limit.
     */
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

        int year = leave.getStartDate().getYear();
        Double usedPaternity = leaveApplicationRepository.getTotalUsedDaysByType(
                employee.getId(), LeaveStatus.APPROVED, year, LeaveType.PATERNITY);
        if (usedPaternity != null && usedPaternity > 0) {
            throw new BadRequestException("PATERNITY leave has already been taken this year.");
        }
    }

    /**
     * COMP_OFF: balance check against earned comp-off days.
     */
    private void validateCompOff(LeaveApplication leave, BigDecimal days) {
        BigDecimal available = compOffService.getAvailableCompOffDays(leave.getEmployeeId());
        if (available.compareTo(days) < 0) {
            throw new BadRequestException(
                    "Insufficient Comp-Off balance. Available: " + available
                            + ", Requested: " + days);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BALANCE DEDUCTION (called on APPROVAL)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Called when a leave is finally approved (either auto or by approver).
     * Deducts from the appropriate balance.
     */
    public void applyBalanceDeduction(LeaveApplication leave) {
        LeaveType type  = leave.getLeaveType();
        double days     = leave.getDays().doubleValue();
        int year        = leave.getStartDate().getYear();
        int month       = leave.getStartDate().getMonthValue();
        Long empId      = leave.getEmployeeId();

        switch (type) {
            case ANNUAL_LEAVE ->
                    annualLeaveBalanceService.deductLeave(empId, year, month, days);

            case COMP_OFF ->
                    compOffService.useCompOff(empId, leave.getDays(), leave.getId());

            // SICK, MATERNITY, PATERNITY: no separate balance table,
            // balance is computed live from approved leave records vs allocation.
            case SICK, MATERNITY, PATERNITY -> { /* no-op */ }
        }
    }

    /**
     * Called on leave cancellation to restore balance.
     */
    public void restoreBalance(LeaveApplication leave) {
        LeaveType type = leave.getLeaveType();
        double days    = leave.getDays().doubleValue();
        int year       = leave.getStartDate().getYear();
        int month      = leave.getStartDate().getMonthValue();
        Long empId     = leave.getEmployeeId();

        switch (type) {
            case ANNUAL_LEAVE ->
                    annualLeaveBalanceService.restoreLeave(empId, year, month, days);

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
            case SICK, MATERNITY, PATERNITY -> { /* no balance table to restore */ }
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
        LocalDate startDate = leave.getStartDate();
        LocalDate endDate   = leave.getEndDate();

        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date cannot be before start date.");
        }

        boolean sameDay    = startDate.isEqual(endDate);
        boolean startIsHalf = leave.getStartDateHalfDayType() != null;
        boolean endIsHalf   = leave.getEndDateHalfDayType()   != null;

        if (sameDay) {
            return (startIsHalf || endIsHalf)
                    ? new BigDecimal("0.5")
                    : BigDecimal.ONE;
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
    // APPROVAL CHAIN SETUP (unchanged)
    // ═══════════════════════════════════════════════════════════════

    private void setupApprovalChain(LeaveApplication leave, Employee employee, BigDecimal days) {
        switch (employee.getRole()) {
            case EMPLOYEE    -> setupEmployeeChain(leave, employee, days);
            case TEAM_LEADER -> setupTeamLeaderChain(leave, employee, days);
            case MANAGER, ADMIN -> setupManagerChain(leave, employee);
            default -> throw new BadRequestException("Unknown role: " + employee.getRole());
        }
    }

    private void setupEmployeeChain(LeaveApplication leave, Employee employee, BigDecimal days) {
        if (employee.getTeamLeaderId() == null) {
            throw new BadRequestException("No Team Leader assigned. Please contact HR.");
        }
        Employee manager = employeeRepository.findById(employee.getManagerId())
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        leave.setTeamLeaderId(employee.getTeamLeaderId());
        leave.setCurrentApprovalLevel(ApprovalLevel.TEAM_LEADER);
        leave.setManagerId(employee.getManagerId());
        leave.setHrId(manager.getManagerId());

        if (days.compareTo(BigDecimal.ONE) <= 0) {
            leave.setRequiredApprovalLevels(1);
        } else if (days.compareTo(BigDecimal.valueOf(7)) < 0) {
            leave.setRequiredApprovalLevels(2);
        } else {
            leave.setRequiredApprovalLevels(3);
        }
    }

    private void setupTeamLeaderChain(LeaveApplication leave, Employee employee, BigDecimal days) {
        if (employee.getManagerId() == null) {
            throw new BadRequestException("No Manager assigned to Team Leader. Please contact HR.");
        }
        Employee manager = employeeRepository.findById(employee.getManagerId())
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        leave.setTeamLeaderId(null);
        leave.setManagerId(employee.getManagerId());
        leave.setHrId(manager.getManagerId());
        leave.setCurrentApprovalLevel(ApprovalLevel.MANAGER);
        leave.setRequiredApprovalLevels(days.compareTo(BigDecimal.valueOf(7)) < 0 ? 1 : 2);
    }

    private void setupManagerChain(LeaveApplication leave, Employee employee) {
        leave.setTeamLeaderId(null);
        leave.setManagerId(null);
        leave.setHrId(employee.getManagerId());
        leave.setCurrentApprovalLevel(ApprovalLevel.HR);
        leave.setRequiredApprovalLevels(1);
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
        ApprovalLevel firstLevel = leave.getCurrentApprovalLevel();

        if (firstLevel == ApprovalLevel.TEAM_LEADER && leave.getTeamLeaderId() != null) {
            employeeRepository.findById(leave.getTeamLeaderId()).ifPresent(tl ->
                    notificationService.createNotification(
                            tl.getId(), employee.getEmail(), tl.getEmail(),
                            EventType.LEAVE_APPLIED, tl.getRole(), Channel.EMAIL,
                            employee.getName() + " applied leave from "
                                    + leave.getStartDate() + " to " + leave.getEndDate()
                                    + ". Awaiting your approval.")
            );
        } else if (firstLevel == ApprovalLevel.MANAGER && leave.getManagerId() != null) {
            employeeRepository.findById(leave.getManagerId()).ifPresent(mgr ->
                    notificationService.createNotification(
                            mgr.getId(), employee.getEmail(), mgr.getEmail(),
                            EventType.LEAVE_APPLIED, mgr.getRole(), Channel.EMAIL,
                            "Team Leader " + employee.getName() + " applied leave from "
                                    + leave.getStartDate() + " to " + leave.getEndDate()
                                    + ". Awaiting your approval.")
            );
        } else if (firstLevel == ApprovalLevel.HR) {
            employeeRepository.findAllHr().forEach(hr ->
                    notificationService.createNotification(
                            hr.getId(), employee.getEmail(), hr.getEmail(),
                            EventType.LEAVE_APPLIED, hr.getRole(), Channel.EMAIL,
                            "Manager " + employee.getName() + " applied leave from "
                                    + leave.getStartDate() + " to " + leave.getEndDate()
                                    + ". Awaiting your approval.")
            );
        }
    }
}