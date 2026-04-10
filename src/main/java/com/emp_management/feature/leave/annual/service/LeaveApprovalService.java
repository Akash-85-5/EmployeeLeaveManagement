package com.emp_management.feature.leave.annual.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.dto.LeaveApplicationWithAttachmentsDto;
import com.emp_management.feature.leave.annual.dto.LeaveDecisionRequest;
import com.emp_management.feature.leave.annual.dto.LeaveRemarkDto;
import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.entity.LeaveApproval;
import com.emp_management.feature.leave.annual.entity.LeaveAttachment;
import com.emp_management.feature.leave.annual.mapper.LeaveApplicationMapper;
import com.emp_management.feature.leave.annual.repository.LeaveApplicationRepository;
import com.emp_management.feature.leave.annual.repository.LeaveApprovalRepository;
import com.emp_management.feature.leave.annual.repository.LeaveAttachmentRepository;
import com.emp_management.feature.notification.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveApprovalService {

    private final EmployeeRepository employeeRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final NotificationService           notificationService;
    private final LeaveApprovalRepository       leaveApprovalRepository;
    private final LeaveApplicationService       leaveApplicationService;
    private final LeaveAttachmentRepository leaveAttachmentRepository;

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


    public Page<LeaveApplicationWithAttachmentsDto> getPendingLeavesForManagerWithAttachments(
            String approverId, Pageable pageable) {
        List<LeaveApplication> all = leaveApplicationRepository
                .findByCurrentApproverIdAndStatus(
                        approverId, RequestStatus.PENDING);
        return toPageDto(convertToDto(all), pageable);
    }

    public LeaveApplicationWithAttachmentsDto getLeaveApplicationWithAttachments(Long leaveId) {
        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new EntityNotFoundException("Leave application not found"));

        List<LeaveAttachment> attachments = leaveAttachmentRepository.findByLeaveApplicationId(leaveId);
        // Fetch remarks/approvals for this leave
        List<LeaveApproval> approvals = leaveApprovalRepository.findByLeaveIdOrderByDecidedAtDesc(leaveId, Pageable.unpaged()).getContent();

        LeaveApplicationWithAttachmentsDto dto = new LeaveApplicationWithAttachmentsDto(
                LeaveApplicationMapper.toDTO(leave),
                attachments
        );
        dto.setRemarks(LeaveApplicationMapper.mapToRemarks(approvals));
        return dto;
    }

//    public LeaveApplicationWithAttachmentsDto getLeaveApplicationWithAttachments(Long leaveId) {
//        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
//                .orElseThrow(() -> new BadRequestException("Leave not found with ID: " + leaveId));
//        return new LeaveApplicationWithAttachmentsDto(
//                leave, leaveAttachmentRepository.findByLeaveApplicationId(leaveId));
//    }

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
        validateDecision(request.getDecision(), request.getComments());

        LeaveApplication leave = leaveApplicationRepository.findById(request.getLeaveId())
                .orElseThrow(() -> new EntityNotFoundException("Leave not found"));

        if (leave.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "Leave is already processed with status: " + leave.getStatus());
        }
        if (request.getComments() == null || request.getComments().trim().isEmpty()) {
            throw new BadRequestException("Remark is required for both approval and rejection");
        }

        Employee approver = employeeRepository.findByEmpId(request.getApproverId())
                .orElseThrow(() -> new EntityNotFoundException("Approver not found"));

        ApprovalLevel currentLevel = leave.getCurrentApprovalLevel();
        validateApproverForLevel(leave, approver, currentLevel);
        recordLevelDecision(leave, approver, currentLevel, request.getDecision());
        saveApprovalRecord(leave.getId(), approver, currentLevel,
                request.getDecision(), request.getComments());

        if (request.getDecision() == RequestStatus.REJECTED) {
            finalizeLeave(leave, RequestStatus.REJECTED, approver, request.getComments());
        }
        else {
            advanceOrFinalize(leave, approver, request.getComments());
        }
    }

    @Transactional
    public void approveLeave(Long leaveId, String approverId, String comments) {
        if (comments == null || comments.trim().isEmpty()) {
            throw new BadRequestException("Remark is required when approving the leave");
        }
        LeaveDecisionRequest req = new LeaveDecisionRequest();
        req.setLeaveId(leaveId);
        req.setApproverId(approverId);
        req.setDecision(RequestStatus.APPROVED);
        req.setComments(comments);
        decideLeave(req);
    }

    @Transactional
    public void rejectLeave(Long leaveId, String approverId, String comments) {
        if (comments == null || comments.trim().isEmpty()) {
            throw new BadRequestException("Remark is required when rejecting the leave");
        }
        LeaveDecisionRequest req = new LeaveDecisionRequest();
        req.setLeaveId(leaveId);
        req.setApproverId(approverId);
        req.setDecision(RequestStatus.REJECTED);
        req.setComments(comments);
        decideLeave(req);
    }

    // ═══════════════════════════════════════════════════════════════
    // BULK DECISION
    // ═══════════════════════════════════════════════════════════════

//    @Transactional
//    public String bulkDecision(BulkLeaveDecisionRequest request, boolean isSecondLevel) {
//        validateDecision(request.getDecision());
//
//        List<LeaveApplication> leaves =
//                leaveApplicationRepository.findAllById(request.getLeaveIds());
//        if (leaves.isEmpty()) throw new RuntimeException("No leaves found");
//
//        Employee approver = employeeRepository.findById(request.getApproverId())
//                .orElseThrow(() -> new RuntimeException("Approver not found"));
//
//        for (LeaveApplication leave : leaves) {
//            if (leave.getStatus() != LeaveStatus.PENDING) continue;
//            ApprovalLevel currentLevel = leave.getCurrentApprovalLevel();
//            if (isSecondLevel && currentLevel != ApprovalLevel.SECOND_APPROVER) continue;
//
//            try {
//                validateApproverForLevel(leave, approver, currentLevel);
//            } catch (BadRequestException e) {
//                continue;
//            }
//
//            recordLevelDecision(leave, approver, currentLevel, request.getDecision());
//            saveApprovalRecord(leave.getId(), approver, currentLevel,
//                    request.getDecision(), "Bulk decision");
//
//            if (request.getDecision() == LeaveStatus.REJECTED) {
//                finalizeLeave(leave, LeaveStatus.REJECTED, approver);
//            } else if (request.getDecision() == LeaveStatus.APPROVED) {
//                advanceOrFinalize(leave, approver);
//            }
//        }
//        return "Bulk decision completed successfully";
//    }

    // ═══════════════════════════════════════════════════════════════
    // HISTORY
    // ═══════════════════════════════════════════════════════════════

    public Page<LeaveApproval> getApprovalHistory(Long leaveId, Pageable pageable) {
        return leaveApprovalRepository.findByLeaveIdOrderByDecidedAtDesc(leaveId, pageable);
    }

    public Page<LeaveApproval> getManagerDecisions(String approverId, Pageable pageable) {
        return leaveApprovalRepository.findByApproverIdOrderByDecidedAtDesc(approverId, pageable);
    }

    public List<LeaveApplication> getEscalatedLeavesForHr() {
        return leaveApplicationRepository.findByEscalatedTrueAndStatus(RequestStatus.PENDING);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE: ADVANCE OR FINALIZE
    //
    // Level TEAM_LEADER approved + required==2 → advance to MANAGER
    // Level TEAM_LEADER approved + required==1 → finalize
    // Level MANAGER approved → always finalize (max 2 levels)
    // ═══════════════════════════════════════════════════════════════

    private void advanceOrFinalize(LeaveApplication leave, Employee currentApprover, String comments) {
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
                finalizeLeave(leave, RequestStatus.APPROVED, currentApprover,comments);
            }
        } else {
            // MANAGER or HR → final level
            finalizeLeave(leave, RequestStatus.APPROVED, currentApprover,comments);
        }
    }

    private void finalizeLeave(LeaveApplication leave,
                               RequestStatus finalStatus,
                               Employee finalApprover,
                               String reason) {
        leave.setStatus(finalStatus);
        if (finalStatus == RequestStatus.REJECTED){
            leave.setRejectionReason(reason);
        }
        leave.setApprovedBy(finalApprover.getEmpId());
        leave.setApprovedRole(finalApprover.getRole().getRoleName());
        leave.setApprovedAt(LocalDateTime.now());
        leave.setEscalated(false);
        if (finalStatus == RequestStatus.APPROVED) {
            leaveApplicationService.applyBalanceDeduction(leave);
        }

        leaveApplicationRepository.save(leave);
        notifyEmployee(leave, finalApprover, finalStatus);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE: VALIDATE APPROVER
    // ═══════════════════════════════════════════════════════════════

    private void validateApproverForLevel(LeaveApplication leave,
                                          Employee approver, ApprovalLevel level) {
        switch (level) {
            case FIRST_APPROVER -> {
                if (leave.getFirstApproverId() == null
                        || !approver.getEmpId().equals(leave.getFirstApproverId())) {
                    throw new BadRequestException(
                            "Unauthorized: You are not the assigned level-1 approver for this leave.");
                }
            }
            case SECOND_APPROVER -> {
                if (leave.getSecondApproverId() == null
                        || !approver.getEmpId().equals(leave.getSecondApproverId())) {
                    throw new BadRequestException(
                            "Unauthorized: You are not the assigned level-2 approver for this leave.");
                }
            }
        }
    }

    private void recordLevelDecision(LeaveApplication leave, Employee approver,
                                     ApprovalLevel level, RequestStatus decision) {
        switch (level) {
            case FIRST_APPROVER -> {
                leave.setFirstApproverDecision(decision);
                leave.setFirstApproverDecidedAt(LocalDateTime.now());
            }
            case SECOND_APPROVER -> {
                leave.setSecondApproverDecision(decision);
                leave.setSecondApproverDecidedAt(LocalDateTime.now());
            }
        }
    }

    private void saveApprovalRecord(Long leaveId, Employee approver, ApprovalLevel level,
                                    RequestStatus decision, String comments) {
        LeaveApproval record = new LeaveApproval();
        record.setLeaveId(leaveId);
        record.setApproverId(approver.getEmpId());
        record.setApprovalLevel(level);
        record.setApproverRole(approver.getRole().getRoleName());
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
        employeeRepository.findByEmpId(leave.getSecondApproverId()).ifPresent(mgr -> {
            String empName = employeeRepository.findByEmpId(leave.getEmployee().getEmpId())
                    .map(Employee::getName).orElse("Employee");
            notificationService.createNotification(
                    mgr.getEmpId(), firstApprover.getEmail(), mgr.getEmail(),
                    EventType.LEAVE_APPLIED, Channel.EMAIL,
                    empName + "'s " + leave.getLeaveType().getLeaveType() + " leave ("
                            + leave.getStartDate() + " to " + leave.getEndDate()
                            + ") approved at level 1. Requires your final approval.");
        });
    }

    private void notifyEmployee(LeaveApplication leave, Employee approver,
                                RequestStatus decision) {
        employeeRepository.findByEmpId(leave.getEmployee().getEmpId()).ifPresent(emp -> {
            String context = switch (decision) {
                case APPROVED -> "Your " + leave.getLeaveType().getLeaveType() + " leave from "
                        + leave.getStartDate() + " to " + leave.getEndDate()
                        + " has been fully approved.";
                case REJECTED -> "Your " + leave.getLeaveType().getLeaveType() + " leave from "
                        + leave.getStartDate() + " to " + leave.getEndDate()
                        + " has been rejected. Reason: "
                        + (leave.getRejectionReason() != null ? leave.getRejectionReason() : "");
                default -> "A meeting is required regarding your leave from "
                        + leave.getStartDate() + " to " + leave.getEndDate() + ".";
            };
            notificationService.createNotification(
                    emp.getEmpId(), approver.getEmail(), emp.getEmail(),
                    switch (decision) {
                        case APPROVED -> EventType.LEAVE_APPROVED;
                        case REJECTED -> EventType.LEAVE_REJECTED;
                        default -> EventType.LEAVE_IN_PROGRESS;
                    },  Channel.EMAIL, context);
        });
    }

    private void notifyEmployeeProgress(LeaveApplication leave, Employee approver, String message) {
        employeeRepository.findByEmpId(leave.getEmployee().getEmpId()).ifPresent(emp ->
                notificationService.createNotification(
                        emp.getEmpId(), approver.getEmail(), emp.getEmail(),
                        EventType.LEAVE_IN_PROGRESS, Channel.EMAIL,
                        message + " (Leave: " + leave.getStartDate()
                                + " to " + leave.getEndDate() + ")")
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private List<LeaveApplicationWithAttachmentsDto> convertToDto(List<LeaveApplication> leaves) {
        if (leaves == null || leaves.isEmpty()) return List.of();

        List<Long> leaveIds = leaves.stream()
                .map(LeaveApplication::getId)
                .collect(Collectors.toList());

        // Batch fetch attachments
        Map<Long, List<LeaveAttachment>> attachmentsByLeaveId =
                leaveAttachmentRepository.findByLeaveApplicationIdIn(leaveIds)
                        .stream()
                        .filter(a -> a.getLeaveApplicationId() != null)
                        .collect(Collectors.groupingBy(LeaveAttachment::getLeaveApplicationId));

        // Batch fetch remarks (Approvals)
        // This satisfies: Emp sees both, and Managers see each other's remarks
        Map<Long, List<LeaveRemarkDto>> remarksByLeaveId =
                leaveApprovalRepository.findByLeaveIdInOrderByDecidedAtAsc(leaveIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                LeaveApproval::getLeaveId,
                                Collectors.mapping(LeaveRemarkDto::fromApproval, Collectors.toList())
                        ));

        return leaves.stream()
                .map(l -> {
                    LeaveApplicationWithAttachmentsDto dto = new LeaveApplicationWithAttachmentsDto(
                            LeaveApplicationMapper.toDTO(l),
                            attachmentsByLeaveId.getOrDefault(l.getId(), List.of())
                    );
                    // Attach the remarks to the DTO
                    dto.setRemarks(remarksByLeaveId.getOrDefault(l.getId(), List.of()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private <T> Page<T> toPageDto(List<T> list, Pageable pageable) {
        if (list == null) return new PageImpl<>(List.of(), pageable, 0);

        int start = (int) pageable.getOffset();
        // Safety check: if start is beyond list size, return empty page
        if (start >= list.size()) {
            return new PageImpl<>(List.of(), pageable, list.size());
        }

        int end = Math.min(start + pageable.getPageSize(), list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    private void validateDecision(RequestStatus decision, String comments) {
        if (decision != RequestStatus.APPROVED && decision != RequestStatus.REJECTED) {
            throw new BadRequestException("Invalid decision: " + decision);
        }
        // Final safety check: ensuring remarks are present at the service level
        if (comments == null || comments.isBlank()) {
            throw new BadRequestException("Remarks are mandatory for " + decision.toString().toLowerCase());
        }
    }}