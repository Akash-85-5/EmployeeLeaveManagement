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

    private final String SERVER_IP = "192.168.1.62";
    private final String SERVER_PORT = "8080";


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


    @Transactional
    public LeaveResponse applyLeave(LeaveApplication leave, boolean confirmLossOfPay)
    {
        checkLeaveOverlap(leave);
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException(  "End date cannot be before start date");
        }

        // 2️⃣ Calculate leave days (holiday + half-day aware)
        BigDecimal calculatedDays = calculateLeaveDuration(leave);

        // 3️⃣ Check balance (Comp-Off / LOP logic)
        String warning = checkBalanceAndGetWarning(leave, calculatedDays);

        if (warning != null && !confirmLossOfPay) {
            return new LeaveResponse(null, warning);
        }

        // 4️⃣ Set calculated fields
        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.PENDING);

        LeaveApplication savedLeave =
                leaveApplicationRepository.save(leave);

        if (leave.getLeaveType() == LeaveType.COMP_OFF && warning == null) {
            compOffService.useCompOff(
                    leave.getEmployeeId(),
                    calculatedDays,
                    savedLeave.getId()
            );
        }
        notifyManager(savedLeave);
        return new LeaveResponse(savedLeave, null);
    }


    private void notifyManager(LeaveApplication leave) {

        Employee employee = employeeRepository.findById(leave.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Employee manager = employeeRepository.findById(employee.getManagerId())
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        notificationService.createNotification(
                manager.getId(),
                manager.getEmail(),
                EventType.LEAVE_APPLIED,
                manager.getRole(),
                Channel.EMAIL,
                "Employee " + employee.getName()
                        + " applied leave from "
                        + leave.getStartDate()
                        + " to "
                        + leave.getEndDate()
        );
    }

    private void checkLeaveOverlap(LeaveApplication leave) {

        List<LeaveApplication> overlaps =
                leaveApplicationRepository.findOverlappingLeaves(
                        leave.getEmployeeId().longValue(),
                        leave.getStartDate(),
                        leave.getEndDate()
                );

        if (!overlaps.isEmpty()) {
            throw new BadRequestException("Leave dates overlap with an existing leave");
        }
    }





    public List<LeaveApplication> getLeavesByEmployee(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeId(employeeId);
    }


    @Transactional
    public LeaveResponse applyAdminLeave(LeaveApplication leave, boolean isConfirmed) {
        validateDates(leave);
        BigDecimal calculatedDays = calculateLeaveDuration(leave);
        String warning = checkBalanceAndGetWarning(leave, calculatedDays);

        if (warning != null && !isConfirmed) {
            return new LeaveResponse(null, warning);
        }

        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.APPROVED);
        processAttachments(leave);

        LeaveApplication savedLeave = leaveApplicationRepository.save(leave);

        if (leave.getLeaveType() == LeaveType.COMP_OFF && warning == null) {
            compOffService.useCompOff(leave.getEmployeeId().longValue(), calculatedDays, savedLeave.getId());
        }

        return new LeaveResponse(savedLeave, null);
    }

    // --- 🛠️ CANCELLATION LOGIC ---

    @Transactional
    public void cancelAdminLeave(Long applicationId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + applicationId));
        performCancellation(leave);
    }

    @Transactional
    public void cancelEmployeeLeave(Long applicationId, Long employeeId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + applicationId));

        if (leave.getEmployeeId().longValue() != employeeId) {
            throw new BadRequestException(
                    "Unauthorized: You cannot cancel another employee's leave."
            );
        }



        if (leave.getStatus() == LeaveStatus.REJECTED || leave.getStatus() == LeaveStatus.CANCELLED) {
            throw new BadRequestException("Leave is already finalized as " + leave.getStatus());
        }
        performCancellation(leave);
    }

    private void performCancellation(LeaveApplication leave) {
        // 🔄 REVERSAL: Restore Comp-Off credits if they were deducted for APPROVED or PENDING leaves
        if (leave.getLeaveType() == LeaveType.COMP_OFF &&
                (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.PENDING)) {

            List<CompOff> linkedCredits = compOffRepository.findByUsedLeaveApplicationId(leave.getId());

            BigDecimal restoredDays = BigDecimal.ZERO;

            for (CompOff credit : linkedCredits) {
                credit.setStatus(CompOffStatus.EARNED);
                credit.setUsedLeaveApplicationId(null);
                compOffRepository.save(credit);
            }
            if (restoredDays.compareTo(BigDecimal.ZERO) > 0) {
                compOffService.restoreCompOffBalance(
                        leave.getEmployeeId().longValue(),
                        restoredDays
                );
            }
        }


        leave.setStatus(LeaveStatus.CANCELLED);
        leaveApplicationRepository.save(leave);
    }

    // --- 🛠️ HELPERS ---

    private String checkBalanceAndGetWarning(LeaveApplication leave, BigDecimal calculatedDays) {
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            BigDecimal available = compOffService.getAvailableCompOffDays(leave.getEmployeeId().longValue());
            if (available.compareTo(calculatedDays) < 0) {
                return "Insufficient leave balance (Available: " + available + "). The request will use carry-forwarded leave from the previous year or proceed as Loss of Pay.";
            }
        }
        return null;
    }
    private void validateDates(LeaveApplication leave) {
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
    }

    private void processAttachments(LeaveApplication leave) {
        if (leave.getAttachments() != null) {
            leave.getAttachments().forEach(attachment -> {
                attachment.setFileUrl("http://" + SERVER_IP + ":" + SERVER_PORT + "/uploads/leaves/" + attachment.getFileUrl());
                attachment.setLeaveApplication(leave);
            });
        }
    }

    public BigDecimal calculateLeaveDuration(LeaveApplication leave) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = leave.getStartDate();
        while (!date.isAfter(leave.getEndDate())) {
            if (!holidayChecker.isNonWorkingDay(date)) {
                BigDecimal inc = (leave.getLeaveType() == LeaveType.HALF_DAY || (leave.getHalfDayType() != null && date.equals(leave.getEndDate())))
                        ? new BigDecimal("0.5") : BigDecimal.ONE;
                total = total.add(inc);
            }
            date = date.plusDays(1);
        }
        if (total.compareTo(BigDecimal.ZERO) == 0) throw new BadRequestException("Selected dates are non-working days.");
        return total;
    }

}


