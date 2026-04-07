package com.example.employeeLeaveApplication.feature.leave.annual.service;

import com.example.employeeLeaveApplication.feature.leave.annual.dto.BulkLeaveDecisionRequest;
import com.example.employeeLeaveApplication.feature.leave.annual.dto.LeaveApplicationWithAttachmentsDto;
import com.example.employeeLeaveApplication.feature.leave.annual.dto.LeaveDecisionRequest;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApproval;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.feature.notification.service.NotificationService;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveApprovalRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveAttachmentRepository;
import com.example.employeeLeaveApplication.shared.enums.*;
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

    private final EmployeeRepository            employeeRepository;
    private final LeaveApplicationRepository    leaveApplicationRepository;
    private final NotificationService           notificationService;
    private final LeaveApprovalRepository       leaveApprovalRepository;
    private final LeaveApplicationService       leaveApplicationService;
    private final LeaveAttachmentRepository     leaveAttachmentRepository;

    public LeaveApprovalService(EmployeeRepository employeeRepository,
                                LeaveApplicationRepository leaveApplicationRepository,
                                NotificationService notificationService,
                                LeaveApprovalRepository leaveApprovalRepository,
                                LeaveApplicationService leaveApplicationService,
                                LeaveAttachmentRepository leaveAttachmentRepository) {
        this.employeeRepository         = employeeRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.notificationService        = notificationService;
        this.leaveApprovalRepository    = leaveApprovalRepository;
        this.leaveApplicationService    = leaveApplicationService;
        this.leaveAttachmentRepository  = leaveAttachmentRepository;
    }

    // ═══════════════════════════════════════════════════════════════
    // PENDING LEAVES WITH ATTACHMENTS
    // TEAM_LEADER = level-1 approver (employee's direct manager)
    // MANAGER     = level-2 approver (level-1's manager)
    // ═══════════════════════════════════════════════════════════════

//    public Page<LeaveApplicationWithAttachmentsDto> getPendingLeavesForTeamLeaderWithAttachments(
//            Long approverId, Pageable pageable) {
//        List<LeaveApplication> all = leaveApplicationRepository
//                .findByFirstApproverIdAndStatusAndCurrentApprovalLevel(
//                        approverId, LeaveStatus.PENDING, ApprovalLevel.FIRST_APPROVER);
//        return toPageDto(convertToDto(all), pageable);
//    }

    public Page<LeaveApplicationWithAttachmentsDto> getPendingLeavesForManagerWithAttachments(
            Long approverId, Pageable pageable) {
        List<LeaveApplication> all = leaveApplicationRepository
                .findByCurrentApproverIdAndStatus(
                        approverId, LeaveStatus.PENDING);
        return toPageDto(convertToDto(all), pageable);
    }

//    public Page<LeaveApplicationWithAttachmentsDto> getPendingLeavesForHrWithAttachments(
//            Pageable pageable) {
//        List<LeaveApplication> all = leaveApplicationRepository
//                .findByStatusAndCurrentApprovalLevel(LeaveStatus.PENDING, ApprovalLevel.HR);
//        return toPageDto(convertToDto(all), pageable);
//    }

    public LeaveApplicationWithAttachmentsDto getLeaveApplicationWithAttachments(Long leaveId) {
        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new BadRequestException("Leave not found with ID: " + leaveId));
        return new LeaveApplicationWithAttachmentsDto(
                leave, leaveAttachmentRepository.findByLeaveApplicationId(leaveId));
    }

//    public Page<LeaveApplication> getPendingLeavesForTeamLeader(Long approverId, Pageable pageable) {
//        return toPage(leaveApplicationRepository.findByFirstApproverIdAndStatusAndCurrentApprovalLevel(
//                approverId, LeaveStatus.PENDING, ApprovalLevel.FIRST_APPROVER), pageable);
//    }
//
//    public Page<LeaveApplication> getPendingLeavesForManager(Long approverId, Pageable pageable) {
//        return toPage(leaveApplicationRepository.findBySecondApproverIdAndStatusAndCurrentApprovalLevel(
//                approverId, LeaveStatus.PENDING, ApprovalLevel.SECOND_APPROVER), pageable);
//    }
//
//    public Page<LeaveApplication> getPendingLeavesForHr(Pageable pageable) {
//        return toPage(leaveApplicationRepository.findByStatusAndCurrentApprovalLevel(
//                LeaveStatus.PENDING, ApprovalLevel.HR), pageable);
//    }

    // ═══════════════════════════════════════════════════════════════
    // CORE DECISION
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

    // ═══════════════════════════════════════════════════════════════
    // BULK DECISION
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public String bulkDecision(BulkLeaveDecisionRequest request, boolean isSecondLevel) {
        validateDecision(request.getDecision());

        List<LeaveApplication> leaves =
                leaveApplicationRepository.findAllById(request.getLeaveIds());
        if (leaves.isEmpty()) throw new RuntimeException("No leaves found");

        Employee approver = employeeRepository.findById(request.getApproverId())
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        for (LeaveApplication leave : leaves) {
            if (leave.getStatus() != LeaveStatus.PENDING) continue;
            ApprovalLevel currentLevel = leave.getCurrentApprovalLevel();
            if (isSecondLevel && currentLevel != ApprovalLevel.SECOND_APPROVER) continue;

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

    // ═══════════════════════════════════════════════════════════════
    // HISTORY
    // ═══════════════════════════════════════════════════════════════

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
    // PRIVATE: ADVANCE OR FINALIZE
    //
    // Level TEAM_LEADER approved + required==2 → advance to MANAGER
    // Level TEAM_LEADER approved + required==1 → finalize
    // Level MANAGER approved → always finalize (max 2 levels)
    // ═══════════════════════════════════════════════════════════════

    private void advanceOrFinalize(LeaveApplication leave, Employee currentApprover) {
        ApprovalLevel current = leave.getCurrentApprovalLevel();
        int required          = leave.getRequiredApprovalLevels();

        if (current == ApprovalLevel.FIRST_APPROVER) {
            if (required >= 2) {
                leave.setCurrentApprovalLevel(ApprovalLevel.SECOND_APPROVER);
                leave.setCurrentApproverId(leave.getSecondApproverId());
                leaveApplicationRepository.save(leave);
                notifySecondApprover(leave, currentApprover);
                notifyEmployeeProgress(leave, currentApprover,
                        "Your leave has been approved at level 1. Pending final approval.");
            } else {
                finalizeLeave(leave, LeaveStatus.APPROVED, currentApprover);
            }
        } else {
            // MANAGER or HR → final level
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

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE: VALIDATE APPROVER
    // ═══════════════════════════════════════════════════════════════

    private void validateApproverForLevel(LeaveApplication leave,
                                          Employee approver, ApprovalLevel level) {
        switch (level) {
            case FIRST_APPROVER -> {
                if (leave.getFirstApproverId() == null
                        || !approver.getId().equals(leave.getFirstApproverId())) {
                    throw new BadRequestException(
                            "Unauthorized: You are not the assigned level-1 approver for this leave.");
                }
            }
            case SECOND_APPROVER -> {
                if (leave.getSecondApproverId() == null
                        || !approver.getId().equals(leave.getSecondApproverId())) {
                    throw new BadRequestException(
                            "Unauthorized: You are not the assigned level-2 approver for this leave.");
                }
            }
        }
    }

    private void recordLevelDecision(LeaveApplication leave, Employee approver,
                                     ApprovalLevel level, LeaveStatus decision) {
        switch (level) {
            case FIRST_APPROVER -> {
                leave.setFirstApproverDecision(decision);
                leave.setFirstApproverDecidedAt(LocalDateTime.now());
            }
            case SECOND_APPROVER -> {
                leave.setManagerDecision(decision);
                leave.setManagerDecidedAt(LocalDateTime.now());
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

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void notifySecondApprover(LeaveApplication leave, Employee firstApprover) {
        if (leave.getSecondApproverId() == null) return;
        employeeRepository.findById(leave.getSecondApproverId()).ifPresent(mgr -> {
            String empName = employeeRepository.findById(leave.getEmployeeId())
                    .map(Employee::getName).orElse("Employee");
            notificationService.createNotification(
                    mgr.getId(), firstApprover.getEmail(), mgr.getEmail(),
                    EventType.LEAVE_APPLIED, mgr.getRole(), Channel.EMAIL,
                    empName + "'s " + leave.getLeaveType().name() + " leave ("
                            + leave.getStartDate() + " to " + leave.getEndDate()
                            + ") approved at level 1. Requires your final approval.");
        });
    }

    private void notifyEmployee(LeaveApplication leave, Employee approver,
                                LeaveStatus decision, String comments) {
        employeeRepository.findById(leave.getEmployeeId()).ifPresent(emp -> {
            String context = switch (decision) {
                case APPROVED -> "Your " + leave.getLeaveType().name() + " leave from "
                        + leave.getStartDate() + " to " + leave.getEndDate()
                        + " has been fully approved.";
                case REJECTED -> "Your " + leave.getLeaveType().name() + " leave from "
                        + leave.getStartDate() + " to " + leave.getEndDate()
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
                        message + " (Leave: " + leave.getStartDate()
                                + " to " + leave.getEndDate() + ")")
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private List<LeaveApplicationWithAttachmentsDto> convertToDto(List<LeaveApplication> leaves) {
        if (leaves.isEmpty()) return List.of();
        List<Long> leaveIds = leaves.stream()
                .map(LeaveApplication::getId).collect(Collectors.toList());
        Map<Long, List<LeaveAttachment>> byLeaveId =
                leaveAttachmentRepository.findByLeaveApplicationIdIn(leaveIds)
                        .stream()
                        .collect(Collectors.groupingBy(LeaveAttachment::getLeaveApplicationId));
        return leaves.stream()
                .map(l -> new LeaveApplicationWithAttachmentsDto(
                        l, byLeaveId.getOrDefault(l.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private <T> Page<T> toPageDto(List<T> list, Pageable pageable) {
        int start   = (int) pageable.getOffset();
        int end     = Math.min(start + pageable.getPageSize(), list.size());
        List<T> content = start > list.size() ? List.of() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    private <T> Page<T> toPage(List<T> list, Pageable pageable) {
        return toPageDto(list, pageable);
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
}