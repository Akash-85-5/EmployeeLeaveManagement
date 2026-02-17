package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.BulkLeaveDecisionRequest;
import com.example.employeeLeaveApplication.dto.LeaveDecisionRequest;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveApproval;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.LeaveApprovalRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;



@Service
public class LeaveApprovalService {

    private final EmployeeRepository employeeRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final NotificationService notificationService;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LossOfPayService lossOfPayService;


    public LeaveApprovalService(EmployeeRepository employeeRepository,
                                LeaveApplicationRepository leaveApplicationRepository,
                                NotificationService notificationService,
                                LeaveApprovalRepository leaveApprovalRepository,
                                LeaveBalanceService leaveBalanceService,
                                LossOfPayService lossOfPayService) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.leaveApprovalRepository = leaveApprovalRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.lossOfPayService = lossOfPayService;
    }


    public Page<LeaveApplication> getPendingLeavesForManager(Long managerId, Pageable pageable) {
        List<Employee> employees = employeeRepository.findAll()
                .stream()
                .filter(e -> managerId.equals(e.getManagerId()))
                .toList();

        List<Long> employeeIds = employees.stream()
                .map(Employee::getId)
                .toList();

        List<LeaveApplication> allPending = leaveApplicationRepository.findByEmployeeIdInAndStatus(
                employeeIds, LeaveStatus.PENDING
        );

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allPending.size());

        List<LeaveApplication> pageContent = allPending.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allPending.size());
    }

    public List<LeaveApplication> getEscalatedLeaves() {
        return leaveApplicationRepository.findByEscalatedTrue();
    }

    @Transactional
    public String hrDecision(Long leaveId, LeaveStatus decision, Long hrId) {

        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (!Boolean.TRUE.equals(leave.getEscalated())) {
            throw new RuntimeException("This leave is not escalated to HR");
        }

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Leave already processed");
        }

        leave.setStatus(decision);
        leave.setEscalated(false);

        leaveApplicationRepository.save(leave);

        // ✅ Balance update must be here BEFORE return
        if (decision == LeaveStatus.APPROVED) {

            leaveBalanceService.applyApprovedLeave(leave);

            int approvedCount =
                    leaveApplicationRepository.countApprovedInMonth(
                            leave.getEmployeeId(),
                            leave.getStartDate().getYear(),
                            leave.getStartDate().getMonthValue()
                    );

            if (approvedCount > 2) {
                lossOfPayService.applyMonthlyLimitViolation(
                        leave.getEmployeeId(),
                        leave.getStartDate().getYear(),
                        leave.getStartDate().getMonthValue()
                );
            }
        }

        LeaveApproval approval = new LeaveApproval();
        approval.setLeaveId(leave.getId());
        approval.setManagerId(hrId);
        approval.setDecision(decision);
        approval.setDecidedAt(LocalDateTime.now());

        leaveApprovalRepository.save(approval);

        return "HR decision recorded successfully";
    }

    // ✅ Bulk Decision (Manager + HR)
    @Transactional
    public String bulkDecision(BulkLeaveDecisionRequest request, boolean isHr) {

        List<LeaveApplication> leaves =
                leaveApplicationRepository.findAllById(request.getLeaveIds());

        if (leaves.isEmpty()) {
            throw new RuntimeException("No leaves found");
        }

        for (LeaveApplication leave : leaves) {

            if (leave.getStatus() != LeaveStatus.PENDING) {
                continue;
            }

            if (isHr && !Boolean.TRUE.equals(leave.getEscalated())) {
                continue;
            }

            leave.setStatus(request.getDecision());

            if (isHr) {
                leave.setEscalated(false);
            }

            leaveApplicationRepository.save(leave);

            LeaveApproval approval = new LeaveApproval();
            approval.setLeaveId(leave.getId());
            approval.setManagerId(request.getApproverId());
            approval.setDecision(request.getDecision());
            approval.setComments("Bulk decision");
            approval.setDecidedAt(LocalDateTime.now());

            leaveApprovalRepository.save(approval);
        }

        return "Bulk decision completed successfully";
    }

    // ✅ Manager Decision
    @Transactional
    public void decideLeave(LeaveDecisionRequest request) {

        LeaveApplication leave = leaveApplicationRepository.findById(request.getLeaveId())
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Leave already processed");
        }

        Employee employee = employeeRepository.findById(leave.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!request.getManagerId().equals(employee.getManagerId())) {
            throw new RuntimeException("Unauthorized manager");
        }

        leave.setStatus(request.getDecision());
        leaveApplicationRepository.save(leave);

        if (request.getDecision() == LeaveStatus.APPROVED) {

            leaveBalanceService.applyApprovedLeave(leave);

            int approvedCount = leaveApplicationRepository.countApprovedInMonth(
                    leave.getEmployeeId(),
                    leave.getStartDate().getYear(),
                    leave.getStartDate().getMonthValue()
            );

            // 🔥 Apply LOP if monthly limit exceeded (>2)
            if (approvedCount > 2) {
                lossOfPayService.applyMonthlyLimitViolation(
                        leave.getEmployeeId(),
                        leave.getStartDate().getYear(),
                        leave.getStartDate().getMonthValue()
                );
            }
        }

        LeaveApproval approval = new LeaveApproval();
        approval.setLeaveId(leave.getId());
        approval.setManagerId(request.getManagerId());
        approval.setDecision(request.getDecision());
        approval.setComments(request.getComments());
        approval.setDecidedAt(LocalDateTime.now());

        leaveApprovalRepository.save(approval);

        String context;
        if (request.getDecision() == LeaveStatus.APPROVED) {
            context = "Your leave from " + leave.getStartDate() + " to " + leave.getEndDate() + " has been approved";
        } else if (request.getDecision() == LeaveStatus.MEETING_REQUIRED) {
            context = "Please attend a meeting regarding the leave request.";
        } else {
            context = "Your leave from " + leave.getStartDate() + " to "
                    + leave.getEndDate() + " has been "
                    + request.getDecision().name().toLowerCase()
                    + ". Reason: " + request.getComments();
        }

        notificationService.createNotification(
                employee.getId(),
                employee.getEmail(),
                mapEventType(request.getDecision()),
                employee.getRole(),
                Channel.EMAIL,
                context
        );
    }

    /**
     * Approve leave
     */
    @Transactional
    public void approveLeave(Long leaveId, Long managerId, String comments) {
        LeaveDecisionRequest request = new LeaveDecisionRequest();
        request.setLeaveId(leaveId);
        request.setManagerId(managerId);
        request.setDecision(LeaveStatus.APPROVED);
        request.setComments(comments);
        decideLeave(request);
    }

    /**
     * Reject leave (alternative endpoint)
     */
    @Transactional
    public void rejectLeave(Long leaveId, Long managerId, String comments) {
        LeaveDecisionRequest request = new LeaveDecisionRequest();
        request.setLeaveId(leaveId);
        request.setManagerId(managerId);
        request.setDecision(LeaveStatus.REJECTED);
        request.setComments(comments);
        decideLeave(request);
    }

    /**
     * Get approval history for a specific leave
     */
    public Page<LeaveApproval> getApprovalHistory(Long leaveId, Pageable pageable) {
        return leaveApprovalRepository.findByLeaveIdOrderByDecidedAtDesc(leaveId, pageable);
    }

    /**
     * Get manager's past decisions with pagination
     */
    public Page<LeaveApproval> getManagerDecisions(Long managerId, Pageable pageable) {
        return leaveApprovalRepository.findByManagerIdOrderByDecidedAtDesc(managerId, pageable);
    }

    /**
     * Get all pending leaves (admin view) with pagination
     */
    public List<LeaveApplication> getAllPendingLeaves(Pageable pageable) {
        return leaveApplicationRepository.findByStatus(LeaveStatus.PENDING);
    }

    private EventType mapEventType(LeaveStatus status) {
        return switch (status) {
            case APPROVED -> EventType.LEAVE_APPROVED;
            case REJECTED -> EventType.LEAVE_REJECTED;
            case MEETING_REQUIRED -> EventType.MEETING_REQUIRED;
            default -> EventType.LEAVE_APPLIED;
        };
    }

    public List<LeaveApplication> getEscalatedLeavesForHr() {
        return leaveApplicationRepository
                .findByEscalatedTrueAndStatus(LeaveStatus.PENDING);

    }


}




