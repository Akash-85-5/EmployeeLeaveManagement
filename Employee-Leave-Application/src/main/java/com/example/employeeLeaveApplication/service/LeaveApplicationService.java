package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.HolidayChecker;
import com.example.employeeLeaveApplication.dto.LeaveResponse;
import com.example.employeeLeaveApplication.entity.CompOff;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.CompOffRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.repository.LeaveAttachmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final LeaveAttachmentRepository leaveAttachmentRepository;

    private final String SERVER_IP = "192.168.1.62";
    private final String SERVER_PORT = "8080";

    public LeaveApplicationService(
            LeaveApplicationRepository leaveApplicationRepository,
            NotificationService notificationService,
            EmployeeRepository employeeRepository,
            HolidayChecker holidayChecker,
            CompOffService compOffService,
            CompOffRepository compOffRepository,
            LeaveAttachmentRepository leaveAttachmentRepository
    ) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.notificationService = notificationService;
        this.employeeRepository = employeeRepository;
        this.holidayChecker = holidayChecker;
        this.compOffService = compOffService;
        this.compOffRepository = compOffRepository;
        this.leaveAttachmentRepository = leaveAttachmentRepository;
    }

    // ==================== EXISTING METHODS ====================

    @Transactional
    public LeaveResponse applyLeave(LeaveApplication leave, boolean confirmLossOfPay) {
        checkLeaveOverlap(leave);
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        BigDecimal calculatedDays = calculateLeaveDuration(leave);
        String warning = checkBalanceAndGetWarning(leave, calculatedDays);

        if (warning != null && !confirmLossOfPay) {
            return new LeaveResponse(null, warning);
        }

        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.PENDING);

        LeaveApplication savedLeave = leaveApplicationRepository.save(leave);

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

    public List<LeaveApplication> getLeavesByEmployee(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public void cancelEmployeeLeave(Long applicationId, Long employeeId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + applicationId));

        if (leave.getEmployeeId().longValue() != employeeId) {
            throw new BadRequestException("Unauthorized: You cannot cancel another employee's leave.");
        }

        if (leave.getStatus() == LeaveStatus.REJECTED || leave.getStatus() == LeaveStatus.CANCELLED) {
            throw new BadRequestException("Leave is already finalized as " + leave.getStatus());
        }
        performCancellation(leave);
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

    @Transactional
    public void cancelAdminLeave(Long applicationId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + applicationId));
        performCancellation(leave);
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

    // ==================== NEW METHODS FOR UPDATED CONTROLLER ====================

    /**
     * Get all leaves with filters and pagination (Admin view)
     */
    public Page<LeaveApplication> getAllLeaves(
            Long employeeId,
            LeaveStatus status,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            Integer year,
            Pageable pageable
    ) {
        return leaveApplicationRepository.findAll(
                createSpecification(employeeId, status, leaveType, startDate, endDate, year),
                pageable
        );
    }

    /**
     * Get single leave by ID
     */
    public LeaveApplication getLeaveById(Long id) {
        return leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + id));
    }

    /**
     * Get employee leaves with filters and pagination
     */
    public Page<LeaveApplication> getLeavesByEmployee(
            Long employeeId,
            LeaveStatus status,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            Integer year,
            Pageable pageable
    ) {
        return leaveApplicationRepository.findAll(
                createSpecification(employeeId, status, leaveType, startDate, endDate, year),
                pageable
        );
    }

    /**
     * Update leave before approval
     */
    @Transactional
    public LeaveResponse updateLeave(
            Long id,
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            String halfDayType
    ) {
        LeaveApplication leave = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + id));

        // Verify ownership
        if (!leave.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Unauthorized: You can only update your own leaves");
        }

        // Can only update pending leaves
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Can only update PENDING leaves. Current status: " + leave.getStatus());
        }

        // Update fields if provided
        if (startDate != null) {
            leave.setStartDate(startDate);
        }
        if (endDate != null) {
            leave.setEndDate(endDate);
        }
        if (reason != null && !reason.isEmpty()) {
            leave.setReason(reason);
        }
        if (halfDayType != null && !halfDayType.isEmpty()) {
            leave.setHalfDayType(HalfDayType.valueOf(halfDayType.toUpperCase()));
        }

        // Recalculate days
        validateDates(leave);
        BigDecimal calculatedDays = calculateLeaveDuration(leave);
        leave.setDays(calculatedDays);

        LeaveApplication savedLeave = leaveApplicationRepository.save(leave);
        return new LeaveResponse(savedLeave, null);
    }

    /**
     * Delete attachment
     */
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        LeaveAttachment attachment = leaveAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BadRequestException("Attachment not found with ID: " + attachmentId));

        leaveAttachmentRepository.delete(attachment);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create JPA Specification for filtering
     */
    private Specification<LeaveApplication> createSpecification(
            Long employeeId,
            LeaveStatus status,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            Integer year
    ) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employeeId"), employeeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (leaveType != null) {
                predicates.add(cb.equal(root.get("leaveType"), leaveType));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), endDate));
            }
            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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
        List<LeaveApplication> overlaps = leaveApplicationRepository.findOverlappingLeaves(
                leave.getEmployeeId().longValue(),
                leave.getStartDate(),
                leave.getEndDate()
        );

        if (!overlaps.isEmpty()) {
            throw new BadRequestException("Leave dates overlap with an existing leave");
        }
    }

    private void performCancellation(LeaveApplication leave) {
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
                compOffService.restoreCompOffBalance(leave.getEmployeeId().longValue(), restoredDays);
            }
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leaveApplicationRepository.save(leave);
    }

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
}