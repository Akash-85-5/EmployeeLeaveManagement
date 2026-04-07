package com.emp_management.feature.od.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.notification.service.NotificationService;
import com.emp_management.feature.od.dto.ODDecisionRequest;
import com.emp_management.feature.od.entity.ODApproval;
import com.emp_management.feature.od.entity.ODRequest;
import com.emp_management.feature.od.repository.ODApprovalRepository;
import com.emp_management.feature.od.repository.ODRequestRepository;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.Channel;
import com.emp_management.shared.enums.EventType;
import com.emp_management.shared.enums.RequestStatus;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ODService {

    private final ODRequestRepository  odRepository;
    private final ODApprovalRepository odApprovalRepository;
    private final EmployeeRepository   employeeRepository;
    private final NotificationService  notificationService;

    public ODService(ODRequestRepository odRepository,
                     ODApprovalRepository odApprovalRepository,
                     EmployeeRepository employeeRepository,
                     NotificationService notificationService) {
        this.odRepository      = odRepository;
        this.odApprovalRepository = odApprovalRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // CREATE OD
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public ODRequest createOD(String employeeId, ODRequest request) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employeeId));

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date.");
        }

        // OD cannot be applied for future dates beyond reasonable lead time
        // (unlike sick leave, OD is pre-planned work — allow future dates freely)
        // Past-date OD: allowed (employee was already on duty — retroactive request)

        // Overlap check
        List<ODRequest> overlaps = odRepository.findOverlappingODs(
                employeeId, request.getStartDate(), request.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new BadRequestException(
                    "You already have an active OD request overlapping these dates.");
        }

        // Calculate working days (simple inclusive count; swap for HolidayChecker if available)
        BigDecimal days = calculateODDuration(request.getStartDate(), request.getEndDate());
        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No working days found in the selected date range.");
        }

        request.setEmployee(employee);
        request.setDays(days);
        request.setStatus(RequestStatus.PENDING);

        // Build approval chain (same logic as LeaveApplicationService.setupApprovalChain)
        setupApprovalChain(request, employee);


        ODRequest saved = odRepository.save(request);
        notifyFirstApprover(saved, employee);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVAL CHAIN SETUP
    // Mirrors LeaveApplicationService.setupApprovalChain exactly
    // ═══════════════════════════════════════════════════════════════

    private void setupApprovalChain(ODRequest od, Employee employee) {
        String firstApproverEmpId = employee.getReportingId();

        if (firstApproverEmpId == null) {
            od.setFirstApproverId(null);
            od.setSecondApproverId(null);
            od.setCurrentApprovalLevel(null);
            od.setRequiredApprovalLevels(0);
            return;
        }

        Employee firstApprover = employeeRepository.findByEmpId(firstApproverEmpId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "First approver not found: " + firstApproverEmpId));

        od.setFirstApproverId(firstApprover.getEmpId());
        od.setCurrentApproverId(firstApprover.getEmpId());
        od.setCurrentApprovalLevel(ApprovalLevel.FIRST_APPROVER);

        String secondApproverEmpId = firstApprover.getReportingId();

        if (secondApproverEmpId == null) {
            od.setSecondApproverId(null);
            od.setRequiredApprovalLevels(1);
        } else {
            Employee secondApprover = employeeRepository.findByEmpId(secondApproverEmpId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Second approver not found: " + secondApproverEmpId));
            od.setSecondApproverId(secondApprover.getEmpId());
            od.setRequiredApprovalLevels(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DECIDE (approve or reject) — mirrors LeaveApprovalService.decideLeave
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void decideOD(ODDecisionRequest request) {
        validateDecision(request.getDecision());

        if (request.getComments() == null || request.getComments().trim().isEmpty()) {
            throw new BadRequestException("Remarks are required for approval or rejection.");
        }

        ODRequest od = odRepository.findById(request.getOdId())
                .orElseThrow(() -> new EntityNotFoundException("OD not found"));

        if (od.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "OD is already processed with status: " + od.getStatus());
        }

        Employee approver = employeeRepository.findByEmpId(request.getApproverId())
                .orElseThrow(() -> new EntityNotFoundException("Approver not found"));

        ApprovalLevel currentLevel = od.getCurrentApprovalLevel();
        validateApproverForLevel(od, approver, currentLevel);
        recordLevelDecision(od, currentLevel, request.getDecision());

        // ────────────── APPEND PREVIOUS REMARKS FOR LEVEL 1 ──────────────
        String previousLevel1Remarks = "";
        if (currentLevel == ApprovalLevel.SECOND_APPROVER) {
            List<ODApproval> previousApprovals =
                    odApprovalRepository.findByOdIdAndApprovalLevel(od.getId(), ApprovalLevel.FIRST_APPROVER);
            if (!previousApprovals.isEmpty()) {
                // Concatenate all level-1 remarks
                StringBuilder sb = new StringBuilder();
                for (ODApproval a : previousApprovals) {
                    sb.append(a.getApproverRole()).append(": ").append(a.getComments()).append(" | ");
                }
                previousLevel1Remarks = sb.toString();
            }
        }

        // Append current approver's remark
        String existing = od.getRemarks() != null ? od.getRemarks() + " | " : "";
        od.setRemarks(existing + previousLevel1Remarks + approver.getRole().getRoleName()
                + ": " + request.getComments());

        // Save individual ODApproval record (visible to next manager)
        saveApprovalRecord(
                od.getId(),
                approver,
                currentLevel,
                request.getDecision(),
                request.getComments()
        );

        if (request.getDecision() == RequestStatus.REJECTED) {
            finalizeOD(od, RequestStatus.REJECTED, approver, request.getComments());
        } else {
            advanceOrFinalize(od, approver, request.getComments());
        }
    }

    @Transactional
    public void approveOD(Long odId, String approverId, String comments) {
        ODDecisionRequest req = new ODDecisionRequest();
        req.setOdId(odId);
        req.setApproverId(approverId);
        req.setDecision(RequestStatus.APPROVED);
        req.setComments(comments);
        decideOD(req);
    }

    @Transactional
    public void rejectOD(Long odId, String approverId, String comments) {
        ODDecisionRequest req = new ODDecisionRequest();
        req.setOdId(odId);
        req.setApproverId(approverId);
        req.setDecision(RequestStatus.REJECTED);
        req.setComments(comments);
        decideOD(req);
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL OD
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void cancelOD(Long odId, String requesterId) {
        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new EntityNotFoundException("OD not found"));

        if (od.getStatus() == RequestStatus.REJECTED
                || od.getStatus() == RequestStatus.CANCELLED) {
            throw new BadRequestException("OD is already finalized as " + od.getStatus());
        }

        // Approved OD: only the employee themselves or HR can cancel
        if (od.getStatus() == RequestStatus.APPROVED) {
            boolean isEmployee = od.getEmployeeId().equals(requesterId);
            Employee requester = employeeRepository.findByEmpId(requesterId)
                    .orElseThrow(() -> new EntityNotFoundException("Requester not found"));
            boolean isHr = "HR".equalsIgnoreCase(
                    requester.getRole() != null ? requester.getRole().getRoleName() : "");
            if (!isEmployee && !isHr) {
                throw new BadRequestException(
                        "Only the employee or HR can cancel an approved OD.");
            }
        } else {
            // PENDING: employee cancels own, or current approver cancels it
            boolean isOwner           = od.getEmployeeId().equals(requesterId);
            boolean isCurrentApprover = requesterId.equals(od.getCurrentApproverId());
            if (!isOwner && !isCurrentApprover) {
                throw new BadRequestException("You are not allowed to cancel this OD request.");
            }
        }

        od.setStatus(RequestStatus.CANCELLED);
        od.setCurrentApproverId(null);
        odRepository.save(od);

        // Notify employee only if someone else cancelled it
        if (!requesterId.equals(od.getEmployeeId())) {
            employeeRepository.findByEmpId(requesterId).ifPresent(canceller ->
                    notify(od.getEmployeeId(),
                            canceller.getEmail(),
                            EventType.OD_CANCELLED,
                            "Your OD request from " + od.getStartDate() + " to "
                                    + od.getEndDate() + " has been cancelled by "
                                    + canceller.getName() + "."));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCALATION — called by ODReminderSchedulerService
    // Mirrors LeaveReminderSchedulerService.escalate logic exactly
    // ═══════════════════════════════════════════════════════════════

    /**
     * Called by the scheduler when an approver has exceeded MAX_REMINDERS
     * without acting. Escalates to the next approver or auto-rejects.
     */
    @Transactional
    public void escalate(ODRequest od) {
        ApprovalLevel currentLevel = od.getCurrentApprovalLevel();

        if (currentLevel == ApprovalLevel.FIRST_APPROVER) {
            escalateFromFirstToSecond(od);
        } else {
            escalateUpTheChain(od);
        }
    }

    private void escalateFromFirstToSecond(ODRequest od) {
        String secondApproverId = od.getSecondApproverId();

        if (secondApproverId == null) {
            autoReject(od);
            return;
        }

        od.setCurrentApproverId(secondApproverId);
        od.setCurrentApprovalLevel(ApprovalLevel.SECOND_APPROVER);
        od.setEscalated(true);
        od.setEscalatedAt(LocalDateTime.now());
        odRepository.save(od);

        notifyEscalatedApprover(od, secondApproverId,
                "This OD was escalated to you because the previous approver did not respond.");
    }

    private void escalateUpTheChain(ODRequest od) {
        String currentApproverId = od.getCurrentApproverId();
        Employee currentApprover = employeeRepository.findByEmpId(currentApproverId).orElse(null);

        if (currentApprover == null) {
            autoReject(od);
            return;
        }

        String nextApproverId = currentApprover.getReportingId();
        if (nextApproverId == null) {
            autoReject(od);
            return;
        }

        od.setCurrentApproverId(nextApproverId);
        od.setEscalated(true);
        od.setEscalatedAt(LocalDateTime.now());
        odRepository.save(od);

        notifyEscalatedApprover(od, nextApproverId,
                "This OD has been escalated to you as the previous approver did not respond.");
    }

    private void autoReject(ODRequest od) {
        od.setStatus(RequestStatus.REJECTED);
        od.setApprovedBy("SYSTEM");
        od.setApprovedRole("SYSTEM");
        od.setApprovedAt(LocalDateTime.now());
        od.setEscalated(false);
        odRepository.save(od);

        notify(od.getEmployeeId(),
                "info@yourcompany.com",
                EventType.OD_REJECTED,
                "Your OD request from " + od.getStartDate() + " to " + od.getEndDate()
                        + " was auto-rejected because no approver responded in time.");
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════

    public Page<ODRequest> getMyODRequests(String employeeId, Pageable pageable) {
        return odRepository.findByEmployee_EmpId(employeeId, pageable);
    }

    public List<ODRequest> getPendingForApprover(String approverId) {
        return odRepository.findByCurrentApproverIdAndStatus(approverId, RequestStatus.PENDING);
    }

    public List<ODRequest> getAllPendingODs() {
        return odRepository.findByStatus(RequestStatus.PENDING);
    }

    public List<ODRequest> getEscalatedODs() {
        return odRepository.findByEscalatedTrueAndStatus(RequestStatus.PENDING);
    }

    public Page<ODApproval> getApprovalHistory(Long odId, Pageable pageable) {
        return odApprovalRepository.findByOdIdOrderByDecidedAtDesc(odId, pageable);
    }

    public Page<ODApproval> getMyDecisions(String approverId, Pageable pageable) {
        return odApprovalRepository.findByApproverIdOrderByDecidedAtDesc(approverId, pageable);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE: ADVANCE OR FINALIZE
    // Mirrors LeaveApprovalService.advanceOrFinalize exactly
    // ═══════════════════════════════════════════════════════════════

    private void advanceOrFinalize(ODRequest od, Employee currentApprover, String comments) {
        ApprovalLevel current = od.getCurrentApprovalLevel();
        int required          = od.getRequiredApprovalLevels();

        if (current == ApprovalLevel.FIRST_APPROVER && required >= 2) {
            od.setCurrentApprovalLevel(ApprovalLevel.SECOND_APPROVER);
            od.setCurrentApproverId(od.getSecondApproverId());
            odRepository.save(od);
            notifySecondApprover(od, currentApprover);
            notifyEmployeeProgress(od, currentApprover,
                    "Your OD request has been approved at level 1. Pending final approval.");
        } else {
            finalizeOD(od, RequestStatus.APPROVED, currentApprover, comments);
        }
    }

    private void finalizeOD(ODRequest od, RequestStatus finalStatus,
                            Employee finalApprover, String comments) {
        od.setStatus(finalStatus);
        od.setApprovedBy(finalApprover.getEmpId());
        od.setApprovedRole(finalApprover.getRole().getRoleName());
        od.setApprovedAt(LocalDateTime.now());
        od.setEscalated(false);
        odRepository.save(od);

        notifyEmployee(od, finalApprover, finalStatus, comments);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE: VALIDATE APPROVER
    // ═══════════════════════════════════════════════════════════════

    private void validateApproverForLevel(ODRequest od, Employee approver,
                                          ApprovalLevel level) {
        switch (level) {
            case FIRST_APPROVER -> {
                if (od.getFirstApproverId() == null
                        || !approver.getEmpId().equals(od.getFirstApproverId())) {
                    throw new BadRequestException(
                            "Unauthorized: You are not the assigned level-1 approver for this OD.");
                }
            }
            case SECOND_APPROVER -> {
                if (od.getSecondApproverId() == null
                        || !approver.getEmpId().equals(od.getSecondApproverId())) {
                    throw new BadRequestException(
                            "Unauthorized: You are not the assigned level-2 approver for this OD.");
                }
            }
        }
    }

    private void recordLevelDecision(ODRequest od, ApprovalLevel level,
                                     RequestStatus decision) {
        switch (level) {
            case FIRST_APPROVER -> {
                od.setFirstApproverDecision(decision);
                od.setFirstApproverDecidedAt(LocalDateTime.now());
            }
            case SECOND_APPROVER -> {
                od.setSecondApproverDecision(decision);
                od.setSecondApproverDecidedAt(LocalDateTime.now());
            }
        }
    }

    private void saveApprovalRecord(Long odId, Employee approver, ApprovalLevel level,
                                    RequestStatus decision, String comments) {
        ODApproval record = new ODApproval();
        record.setOdId(odId);
        record.setApproverId(approver.getEmpId());
        record.setApprovalLevel(level);
        record.setApproverRole(approver.getRole().getRoleName());
        record.setDecision(decision);
        record.setComments(comments);
        record.setDecidedAt(LocalDateTime.now());
        odApprovalRepository.save(record);
    }

    // ═══════════════════════════════════════════════════════════════
    // DURATION CALCULATION
    // Simple inclusive weekday count. Replace with HolidayChecker if available.
    // ═══════════════════════════════════════════════════════════════

    private BigDecimal calculateODDuration(LocalDate start, LocalDate end) {
        long count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            switch (current.getDayOfWeek()) {
                case SATURDAY:
                case SUNDAY:
                    break;
                default:
                    count++;
            }
            current = current.plusDays(1);
        }
        return BigDecimal.valueOf(count);
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void notifyFirstApprover(ODRequest od, Employee employee) {
        if (od.getFirstApproverId() == null) return;
        employeeRepository.findByEmpId(od.getFirstApproverId()).ifPresent(approver ->
                notificationService.createNotification(
                        approver.getEmpId(),
                        employee.getEmail(),
                        approver.getEmail(),
                        EventType.OD_APPLIED,
                        Channel.EMAIL,
                        employee.getName() + " submitted an OD request from "
                                + od.getStartDate() + " to " + od.getEndDate()
                                + ". Awaiting your approval."));
    }

    private void notifySecondApprover(ODRequest od, Employee firstApprover) {
        if (od.getSecondApproverId() == null) return;
        employeeRepository.findByEmpId(od.getSecondApproverId()).ifPresent(mgr -> {
            String empName = od.getEmployeeName() != null ? od.getEmployeeName() : "Employee";
            notificationService.createNotification(
                    mgr.getEmpId(),
                    firstApprover.getEmail(),
                    mgr.getEmail(),
                    EventType.OD_APPLIED,
                    Channel.EMAIL,
                    empName + "'s OD request (" + od.getStartDate() + " to " + od.getEndDate()
                            + ") approved at level 1. Requires your final approval.");
        });
    }

    private void notifyEmployee(ODRequest od, Employee approver,
                                RequestStatus decision, String reason) {
        employeeRepository.findByEmpId(od.getEmployeeId()).ifPresent(emp -> {
            String context = switch (decision) {
                case APPROVED -> "Your OD request from " + od.getStartDate()
                        + " to " + od.getEndDate() + " has been fully approved.";
                case REJECTED -> "Your OD request from " + od.getStartDate()
                        + " to " + od.getEndDate() + " has been rejected."
                        + (reason != null ? " Reason: " + reason : "");
                default -> "An update is available for your OD request from "
                        + od.getStartDate() + " to " + od.getEndDate() + ".";
            };
            notificationService.createNotification(
                    emp.getEmpId(),
                    approver.getEmail(),
                    emp.getEmail(),
                    mapEventType(decision),
                    Channel.EMAIL,
                    context);
        });
    }

    private void notifyEmployeeProgress(ODRequest od, Employee approver, String message) {
        employeeRepository.findByEmpId(od.getEmployeeId()).ifPresent(emp ->
                notificationService.createNotification(
                        emp.getEmpId(),
                        approver.getEmail(),
                        emp.getEmail(),
                        EventType.OD_IN_PROGRESS,
                        Channel.EMAIL,
                        message + " (OD: " + od.getStartDate() + " to " + od.getEndDate() + ")"));
    }

    private void notifyEscalatedApprover(ODRequest od, String newApproverId,
                                         String escalationReason) {
        employeeRepository.findByEmpId(newApproverId).ifPresent(approver ->
                employeeRepository.findByEmpId(od.getEmployeeId()).ifPresent(emp -> {
                    String message = "ESCALATED: " + emp.getName() + "'s OD request ("
                            + od.getStartDate() + " to " + od.getEndDate()
                            + ") requires your decision. " + escalationReason;
                    notificationService.createNotification(
                            approver.getEmpId(),
                            "info@yourcompany.com",
                            approver.getEmail(),
                            EventType.OD_APPLIED,
                            Channel.EMAIL,
                            message);
                }));
    }

    private void notify(String recipientEmpId, String fromEmail,
                        EventType eventType, String context) {
        employeeRepository.findByEmpId(recipientEmpId).ifPresent(emp ->
                notificationService.createNotification(
                        emp.getEmpId(),
                        fromEmail,
                        emp.getEmail(),
                        eventType,
                        Channel.EMAIL,
                        context));
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private void validateDecision(RequestStatus decision) {
        if (decision != RequestStatus.APPROVED && decision != RequestStatus.REJECTED) {
            throw new BadRequestException("Invalid decision: " + decision);
        }
    }

    private EventType mapEventType(RequestStatus status) {
        return switch (status) {
            case APPROVED -> EventType.OD_APPROVED;
            case REJECTED -> EventType.OD_REJECTED;
            default       -> EventType.OD_APPLIED;
        };
    }
}