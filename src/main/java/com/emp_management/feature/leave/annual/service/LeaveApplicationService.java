package com.emp_management.feature.leave.annual.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.entity.EmployeePersonalDetails;
import com.emp_management.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.holiday.utils.HolidayChecker;
import com.emp_management.feature.leave.annual.dto.LeaveApplicationResponseDTO;
import com.emp_management.feature.leave.annual.dto.LeaveResponse;
import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.mapper.LeaveApplicationMapper;
import com.emp_management.feature.leave.annual.repository.LeaveApplicationRepository;
import com.emp_management.feature.leave.annual.repository.LeaveAttachmentRepository;
import com.emp_management.feature.leave.annual.repository.LeaveTypeRepository;
import com.emp_management.feature.leave.carryforward.service.CarryForwardBalanceService;
import com.emp_management.feature.leave.compoff.entity.CompOff;
import com.emp_management.feature.leave.compoff.repository.CompOffRepository;
import com.emp_management.feature.leave.compoff.service.CompOffService;
import com.emp_management.feature.notification.service.NotificationService;
import com.emp_management.shared.enums.*;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository           leaveTypeRepository;
    private final HolidayChecker holidayChecker;
    private final CompOffService compOffService;
    private final CompOffRepository compOffRepository;
    private final LeaveAttachmentRepository leaveAttachmentRepository;
    private final AnnualLeaveBalanceService    annualLeaveBalanceService;
    private final SickLeaveBalanceService      sickLeaveBalanceService;
    private final CarryForwardBalanceService carryForwardBalanceService;// ✅ NEW FIELD
    private final EmployeePersonalDetailsRepository personalDetailsRepository;
//    private final SeparationService            separationService;

    public LeaveApplicationService(
            LeaveApplicationRepository leaveApplicationRepository,
            NotificationService notificationService,
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository,
            HolidayChecker holidayChecker,
            CompOffService compOffService,
            CompOffRepository compOffRepository,
            LeaveAttachmentRepository leaveAttachmentRepository,
            AnnualLeaveBalanceService annualLeaveBalanceService,
            SickLeaveBalanceService sickLeaveBalanceService,
            CarryForwardBalanceService carryForwardBalanceService,
            EmployeePersonalDetailsRepository personalDetailsRepository){
//            SeparationService separationService) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.notificationService         = notificationService;
        this.employeeRepository          = employeeRepository;
        this.leaveTypeRepository         = leaveTypeRepository;
        this.holidayChecker              = holidayChecker;
        this.compOffService              = compOffService;
        this.compOffRepository           = compOffRepository;
        this.leaveAttachmentRepository   = leaveAttachmentRepository;
        this.annualLeaveBalanceService   = annualLeaveBalanceService;
        this.sickLeaveBalanceService     = sickLeaveBalanceService;
        this.carryForwardBalanceService  = carryForwardBalanceService; // ✅ NEW ASSIGNMENT
        this.personalDetailsRepository   = personalDetailsRepository;
//        this.separationService           = separationService;
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

        Employee employee = leave.getEmployee();

        validateLeaveTypeAndBalance(leave, employee, calculatedDays);
        setupApprovalChain(leave, employee);

        // No manager → auto-approve
        if (leave.getRequiredApprovalLevels() == 0) {
            leave.setStatus(RequestStatus.APPROVED);
            leave.setApprovedBy(employee.getEmpId());
            leave.setApprovedRole(employee.getRole().getRoleName());
            leave.setApprovedAt(LocalDateTime.now());
            LeaveApplication saved = leaveApplicationRepository.save(leave);
            applyBalanceDeduction(saved);
            return new LeaveResponse(saved, null);
        }

        leave.setStatus(RequestStatus.PENDING);
        LeaveApplication saved = leaveApplicationRepository.save(leave);
        notifyFirstApprover(saved, employee);
        return new LeaveResponse(saved, null);
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVAL CHAIN SETUP
    // Uses Employee.reportingId (String empId of manager)
    // ═══════════════════════════════════════════════════════════════

    private void setupApprovalChain(LeaveApplication leave, Employee employee) {
        // reportingId is stored as Long in your entity — fetch manager by their numeric id
        String firstApproverNumericId = employee.getReportingId();

        if (firstApproverNumericId == null) {
            leave.setFirstApproverId(null);
            leave.setSecondApproverId(null);
            leave.setCurrentApproverId(null);
            leave.setCurrentApprovalLevel(null);
            leave.setRequiredApprovalLevels(0);
            return;
        }

        Employee firstApprover = employeeRepository.findByEmpId(firstApproverNumericId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "First approver not found: " + firstApproverNumericId));

        leave.setFirstApproverId(firstApprover.getEmpId());
        leave.setCurrentApproverId(firstApprover.getEmpId());
        leave.setCurrentApprovalLevel(ApprovalLevel.FIRST_APPROVER);

        String secondApproverNumericId = firstApprover.getReportingId();

        if (secondApproverNumericId == null) {
            leave.setSecondApproverId(null);
            leave.setRequiredApprovalLevels(1);
        } else {
            Employee secondApprover = employeeRepository.findByEmpId(secondApproverNumericId)
                    .orElseThrow(() -> new BadRequestException(
                            "Second approver not found: " + secondApproverNumericId));
            leave.setSecondApproverId(secondApprover.getEmpId());
            leave.setRequiredApprovalLevels(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATE LEAVE TYPE AND BALANCE
    // Driven by leaveType.leaveType (String name) — no enum switch
    // ═══════════════════════════════════════════════════════════════

    private void validateLeaveTypeAndBalance(LeaveApplication leave,
                                             Employee employee,
                                             BigDecimal days) {
        String typeName = leave.getLeaveType().getLeaveType().toUpperCase();
        int year  = leave.getStartDate().getYear();
        int month = leave.getStartDate().getMonthValue();

        switch (typeName) {
            case "SICK"           -> validateSickLeave(leave, days, year, month);
            case "ANNUAL"   -> validateAnnualLeave(leave, days, year, month);
            case "CARRY_FORWARD"  -> validateCarryForward(leave, days, year); // ✅ NEW CASE
            case "MATERNITY"      -> validateMaternity(leave, employee, days);
            case "PATERNITY"      -> validatePaternity(leave, employee, days);
            case "COMP_OFF"       -> validateCompOff(leave, days);
            default -> {
                // Generic validation for any future leave types:
                // Just check the total allocated days from the entity
                Double allocatedDays = leave.getLeaveType().getAllocatedDays();
                Double used = leaveApplicationRepository.getTotalUsedDaysByType(
                        employee.getEmpId(), RequestStatus.APPROVED, year,
                        leave.getLeaveType().getLeaveType());
                double available = allocatedDays - (used != null ? used : 0.0);
                if (days.doubleValue() > available) {
                    throw new BadRequestException(
                            "Insufficient " + typeName + " balance. Available: "
                                    + available + ", Requested: " + days);
                }
            }
        }
    }

    private void validateSickLeave(LeaveApplication leave, BigDecimal days, int year, int month) {
        double available = sickLeaveBalanceService
                .getAvailableForMonth(leave.getEmployeeId(), year, month);
        if (days.doubleValue() > available) {
            throw new BadRequestException(
                    "Insufficient SICK leave balance for "
                            + java.time.Month.of(month).name() + " " + year
                            + ". Available: " + String.format("%.1f", available)
                            + ", Requested: " + days);
        }
    }

    private void validateAnnualLeave(LeaveApplication leave, BigDecimal days, int year, int month) {
        double available = annualLeaveBalanceService
                .getAvailableForMonth(leave.getEmployeeId(), year, month);
        if (days.doubleValue() > available) {
            throw new BadRequestException(
                    "Insufficient ANNUAL_LEAVE balance for "
                            + java.time.Month.of(month).name() + " " + year
                            + ". Available: " + String.format("%.1f", available)
                            + ", Requested: " + days);
        }
    }

    /**
     * ✅ NEW — Validates carry-forward balance before allowing application.
     *
     * Reads CarryForwardBalance.remaining for the employee in the leave year.
     * If no balance record exists → employee has 0 carry-forward → reject.
     * Balance is a yearly bucket (not monthly), so we only check by year.
     */
    private void validateCarryForward(LeaveApplication leave, BigDecimal days, int year) {
        double available = carryForwardBalanceService
                .getAvailableBalance(leave.getEmployee().getEmpId(), year);
        if (days.doubleValue() > available) {
            throw new BadRequestException(
                    "Insufficient CARRY_FORWARD balance for year " + year
                            + ". Available: " + String.format("%.1f", available)
                            + ", Requested: " + days);
        }
    }

    private void validateMaternity(LeaveApplication leave, Employee employee, BigDecimal days) {
        EmployeePersonalDetails details = personalDetailsRepository
                .findByEmployee_EmpId(employee.getEmpId()).orElse(null);
        if (details == null || details.getGender() != Gender.FEMALE) {
            throw new BadRequestException("MATERNITY leave is only available for female employees.");
        }
        double maxDays = leave.getLeaveType().getAllocatedDays();
        if (days.doubleValue() > maxDays) {
            throw new BadRequestException(
                    "MATERNITY leave cannot exceed " + maxDays + " days.");
        }
        Double used = leaveApplicationRepository.getTotalUsedDaysByType(
                employee.getEmpId(), RequestStatus.APPROVED,
                leave.getStartDate().getYear(), leave.getLeaveType().getLeaveType());
        if (used != null && used > 0) {
            throw new BadRequestException("MATERNITY leave has already been taken this year.");
        }
    }

    private void validatePaternity(LeaveApplication leave, Employee employee, BigDecimal days) {
        EmployeePersonalDetails details = personalDetailsRepository
                .findByEmployee_EmpId(employee.getEmpId()).orElse(null);
        if (details == null || details.getGender() != Gender.MALE) {
            throw new BadRequestException("PATERNITY leave is only available for male employees.");
        }
        double maxDays = leave.getLeaveType().getAllocatedDays();
        if (days.doubleValue() > maxDays) {
            throw new BadRequestException(
                    "PATERNITY leave cannot exceed " + maxDays + " days.");
        }
        Double used = leaveApplicationRepository.getTotalUsedDaysByType(
                employee.getEmpId(), RequestStatus.APPROVED,
                leave.getStartDate().getYear(), leave.getLeaveType().getLeaveType());
        if (used != null && used > 0) {
            throw new BadRequestException("PATERNITY leave has already been taken this year.");
        }
    }

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
        double days  = leave.getDays().doubleValue();
        int year     = leave.getStartDate().getYear();
        int month    = leave.getStartDate().getMonthValue();
        String empId = leave.getEmployeeId();
        String type  = leave.getLeaveType().getLeaveType().toUpperCase();

        switch (type) {
            case "ANNUAL"  -> annualLeaveBalanceService.deductLeave(empId, year, month, days);
            case "SICK"          -> sickLeaveBalanceService.deductLeave(empId, year, month, days);
            case "CARRY_FORWARD" -> carryForwardBalanceService.deductLeave(empId, year, days); // ✅ NEW
            case "COMP_OFF"      -> compOffService.useCompOff(empId, leave.getDays(), leave.getId());
            default              -> { /* MATERNITY/PATERNITY — no balance table */ }
        }

//        if (separationService.isInNoticePeriod(empId)) {

//            separationService.extendNoticePeriod(empId, (int) Math.ceil(days));
//        }
    }

    public void restoreBalance(LeaveApplication leave) {
        double days  = leave.getDays().doubleValue();
        int year     = leave.getStartDate().getYear();
        int month    = leave.getStartDate().getMonthValue();
        String empId = leave.getEmployeeId();
        String type  = leave.getLeaveType().getLeaveType().toUpperCase();

        switch (type) {
            case "ANNUAL"  -> annualLeaveBalanceService.restoreLeave(empId, year, month, days);
            case "SICK"          -> sickLeaveBalanceService.restoreLeave(empId, year, month, days);
            case "CARRY_FORWARD" -> carryForwardBalanceService.restoreLeave(empId, year, days); // ✅ NEW
            case "COMP_OFF"      -> {
                List<CompOff> linked = compOffRepository.findByUsedLeaveApplicationId(leave.getId());
                BigDecimal restored = BigDecimal.ZERO;
                for (CompOff c : linked) {
                    c.setStatus(RequestStatus.EARNED);
                    c.setUsedLeaveApplicationId(null);
                    compOffRepository.save(c);
                    restored = restored.add(c.getDays());
                }
                if (restored.compareTo(BigDecimal.ZERO) > 0) {
                    compOffService.restoreCompOffBalance(empId, restored);
                }
            }
            default -> { /* MATERNITY/PATERNITY — no balance table */ }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL LEAVE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void cancelEmployeeLeave(Long applicationId, String employeeId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException(
                        "Leave application not found with ID: " + applicationId));

        if (!leave.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Unauthorized: You cannot cancel another employee's leave.");
        }
        if (leave.getStatus() == RequestStatus.REJECTED
                || leave.getStatus() == RequestStatus.CANCELLED) {
            throw new BadRequestException("Leave is already finalized as " + leave.getStatus());
        }
        if (leave.getStatus() == RequestStatus.APPROVED) {
            restoreBalance(leave);
        }
        leave.setStatus(RequestStatus.CANCELLED);
        leaveApplicationRepository.save(leave);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE LEAVE (before approval)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LeaveResponse updateLeave(Long id, String employeeId,
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
        if (leave.getStatus() != RequestStatus.PENDING) {
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

        leave.setDays(calculateLeaveDuration(leave));
        return new LeaveResponse(leaveApplicationRepository.save(leave), null);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════

    public LeaveApplication getLeaveById(Long id) {
        return leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(
                        "Leave application not found with ID: " + id));
    }

    public List<LeaveApplicationResponseDTO> getLeavesByEmployee(String employeeId) {
        return leaveApplicationRepository
                .findByEmployee_EmpId(employeeId)
                .stream()
                .map(LeaveApplicationMapper::toDTO)
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // LEAVE DURATION (logic unchanged)
    // ═══════════════════════════════════════════════════════════════

    public BigDecimal calculateLeaveDuration(LeaveApplication leave) {
        LocalDate startDate = leave.getStartDate();
        LocalDate endDate   = leave.getEndDate();

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
                    "No working days found in the selected date range.");
        }

        BigDecimal days = BigDecimal.valueOf(workingDays);
        if (startIsHalf && !holidayChecker.isNonWorkingDay(startDate))
            days = days.subtract(new BigDecimal("0.5"));
        if (endIsHalf && !holidayChecker.isNonWorkingDay(endDate))
            days = days.subtract(new BigDecimal("0.5"));

        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Calculated leave duration is zero or negative. Check dates and half-day selections.");
        }
        return days;
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void checkHolidaysInRange(LeaveApplication leave) {
        LocalDate start = leave.getStartDate();
        LocalDate end   = leave.getEndDate();
        if (start.isEqual(end)) {
            String reason = holidayChecker.getNonWorkingDayReason(start);
            if (reason != null)
                throw new BadRequestException(
                        "Cannot apply leave on " + start + " — non-working day: " + reason);
            return;
        }
        if (holidayChecker.countWorkingDays(start, end) == 0) {
            throw new BadRequestException(
                    "The date range (" + start + " to " + end + ") contains no working days.");
        }
    }

    private void checkLeaveOverlap(LeaveApplication leave) {
        List<LeaveApplication> overlaps = leaveApplicationRepository.findOverlappingLeaves(
                leave.getEmployeeId(), leave.getStartDate(), leave.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new BadRequestException("Leave dates overlap with an existing leave.");
        }
    }

    private void notifyFirstApprover(LeaveApplication leave, Employee employee) {
        if (leave.getFirstApproverId() == null) return;

        Employee approver = employeeRepository.findByEmpId(leave.getFirstApproverId())
                        .orElseThrow(()-> new EntityNotFoundException("Approver not found"));
    notificationService.createNotification(
                        approver.getEmpId(), employee.getEmail(), approver.getEmail(),
                        EventType.LEAVE_APPLIED,  Channel.EMAIL,
                        employee.getName() + " applied for "
                                + leave.getLeaveType().getLeaveType() + " leave from "
                                + leave.getStartDate() + " to " + leave.getEndDate()
                                + ". Awaiting your approval.");
    }

}
