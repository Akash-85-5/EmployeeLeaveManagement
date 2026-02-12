package com.example.notificationservice.service;

import com.example.notificationservice.dto.BulkLeaveDecisionRequest;
import com.example.notificationservice.dto.LeaveDecisionRequest;
import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.entity.LeaveApproval;
import com.example.notificationservice.enums.*;
import com.example.notificationservice.repository.LeaveApprovalRepository;
import com.example.notificationservice.repository.EmployeeRepository;
import com.example.notificationservice.repository.LeaveApplicationRepository;
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

    // ✅ FIXED METHOD (Directly using leave.managerId)
    public List<LeaveApplication> getPendingLeavesForManager(Long managerId) {
        return leaveApplicationRepository
                .findByManagerIdAndStatus(managerId, LeaveStatus.PENDING);
    }

    public List<LeaveApplication> getEscalatedLeaves() {
        return leaveApplicationRepository.findByEscalatedTrue();
    }

    // ✅ HR Decision
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
        approval.setManagerId(request.getManagerId());
        approval.setDecision(request.getDecision());
        approval.setComments(request.getComments());
        approval.setDecidedAt(LocalDateTime.now());

        leaveApprovalRepository.save(approval);

        String context;
        if (request.getDecision() == LeaveStatus.APPROVED) {
            context = "Your leave from " + leave.getStartDate() + " to "
                    + leave.getEndDate() + " has been approved";
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
