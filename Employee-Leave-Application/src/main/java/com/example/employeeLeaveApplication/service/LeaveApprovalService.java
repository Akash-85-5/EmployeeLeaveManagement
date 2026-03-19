package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.BulkLeaveDecisionRequest;
import com.example.employeeLeaveApplication.dto.LeaveApplicationWithAttachmentsDto;
import com.example.employeeLeaveApplication.dto.LeaveDecisionRequest;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveApproval;
import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveApprovalRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.repository.LeaveAttachmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveApprovalService {

    private final EmployeeRepository employeeRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final NotificationService notificationService;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveApplicationService leaveApplicationService;
    private final LeaveAttachmentRepository leaveAttachmentRepository;

    public LeaveApprovalService(EmployeeRepository employeeRepository,
                                LeaveApplicationRepository leaveApplicationRepository,
                                NotificationService notificationService,
                                LeaveApprovalRepository leaveApprovalRepository,
                                LeaveApplicationService leaveApplicationService,
                                LeaveAttachmentRepository leaveAttachmentRepository) {
        this.employeeRepository        = employeeRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.notificationService        = notificationService;
        this.leaveApprovalRepository    = leaveApprovalRepository;
        this.leaveApplicationService    = leaveApplicationService;
        this.leaveAttachmentRepository  = leaveAttachmentRepository;
    }

    // ═══════════════════════════════════════════════════════════════
    // PENDING LEAVES WITH ATTACHMENTS — per role
    // ═══════════════════════════════════════════════════════════════

    /**
     * Fetch pending leaves for Team Leader WITH attachments.
     * Optimized: Single query for leaves + batch fetch attachments.
     */
    public Page<LeaveApplicationWithAttachmentsDto> getPendingLeavesForTeamLeaderWithAttachments(
            Long teamLeaderId, Pageable pageable) {

        List<LeaveApplication> all = leaveApplicationRepository
                .findByTeamLeaderIdAndStatusAndCurrentApprovalLevel(
                        teamLeaderId, LeaveStatus.PENDING, ApprovalLevel.TEAM_LEADER);

        // Convert to DTOs with attachments
        List<LeaveApplicationWithAttachmentsDto> dtos =
                convertToDto(all);

        return toPageDto(dtos, pageable);
    }

    /**
     * Fetch pending leaves for Manager WITH attachments.
     */
    public Page<LeaveApplicationWithAttachmentsDto> getPendingLeavesForManagerWithAttachments(
            Long managerId, Pageable pageable) {

        List<LeaveApplication> all = leaveApplicationRepository
                .findByManagerIdAndStatusAndCurrentApprovalLevel(
                        managerId, LeaveStatus.PENDING, ApprovalLevel.MANAGER);

        List<LeaveApplicationWithAttachmentsDto> dtos =
                convertToDto(all);

        return toPageDto(dtos, pageable);
    }

    /**
     * Fetch pending leaves for HR WITH attachments.
     */
    public Page<LeaveApplicationWithAttachmentsDto> getPendingLeavesForHrWithAttachments(
            Pageable pageable) {

        List<LeaveApplication> all = leaveApplicationRepository
                .findByStatusAndCurrentApprovalLevel(LeaveStatus.PENDING, ApprovalLevel.HR);

        List<LeaveApplicationWithAttachmentsDto> dtos =
                convertToDto(all);

        return toPageDto(dtos, pageable);
    }

    /**
     * Get single leave application with attachments by ID.
     * Used when approver clicks to view a specific leave.
     */
    public LeaveApplicationWithAttachmentsDto getLeaveApplicationWithAttachments(Long leaveId) {
        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new BadRequestException("Leave not found with ID: " + leaveId));

        return new LeaveApplicationWithAttachmentsDto(
                leave,
                leaveAttachmentRepository.findByLeaveApplicationId(leaveId)
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER: Convert List<LeaveApplication> to List<DTO with Attachments>
    // ═══════════════════════════════════════════════════════════════

    /**
     * Converts a list of LeaveApplications to DTOs with attachments.
     * Optimized approach:
     *  1. Get all leave IDs
     *  2. Batch fetch all attachments for those leave IDs
     *  3. Create DTOs by matching attachments to leaves
     *
     * This avoids N+1 queries (1 query per leave for attachments).
     */
    private List<LeaveApplicationWithAttachmentsDto> convertToDto(
            List<LeaveApplication> leaves) {

        if (leaves.isEmpty()) {
            return List.of();
        }

        // Extract leave IDs
        List<Long> leaveIds = leaves.stream()
                .map(LeaveApplication::getId)
                .collect(Collectors.toList());

        // Batch fetch ALL attachments for these leaves in ONE query
        List<LeaveAttachment> allAttachments =
                leaveAttachmentRepository.findByLeaveApplicationIdIn(leaveIds);

        // Group attachments by leaveId (for fast lookup)
        Map<Long, List<LeaveAttachment>> attachmentsByLeaveId = leaveAttachmentRepository
                .findByLeaveApplicationIdIn(leaveIds)
                .stream()
                .collect(Collectors.groupingBy(att -> att.getLeaveApplicationId()));

        // Create DTOs
        return leaves.stream()
                .map(leave -> {
                    List<LeaveAttachment> attachments =
                            attachmentsByLeaveId.getOrDefault(leave.getId(), List.of());
                    return new LeaveApplicationWithAttachmentsDto(leave, attachments);
                })
                .collect(Collectors.toList());
    }

    /**
     * Helper method to convert List to Page.
     */
    private <T> Page<T> toPageDto(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), list.size());
        List<T> content = (start > list.size()) ? List.of() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // ORIGINAL METHODS (unchanged)
    // ═══════════════════════════════════════════════════════════════

    public Page<LeaveApplication> getPendingLeavesForTeamLeader(Long teamLeaderId, Pageable pageable) {
        List<LeaveApplication> all = leaveApplicationRepository
                .findByTeamLeaderIdAndStatusAndCurrentApprovalLevel(
                        teamLeaderId, LeaveStatus.PENDING, ApprovalLevel.TEAM_LEADER);
        return toPage(all, pageable);
    }

    public Page<LeaveApplication> getPendingLeavesForManager(Long managerId, Pageable pageable) {
        List<LeaveApplication> all = leaveApplicationRepository
                .findByManagerIdAndStatusAndCurrentApprovalLevel(
                        managerId, LeaveStatus.PENDING, ApprovalLevel.MANAGER);
        return toPage(all, pageable);
    }

    public Page<LeaveApplication> getPendingLeavesForHr(Pageable pageable) {
        List<LeaveApplication> all = leaveApplicationRepository
                .findByStatusAndCurrentApprovalLevel(LeaveStatus.PENDING, ApprovalLevel.HR);
        return toPage(all, pageable);
    }

    // ═══════════════════════════════════════════════════════════════
    // CORE DECISION (unchanged)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void decideLeave(LeaveDecisionRequest request) {
        validateDecision(request.getDecision());

        LeaveApplication leave = leaveApplicationRepository.findById(request.getLeaveId())
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException(
                    "Leave is already processed with status: " + leave.getStatus());
        }

        Employee approver = employeeRepository.findById(request.getApproverId())
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        ApprovalLevel currentLevel = leave.getCurrentApprovalLevel();
        validateApproverForLevel(leave, approver, currentLevel);
        recordLevelDecision(leave, approver, currentLevel, request.getDecision());
        saveApprovalRecord(leave.getId(), approver, currentLevel,
                request.getDecision(), request.getComments());

        if (request.getDecision() == LeaveStatus.REJECTED) {
            finalizeLeave(leave, LeaveStatus.REJECTED, approver);
        } else if (request.getDecision() == LeaveStatus.MEETING_REQUIRED) {
            leaveApplicationRepository.save(leave);
            notifyEmployee(leave, approver, request.getDecision(), request.getComments());
        } else {
            advanceOrFinalize(leave, approver);
        }
    }

    @Transactional
    public void approveLeave(Long leaveId, Long approverId, String comments) {
        LeaveDecisionRequest req = new LeaveDecisionRequest();
        req.setLeaveId(leaveId);
        req.setApproverId(approverId);
        req.setDecision(LeaveStatus.APPROVED);
        req.setComments(comments);
        decideLeave(req);
    }

    @Transactional
    public void rejectLeave(Long leaveId, Long approverId, String comments) {
        LeaveDecisionRequest req = new LeaveDecisionRequest();
        req.setLeaveId(leaveId);
        req.setApproverId(approverId);
        req.setDecision(LeaveStatus.REJECTED);
        req.setComments(comments);
        decideLeave(req);
    }

    @Transactional
    public String bulkDecision(BulkLeaveDecisionRequest request, boolean isHr) {
        validateDecision(request.getDecision());

        List<LeaveApplication> leaves =
                leaveApplicationRepository.findAllById(request.getLeaveIds());
        if (leaves.isEmpty()) throw new RuntimeException("No leaves found");

        Employee approver = employeeRepository.findById(request.getApproverId())
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        for (LeaveApplication leave : leaves) {
            if (leave.getStatus() != LeaveStatus.PENDING) continue;

            ApprovalLevel currentLevel = leave.getCurrentApprovalLevel();
            if (isHr && currentLevel != ApprovalLevel.HR) continue;

            try {
                validateApproverForLevel(leave, approver, currentLevel);
            } catch (BadRequestException e) {
                continue;
            }

            recordLevelDecision(leave, approver, currentLevel, request.getDecision());
            saveApprovalRecord(leave.getId(), approver, currentLevel,
                    request.getDecision(), "Bulk decision");

            if (request.getDecision() == LeaveStatus.REJECTED) {
                finalizeLeave(leave, LeaveStatus.REJECTED, approver);
            } else if (request.getDecision() == LeaveStatus.APPROVED) {
                advanceOrFinalize(leave, approver);
            }
        }
        return "Bulk decision completed successfully";
    }

    public Page<LeaveApproval> getApprovalHistory(Long leaveId, Pageable pageable) {
        return leaveApprovalRepository.findByLeaveIdOrderByDecidedAtDesc(leaveId, pageable);
    }

    public Page<LeaveApproval> getManagerDecisions(Long approverId, Pageable pageable) {
        return leaveApprovalRepository.findByApproverIdOrderByDecidedAtDesc(approverId, pageable);
    }

    public List<LeaveApplication> getEscalatedLeavesForHr() {
        return leaveApplicationRepository.findByEscalatedTrueAndStatus(LeaveStatus.PENDING);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS (unchanged)
    // ═══════════════════════════════════════════════════════════════

    private void advanceOrFinalize(LeaveApplication leave, Employee currentApprover) {
        int required = leave.getRequiredApprovalLevels();
        ApprovalLevel current = leave.getCurrentApprovalLevel();

        if (current == ApprovalLevel.TEAM_LEADER) {
            if (required >= 2) {
                leave.setCurrentApprovalLevel(ApprovalLevel.MANAGER);
                leaveApplicationRepository.save(leave);
                notifyManager(leave, currentApprover);
                notifyEmployeeProgress(leave, currentApprover,
                        "Your leave has been approved by Team Leader. Pending Manager approval.");
            } else {
                finalizeLeave(leave, LeaveStatus.APPROVED, currentApprover);
            }

        } else if (current == ApprovalLevel.MANAGER) {
            if (required >= 3) {
                leave.setCurrentApprovalLevel(ApprovalLevel.HR);
                leave.setEscalated(true);
                leave.setEscalatedAt(LocalDateTime.now());
                leaveApplicationRepository.save(leave);
                notifyHr(leave, currentApprover);
                notifyEmployeeProgress(leave, currentApprover,
                        "Your leave has been approved by Manager. Pending HR approval.");
            } else {
                finalizeLeave(leave, LeaveStatus.APPROVED, currentApprover);
            }

        } else if (current == ApprovalLevel.HR) {
            finalizeLeave(leave, LeaveStatus.APPROVED, currentApprover);
        }
    }

    private void finalizeLeave(LeaveApplication leave,
                               LeaveStatus finalStatus,
                               Employee finalApprover) {
        leave.setStatus(finalStatus);
        leave.setApprovedBy(finalApprover.getId());
        leave.setApprovedRole(finalApprover.getRole());
        leave.setApprovedAt(LocalDateTime.now());
        leave.setEscalated(false);

        if (finalStatus == LeaveStatus.APPROVED) {
            leaveApplicationService.applyBalanceDeduction(leave);
        }

        leaveApplicationRepository.save(leave);
        notifyEmployee(leave, finalApprover, finalStatus, null);
    }

    private void validateApproverForLevel(LeaveApplication leave,
                                          Employee approver, ApprovalLevel level) {
        switch (level) {
            case TEAM_LEADER -> {
                if (leave.getTeamLeaderId() == null
                        || !approver.getId().equals(leave.getTeamLeaderId())) {
                    throw new BadRequestException(
                            "Unauthorized: Only the assigned Team Leader can approve.");
                }
                if (approver.getRole() != Role.TEAM_LEADER) {
                    throw new BadRequestException("Approver does not have TEAM_LEADER role.");
                }
            }
            case MANAGER -> {
                if (!approver.getId().equals(leave.getManagerId())) {
                    throw new BadRequestException(
                            "Unauthorized: Only the assigned Manager can approve at this stage.");
                }
                if (approver.getRole() != Role.MANAGER) {
                    throw new BadRequestException("Approver does not have MANAGER role.");
                }
            }
            case HR -> {
                if (approver.getRole() != Role.HR) {
                    throw new BadRequestException("Only HR can approve at this stage.");
                }
            }
        }
    }

    private void recordLevelDecision(LeaveApplication leave, Employee approver,
                                     ApprovalLevel level, LeaveStatus decision) {
        switch (level) {
            case TEAM_LEADER -> {
                leave.setTeamLeaderDecision(decision);
                leave.setTeamLeaderDecidedAt(LocalDateTime.now());
            }
            case MANAGER -> {
                leave.setManagerDecision(decision);
                leave.setManagerDecidedAt(LocalDateTime.now());
            }
            case HR -> {
                leave.setHrDecision(decision);
                leave.setHrDecidedAt(LocalDateTime.now());
                leave.setHrDecidedBy(approver.getId());
            }
        }
    }

    private void saveApprovalRecord(Long leaveId, Employee approver, ApprovalLevel level,
                                    LeaveStatus decision, String comments) {
        LeaveApproval record = new LeaveApproval();
        record.setLeaveId(leaveId);
        record.setApproverId(approver.getId());
        record.setApprovalLevel(level);
        record.setApproverRole(approver.getRole());
        record.setDecision(decision);
        record.setComments(comments);
        record.setDecidedAt(LocalDateTime.now());
        leaveApprovalRepository.save(record);
    }

    private void notifyManager(LeaveApplication leave, Employee teamLeader) {
        if (leave.getManagerId() == null) return;
        employeeRepository.findById(leave.getManagerId()).ifPresent(mgr -> {
            String empName = employeeRepository.findById(leave.getEmployeeId())
                    .map(Employee::getName).orElse("Employee");
            notificationService.createNotification(
                    mgr.getId(), teamLeader.getEmail(), mgr.getEmail(),
                    EventType.LEAVE_APPLIED, mgr.getRole(), Channel.EMAIL,
                    empName + "'s leave (" + leave.getStartDate() + " to " + leave.getEndDate()
                            + ") approved by Team Leader. Requires your approval.");
        });
    }

    private void notifyHr(LeaveApplication leave, Employee manager) {
        String empName = employeeRepository.findById(leave.getEmployeeId())
                .map(Employee::getName).orElse("Employee");
        employeeRepository.findAllHr().forEach(hr ->
                notificationService.createNotification(
                        hr.getId(), manager.getEmail(), hr.getEmail(),
                        EventType.LEAVE_APPLIED, hr.getRole(), Channel.EMAIL,
                        empName + "'s leave (" + leave.getStartDate() + " to " + leave.getEndDate()
                                + ") approved by Manager. Requires HR approval.")
        );
    }

    private void notifyEmployee(LeaveApplication leave, Employee approver,
                                LeaveStatus decision, String comments) {
        employeeRepository.findById(leave.getEmployeeId()).ifPresent(emp -> {
            String context = switch (decision) {
                case APPROVED -> "Your leave from " + leave.getStartDate()
                        + " to " + leave.getEndDate() + " has been fully approved.";
                case REJECTED -> "Your leave from " + leave.getStartDate()
                        + " to " + leave.getEndDate()
                        + " has been rejected. Reason: "
                        + (comments != null ? comments : "N/A");
                default -> "A meeting is required regarding your leave from "
                        + leave.getStartDate() + " to " + leave.getEndDate() + ".";
            };
            notificationService.createNotification(
                    emp.getId(), approver.getEmail(), emp.getEmail(),
                    mapEventType(decision), emp.getRole(), Channel.EMAIL, context);
        });
    }

    private void notifyEmployeeProgress(LeaveApplication leave, Employee approver, String message) {
        employeeRepository.findById(leave.getEmployeeId()).ifPresent(emp ->
                notificationService.createNotification(
                        emp.getId(), approver.getEmail(), emp.getEmail(),
                        EventType.LEAVE_IN_PROGRESS, emp.getRole(), Channel.EMAIL,
                        message + " (Leave: " + leave.getStartDate() + " to " + leave.getEndDate() + ")")
        );
    }

    private void validateDecision(LeaveStatus decision) {
        if (decision != LeaveStatus.APPROVED
                && decision != LeaveStatus.REJECTED
                && decision != LeaveStatus.MEETING_REQUIRED) {
            throw new BadRequestException("Invalid decision: " + decision);
        }
    }

    private EventType mapEventType(LeaveStatus status) {
        return switch (status) {
            case APPROVED         -> EventType.LEAVE_APPROVED;
            case REJECTED         -> EventType.LEAVE_REJECTED;
            case MEETING_REQUIRED -> EventType.MEETING_REQUIRED;
            default               -> EventType.LEAVE_APPLIED;
        };
    }

    private <T> Page<T> toPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), list.size());
        List<T> content = (start > list.size()) ? List.of() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }
}