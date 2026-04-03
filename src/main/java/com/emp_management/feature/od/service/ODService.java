package com.emp_management.feature.od.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.od.entity.ODApproval;
import com.emp_management.feature.od.entity.ODRequest;
import com.emp_management.feature.od.repository.ODApprovalRepository;
import com.emp_management.feature.od.repository.ODRequestRepository;
import com.emp_management.shared.enums.ApprovalLevel;
import com.emp_management.shared.enums.Channel;
import com.emp_management.shared.enums.EventType;
import com.emp_management.shared.enums.RequestStatus;
import com.emp_management.shared.exceptions.BadRequestException;
import com.emp_management.shared.exceptions.ResourceNotFoundException;
import com.emp_management.feature.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OD (On-Duty) Service
 *
 * All 10 concepts implemented:
 *  1.  OD application — future dates only, employee + manager must exist
 *  2.  Two-level approval — FIRST_APPROVER → SECOND_APPROVER (mirrors LeaveApprovalService)
 *  3.  Escalation — @Scheduled job inside this service, no separate file
 *  4.  Cancel during PENDING — allowed
 *  5.  Cancel when APPROVED — blocked
 *  6.  Approve / Reject at current level
 *  7.  Full audit trail — ODApproval record saved for every decision
 *  8.  Overlap prevention — query checks existing PENDING/APPROVED ODs
 *  9.  Notifications — triggered on every state change
 * 10.  Block OD on dates with approved leave
 */
@Service
@Transactional
public class ODService {

    private static final Logger log = LoggerFactory.getLogger(ODService.class);
    private static final int ESCALATION_DAYS = 3;

    private final ODRequestRepository  odRepository;
    private final ODApprovalRepository odApprovalRepository;
    private final EmployeeRepository   employeeRepository;
    private final NotificationService  notificationService;

    public ODService(ODRequestRepository odRepository,
                     ODApprovalRepository odApprovalRepository,
                     EmployeeRepository employeeRepository,
                     NotificationService notificationService) {
        this.odRepository        = odRepository;
        this.odApprovalRepository = odApprovalRepository;
        this.employeeRepository  = employeeRepository;
        this.notificationService = notificationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONCEPT #1 — APPLY OD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Creates an OD request.
     * Validations:
     *   - Employee exists
     *   - End date not before start date
     *   - Start date must be strictly after today (no same-day OD)
     *   - No overlapping OD (PENDING or APPROVED) in that date range
     *   - No APPROVED leave in that date range
     *   - Employee must have a reporting manager
     *
     * Sets up approval chain mirroring setupApprovalChain() in LeaveApplicationService.
     * Triggers OD_APPLIED notification to first approver.
     */
    public ODRequest createOD(String empCode, ODRequest request) {

        Employee employee = employeeRepository.findByEmpId(empCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + empCode));

        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date.");
        }

        // Concept #1: Future dates only — same-day OD not allowed
        if (!request.getStartDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("OD can only be applied for future dates (not today or past).");
        }

        // Concept #8: Prevent overlapping ODs
        List<ODRequest> overlapping = odRepository.findOverlappingODs(
                empCode, request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("An OD request already exists for these dates.");
        }

        // Concept #10: Block OD on dates with approved leave
        Boolean hasLeave = odRepository.hasApprovedLeaveInRange(
                empCode, request.getStartDate(), request.getEndDate());
        if (Boolean.TRUE.equals(hasLeave)) {
            throw new BadRequestException(
                    "Cannot apply OD on dates where you already have an approved leave.");
        }

        // Setup approval chain (mirrors LeaveApplicationService.setupApprovalChain)
        setupApprovalChain(request, employee);

        request.setEmployee(employee);
        request.setStatus(RequestStatus.PENDING);
        request.setYear(request.getStartDate().getYear());
        request.setEscalated(false);
        request.setEscalatedAt(null);

        ODRequest saved = odRepository.save(request);

        // Concept #9: Notify first approver
        if (saved.getFirstApproverId() != null) {
            employeeRepository.findByEmpId(saved.getFirstApproverId()).ifPresent(approver ->
                    sendNotification(
                            approver.getEmpId(), employee.getEmail(), approver.getEmail(),
                            EventType.OD_APPLIED,
                            employee.getName() + " applied for OD from "
                                    + saved.getStartDate() + " to " + saved.getEndDate()
                                    + ". Awaiting your approval."
                    )
            );
        }

        return saved;
    }

    /**
     * Mirrors LeaveApplicationService.setupApprovalChain().
     * Uses employee.getReportingId() as the first approver's empId (String).
     */
    private void setupApprovalChain(ODRequest od, Employee employee) {
        String firstApproverEmpId = employee.getReportingId();

        if (firstApproverEmpId == null) {
            // No manager → single-level auto (can be extended to auto-approve if needed)
            throw new BadRequestException("No manager assigned. Cannot submit OD request.");
        }

        Employee firstApprover = employeeRepository.findByEmpId(firstApproverEmpId)
                .orElseThrow(() -> new ResourceNotFoundException(
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
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Second approver not found: " + secondApproverEmpId));
            od.setSecondApproverId(secondApprover.getEmpId());
            od.setRequiredApprovalLevels(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONCEPT #6 — APPROVE OD
    // CONCEPT #2 — TWO-LEVEL APPROVAL
    // CONCEPT #7 — STORE EVERY DECISION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Approve OD at the current approval level.
     *
     * FIRST_APPROVER approved + requiredLevels == 2 → advance to SECOND_APPROVER
     * FIRST_APPROVER approved + requiredLevels == 1 → finalize as APPROVED
     * SECOND_APPROVER approved                     → finalize as APPROVED
     *
     * Saves an ODApproval audit record for every decision (Concept #7).
     */
    public ODRequest approveOD(Long odId, String approverEmpCode, String comments) {

        ODRequest od = findOD(odId);

        if (od.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("OD is already finalized with status: " + od.getStatus());
        }

        Employee approver = employeeRepository.findByEmpId(approverEmpCode)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found: " + approverEmpCode));

        validateApproverForCurrentLevel(od, approver);
        recordLevelDecision(od, approver.getEmpId(), RequestStatus.APPROVED);
        saveAuditRecord(od.getId(), approver, od.getCurrentApprovalLevel(), RequestStatus.APPROVED, comments);

        advanceOrFinalize(od, approver);
        return od;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONCEPT #6 — REJECT OD
    // CONCEPT #7 — STORE EVERY DECISION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reject OD at the current approval level.
     * Any level can reject — status goes straight to REJECTED.
     * Saves an ODApproval audit record (Concept #7).
     */
    public ODRequest rejectOD(Long odId, String approverEmpCode, String comments) {

        ODRequest od = findOD(odId);

        if (od.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Cannot reject. OD status is: " + od.getStatus());
        }

        Employee approver = employeeRepository.findByEmpId(approverEmpCode)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found: " + approverEmpCode));

        validateApproverForCurrentLevel(od, approver);
        recordLevelDecision(od, approver.getEmpId(), RequestStatus.REJECTED);
        saveAuditRecord(od.getId(), approver, od.getCurrentApprovalLevel(), RequestStatus.REJECTED, comments);

        finalizeOD(od, RequestStatus.REJECTED, approver, comments);
        return od;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONCEPT #4 — CANCEL (PENDING only)
    // CONCEPT #5 — CANNOT CANCEL when APPROVED
    // ═══════════════════════════════════════════════════════════════

    /**
     * Cancel an OD request.
     * Only the employee who applied can cancel.
     * Only PENDING OD can be cancelled (not APPROVED, REJECTED, CANCELLED).
     * Notifies the current approver of the cancellation.
     */
    public ODRequest cancelOD(Long odId, String empCode) {

        ODRequest od = findOD(odId);

        if (!od.getEmployeeId().equals(empCode)) {
            throw new BadRequestException("Only the OD applicant can cancel this request.");
        }

        // Concept #4: Only PENDING can be cancelled
        // Concept #5: APPROVED cannot be cancelled
        if (od.getStatus() == RequestStatus.APPROVED) {
            throw new BadRequestException(
                    "Cannot cancel an APPROVED OD. Please contact your manager.");
        }
        if (od.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "Cannot cancel OD with status: " + od.getStatus()
                            + ". Only PENDING OD can be cancelled.");
        }

        String pendingApproverId = od.getCurrentApproverId();
        od.setStatus(RequestStatus.CANCELLED);
        od.setCurrentApproverId(null);
        odRepository.save(od);

        // Concept #9: Notify the approver who was waiting
        if (pendingApproverId != null) {
            employeeRepository.findByEmpId(pendingApproverId).ifPresent(approver ->
                    sendNotification(
                            approver.getEmpId(),
                            od.getEmployee().getEmail(),
                            approver.getEmail(),
                            EventType.OD_CANCELLED,
                            od.getEmployeeName() + "'s OD request from "
                                    + od.getStartDate() + " to " + od.getEndDate()
                                    + " has been CANCELLED by the employee."
                    )
            );
        }

        return od;
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════

    public List<ODRequest> getMyODRequests(String empCode) {
        employeeRepository.findByEmpId(empCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + empCode));
        return odRepository.findByEmployee_EmpId(empCode);
    }

    public List<ODRequest> getMyPendingApprovals(String approverId) {
        employeeRepository.findByEmpId(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + approverId));
        return odRepository.findByCurrentApproverIdAndStatus(approverId, RequestStatus.PENDING);
    }

    public Page<ODApproval> getApprovalHistory(Long odId, Pageable pageable) {
        return odApprovalRepository.findByOdIdOrderByDecidedAtDesc(odId, pageable);
    }

    public List<ODRequest> getEscalatedODs() {
        return odRepository.findByEscalatedTrueAndStatus(RequestStatus.PENDING);
    }

    // ═══════════════════════════════════════════════════════════════
    // CONCEPT #3 — ESCALATION (runs inside this service, no new file)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Runs daily at 9 AM.
     * Finds PENDING ODs not yet escalated that were created more than ESCALATION_DAYS ago.
     * If there is a higher manager, escalate to them.
     * If no higher manager exists, auto-reject.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void escalatePendingODs() {
        log.info("[OD-SCHEDULER] Starting OD escalation job");

        LocalDateTime threshold = LocalDateTime.now().minusDays(ESCALATION_DAYS);
        List<ODRequest> pendingODs = odRepository
                .findByStatusAndCreatedAtBeforeAndEscalatedFalse(RequestStatus.PENDING, threshold);

        log.info("[OD-SCHEDULER] Found {} OD(s) eligible for escalation", pendingODs.size());

        for (ODRequest od : pendingODs) {
            try {
                processEscalation(od);
            } catch (Exception e) {
                log.error("[OD-SCHEDULER] Error escalating OD ID {}: {}", od.getId(), e.getMessage());
            }
        }

        log.info("[OD-SCHEDULER] OD escalation job complete");
    }

    private void processEscalation(ODRequest od) {
        if (od.getCurrentApproverId() == null) {
            autoRejectOD(od, "No approver assigned.");
            return;
        }

        Employee currentApprover = employeeRepository.findByEmpId(od.getCurrentApproverId())
                .orElse(null);

        if (currentApprover == null) {
            autoRejectOD(od, "Assigned approver no longer exists.");
            return;
        }

        String higherManagerId = currentApprover.getReportingId();
        if (higherManagerId == null) {
            autoRejectOD(od, "No higher manager available for escalation.");
            return;
        }

        Employee higherManager = employeeRepository.findByEmpId(higherManagerId).orElse(null);
        if (higherManager == null) {
            autoRejectOD(od, "Higher manager record not found.");
            return;
        }

        // Escalate: move to next level if currently at FIRST_APPROVER
        if (od.getCurrentApprovalLevel() == ApprovalLevel.FIRST_APPROVER) {
            od.setCurrentApprovalLevel(ApprovalLevel.SECOND_APPROVER);
            if (od.getSecondApproverId() == null) {
                od.setSecondApproverId(higherManager.getEmpId());
            }
        }
        od.setCurrentApproverId(higherManager.getEmpId());
        od.setEscalated(true);
        od.setEscalatedAt(LocalDateTime.now());
        odRepository.save(od);

        log.info("[OD-SCHEDULER] Escalated OD ID {} to {}", od.getId(), higherManager.getEmpId());

        // Concept #9: Notify escalated-to manager
        sendNotification(
                higherManager.getEmpId(),
                od.getEmployee().getEmail(),
                higherManager.getEmail(),
                EventType.OD_ESCALATED,
                "OD request from " + od.getEmployeeName()
                        + " (" + od.getStartDate() + " to " + od.getEndDate()
                        + ") has been ESCALATED to you after " + ESCALATION_DAYS
                        + " days of no action by the previous approver."
        );
    }

    private void autoRejectOD(ODRequest od, String reason) {
        log.warn("[OD-SCHEDULER] Auto-rejecting OD ID {} — {}", od.getId(), reason);
        od.setStatus(RequestStatus.REJECTED);
        od.setCurrentApproverId(null);
        odRepository.save(od);

        // Concept #9: Notify employee of auto-rejection
        sendNotification(
                od.getEmployeeId(),
                "noreply@company.com",
                od.getEmployee().getEmail(),
                EventType.OD_REJECTED,
                "Your OD request from " + od.getStartDate() + " to " + od.getEndDate()
                        + " was AUTO-REJECTED after " + ESCALATION_DAYS
                        + " days of no approver action. Reason: " + reason
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE: APPROVAL HELPERS (mirrors LeaveApprovalService)
    // ═══════════════════════════════════════════════════════════════

    /** Advance to next level or finalize — mirrors advanceOrFinalize() in LeaveApprovalService */
    private void advanceOrFinalize(ODRequest od, Employee currentApprover) {
        ApprovalLevel current  = od.getCurrentApprovalLevel();
        int required           = od.getRequiredApprovalLevels();

        if (current == ApprovalLevel.FIRST_APPROVER && required >= 2) {
            // Move to second approver
            od.setCurrentApprovalLevel(ApprovalLevel.SECOND_APPROVER);
            od.setCurrentApproverId(od.getSecondApproverId());
            od.setEscalated(false);
            odRepository.save(od);

            // Concept #9: Notify second approver
            if (od.getSecondApproverId() != null) {
                employeeRepository.findByEmpId(od.getSecondApproverId()).ifPresent(mgr ->
                        sendNotification(
                                mgr.getEmpId(),
                                currentApprover.getEmail(),
                                mgr.getEmail(),
                                EventType.OD_APPLIED,
                                od.getEmployeeName() + "'s OD from "
                                        + od.getStartDate() + " to " + od.getEndDate()
                                        + " approved at Level 1. Requires your final approval."
                        )
                );
            }

            // Notify employee of Level 1 progress
            sendNotification(
                    od.getEmployeeId(),
                    currentApprover.getEmail(),
                    od.getEmployee().getEmail(),
                    EventType.OD_IN_PROGRESS,
                    "Your OD from " + od.getStartDate() + " to " + od.getEndDate()
                            + " has been approved at Level 1. Pending final approval."
            );

        } else {
            // Final approval
            finalizeOD(od, RequestStatus.APPROVED, currentApprover, null);
        }
    }

    /** Finalize OD with given status. Mirrors finalizeLeave() in LeaveApprovalService. */
    private void finalizeOD(ODRequest od, RequestStatus finalStatus,
                            Employee finalApprover, String comments) {
        od.setStatus(finalStatus);
        od.setApprovedBy(finalApprover.getEmpId());
        od.setApprovedRole(finalApprover.getRole().getRoleName());
        od.setApprovedAt(LocalDateTime.now());
        od.setEscalated(false);
        od.setCurrentApproverId(null);
        odRepository.save(od);

        // Concept #9: Notify employee of final decision
        EventType event = finalStatus == RequestStatus.APPROVED ? EventType.OD_APPROVED : EventType.OD_REJECTED;
        String message = finalStatus == RequestStatus.APPROVED
                ? "Your OD from " + od.getStartDate() + " to " + od.getEndDate() + " has been fully APPROVED."
                : "Your OD from " + od.getStartDate() + " to " + od.getEndDate()
                + " has been REJECTED." + (comments != null ? " Reason: " + comments : "");

        sendNotification(
                od.getEmployeeId(),
                finalApprover.getEmail(),
                od.getEmployee().getEmail(),
                event,
                message
        );
    }

    /** Validates that the given approver is the current assigned approver. */
    private void validateApproverForCurrentLevel(ODRequest od, Employee approver) {
        if (od.getCurrentApproverId() == null
                || !approver.getEmpId().equals(od.getCurrentApproverId())) {
            throw new BadRequestException(
                    "You are not the current assigned approver for this OD. "
                            + "Current approver: " + od.getCurrentApproverId());
        }
    }

    /** Records the decision timestamps on the OD entity itself. Mirrors recordLevelDecision(). */
    private void recordLevelDecision(ODRequest od, String approverId, RequestStatus decision) {
        switch (od.getCurrentApprovalLevel()) {
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

    /**
     * Saves an ODApproval audit record.
     * Concept #7: every approval / rejection action is persisted.
     */
    private void saveAuditRecord(Long odId, Employee approver, ApprovalLevel level,
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

    private ODRequest findOD(Long odId) {
        return odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD request not found with ID: " + odId));
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION HELPER
    // ═══════════════════════════════════════════════════════════════

    /** Concept #9 — single notification helper used throughout this service. */
    private void sendNotification(String recipientEmpCode,
                                  String fromEmail,
                                  String toEmail,
                                  EventType eventType,
                                  String context) {
        try {
            notificationService.createNotification(
                    recipientEmpCode, fromEmail, toEmail, eventType, Channel.EMAIL, context);
        } catch (Exception e) {
            log.warn("[OD] Notification failed for {} ({}): {}", recipientEmpCode, eventType, e.getMessage());
        }
    }
}