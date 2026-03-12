package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.PolicyConstants;
import com.example.employeeLeaveApplication.dto.BulkLeaveDecisionRequest;
import com.example.employeeLeaveApplication.dto.LeaveDecisionRequest;
import com.example.employeeLeaveApplication.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveApproval;
import com.example.employeeLeaveApplication.enums.*;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveApprovalRepository;
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

    // ═══════════════════════════════════════════════════════════════
    // DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════

    private final EmployeeRepository employeeRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final NotificationService notificationService;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LossOfPayService lossOfPayService;
    private final CompOffService compOffService;
    private final CarryForwardBalanceRepository carryForwardBalanceRepository;

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public LeaveApprovalService(EmployeeRepository employeeRepository,
                                LeaveApplicationRepository leaveApplicationRepository,
                                NotificationService notificationService,
                                LeaveApprovalRepository leaveApprovalRepository,
                                LeaveBalanceService leaveBalanceService,
                                LossOfPayService lossOfPayService,
                                CompOffService compOffService,
                                CarryForwardBalanceRepository carryForwardBalanceRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.notificationService = notificationService;
        this.leaveApprovalRepository = leaveApprovalRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.lossOfPayService = lossOfPayService;
        this.compOffService = compOffService;
        this.carryForwardBalanceRepository = carryForwardBalanceRepository;
    }

    // ═══════════════════════════════════════════════════════════════
    // PENDING LEAVES — per role
    // ═══════════════════════════════════════════════════════════════

    /** Team Leader: sees leaves pending at TL level where they are the assigned TL */
    public Page<LeaveApplication> getPendingLeavesForTeamLeader(Long teamLeaderId, Pageable pageable) {
        List<LeaveApplication> all = leaveApplicationRepository
                .findByTeamLeaderIdAndStatusAndCurrentApprovalLevel(
                        teamLeaderId, LeaveStatus.PENDING, ApprovalLevel.TEAM_LEADER);
        return toPage(all, pageable);
    }

    /** Manager: sees leaves pending at MANAGER level where they are the assigned manager */
    public Page<LeaveApplication> getPendingLeavesForManager(Long managerId, Pageable pageable) {
        List<LeaveApplication> all = leaveApplicationRepository
                .findByManagerIdAndStatusAndCurrentApprovalLevel(
                        managerId, LeaveStatus.PENDING, ApprovalLevel.MANAGER);
        return toPage(all, pageable);
    }

    /** HR: sees all leaves pending at HR level */
    public Page<LeaveApplication> getPendingLeavesForHr(Pageable pageable) {
        List<LeaveApplication> all = leaveApplicationRepository
                .findByStatusAndCurrentApprovalLevel(LeaveStatus.PENDING, ApprovalLevel.HR);
        return toPage(all, pageable);
    }

    // ═══════════════════════════════════════════════════════════════
    // CORE DECISION — handles TL / Manager / HR transparently
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void decideLeave(LeaveDecisionRequest request) {
        validateDecision(request.getDecision());

        LeaveApplication leave = leaveApplicationRepository.findById(request.getLeaveId())
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Leave is already processed with status: " + leave.getStatus());
        }

        Employee approver = employeeRepository.findById(request.getApproverId())
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        // Determine and validate the approver's role for the current stage
        ApprovalLevel currentLevel = leave.getCurrentApprovalLevel();
        validateApproverForLevel(leave, approver, currentLevel);

        // Record the decision at this level
        recordLevelDecision(leave, approver, currentLevel, request.getDecision(), request.getComments());

        // Save the approval audit record
        saveApprovalRecord(leave.getId(), approver, currentLevel, request.getDecision(), request.getComments());

        // If rejected at any level → final rejection
        if (request.getDecision() == LeaveStatus.REJECTED) {
            finalizeLeave(leave, LeaveStatus.REJECTED, approver);
        } else if (request.getDecision() == LeaveStatus.MEETING_REQUIRED) {
            // Keep PENDING, just notify employee
            leaveApplicationRepository.save(leave);
            notifyEmployee(leave, approver, request.getDecision(), request.getComments());
        } else {
            // APPROVED at this level — advance or finalize
            advanceOrFinalize(leave, approver);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVENIENCE WRAPPERS
    // ═══════════════════════════════════════════════════════════════

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
    public String bulkDecision(BulkLeaveDecisionRequest request, boolean isHr) {
        validateDecision(request.getDecision());

        List<LeaveApplication> leaves = leaveApplicationRepository.findAllById(request.getLeaveIds());
        if (leaves.isEmpty()) throw new RuntimeException("No leaves found");

        Employee approver = employeeRepository.findById(request.getApproverId())
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        for (LeaveApplication leave : leaves) {
            if (leave.getStatus() != LeaveStatus.PENDING) continue;

            ApprovalLevel currentLevel = leave.getCurrentApprovalLevel();

            // HR bulk: only act on HR-level leaves
            if (isHr && currentLevel != ApprovalLevel.HR) continue;

            try {
                validateApproverForLevel(leave, approver, currentLevel);
            } catch (BadRequestException e) {
                continue; // skip leaves this approver can't act on
            }

            recordLevelDecision(leave, approver, currentLevel, request.getDecision(), "Bulk decision");
            saveApprovalRecord(leave.getId(), approver, currentLevel, request.getDecision(), "Bulk decision");

            if (request.getDecision() == LeaveStatus.REJECTED) {
                finalizeLeave(leave, LeaveStatus.REJECTED, approver);
            } else if (request.getDecision() == LeaveStatus.APPROVED) {
                advanceOrFinalize(leave, approver);
            }
        }
        return "Bulk decision completed successfully";
    }

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE LOP CONFIRMATION
    // POST /api/leave-approvals/{leaveId}/lop-confirmation
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void handleLopConfirmation(Long leaveId,
                                      Long empId,
                                      boolean confirmed) {

        LeaveApplication leave = leaveApplicationRepository
                .findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (!leave.getEmployeeId().equals(empId)) {
            throw new BadRequestException("Unauthorized");
        }

        if (leave.getStatus() != LeaveStatus.PENDING_LOP_CONFIRMATION) {
            throw new BadRequestException(
                    "Leave is not awaiting LOP confirmation. " +
                            "Current status: " + leave.getStatus());
        }

        Employee emp = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        double lopDays    = leave.getPendingLopDays();
        int year          = leave.getStartDate().getYear();
        int month         = leave.getStartDate().getMonthValue();
        double lopPercent = lopDays * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY;

        if (confirmed) {
            // ✅ Employee accepted LOP — apply and approve
            lossOfPayService.applyLossOfPay(empId, year, month, lopDays);

            leave.setStatus(LeaveStatus.APPROVED);
            leave.setLossOfPayApplied(leave.getLossOfPayApplied() + lopPercent);
            leave.setPendingLopDays(null);
            leaveApplicationRepository.save(leave);

            notificationService.createNotification(
                    empId,
                    "system",
                    emp.getEmail(),
                    EventType.LEAVE_APPROVED,
                    emp.getRole(),
                    Channel.EMAIL,
                    "✅ Your leave from " + leave.getStartDate()
                            + " to " + leave.getEndDate()
                            + " is confirmed.\n"
                            + "Loss of Pay applied: "
                            + String.format("%.2f%%", lopPercent)
                            + " will be deducted from your salary."
            );

        } else {
            // ❌ Employee chose to cancel — no LOP
            leave.setStatus(LeaveStatus.CANCELLED);
            leave.setPendingLopDays(null);
            leaveApplicationRepository.save(leave);

            notificationService.createNotification(
                    empId,
                    "system",
                    emp.getEmail(),
                    EventType.LEAVE_CANCELLED,
                    emp.getRole(),
                    Channel.EMAIL,
                    "❌ Your leave from " + leave.getStartDate()
                            + " to " + leave.getEndDate()
                            + " has been cancelled as per your request.\n"
                            + "No Loss of Pay has been applied."
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HISTORY & QUERIES
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
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void validateApproverForLevel(LeaveApplication leave,
                                          Employee approver, ApprovalLevel level) {
        switch (level) {
            case TEAM_LEADER -> {
                if (leave.getTeamLeaderId() == null ||
                        !approver.getId().equals(leave.getTeamLeaderId())) {
                    throw new BadRequestException(
                            "Unauthorized: Only the assigned Team Leader can approve.");
                }
                if (approver.getRole() != Role.TEAM_LEADER) {
                    throw new BadRequestException(
                            "Approver does not have TEAM_LEADER role.");
                }
            }
            case MANAGER -> {
                if (leave.getManagerId() == null ||
                        !approver.getId().equals(leave.getManagerId())) {
                    throw new BadRequestException(
                            "Unauthorized: Only the assigned Manager can approve.");
                }
                if (approver.getRole() != Role.MANAGER) {
                    throw new BadRequestException(
                            "Approver does not have MANAGER role.");
                }
            }
            case HR -> {
                if (approver.getRole() != Role.HR) {
                    throw new BadRequestException(
                            "Only HR can approve at this stage.");
                }
            }
        }
    }

    private void recordLevelDecision(LeaveApplication leave, Employee approver,
                                     ApprovalLevel level, LeaveStatus decision, String comments) {
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

    private void advanceOrFinalize(LeaveApplication leave, Employee currentApprover) {
        int required = leave.getRequiredApprovalLevels();
        ApprovalLevel current = leave.getCurrentApprovalLevel();

        if (current == ApprovalLevel.TEAM_LEADER) {
            if (required >= 2) {
                // Advance to Manager
                leave.setCurrentApprovalLevel(ApprovalLevel.MANAGER);
                leaveApplicationRepository.save(leave);
                notifyManager(leave, currentApprover);
                notifyEmployeeProgress(leave, currentApprover,
                        "Your leave has been approved by Team Leader. " +
                                "Pending Manager approval.");
            } else {
                // TL only → done
                finalizeLeave(leave, LeaveStatus.APPROVED, currentApprover);
            }

        } else if (current == ApprovalLevel.MANAGER) {
            if (required >= 3) {
                // Advance to HR
                leave.setCurrentApprovalLevel(ApprovalLevel.HR);
                leave.setEscalated(true);
                leave.setEscalatedAt(LocalDateTime.now());
                leaveApplicationRepository.save(leave);
                notifyHr(leave, currentApprover);
                notifyEmployeeProgress(leave, currentApprover,
                        "Your leave has been approved by Manager. " +
                                "Pending HR approval.");
            } else {
                // Manager is final approver (Employee 2-6 days OR Team Leader < 7 days)
                finalizeLeave(leave, LeaveStatus.APPROVED, currentApprover);
            }

        } else if (current == ApprovalLevel.HR) {
            // HR approved → fully done
            finalizeLeave(leave, LeaveStatus.APPROVED, currentApprover);
        }
    }
    /**
     * Called ONLY when the LAST required approver signs off.
     *
     * ✅ FIX APPLIED HERE:
     * BEFORE → countApprovedInMonth()      counts applications (wrong)
     * AFTER  → getTotalApprovedDaysInMonth() counts actual days (correct)
     *
     * Why: Monthly limit = 2 DAYS not 2 applications
     * Example:
     *   1 leave of 3 days = 1 application but 3 days
     *   OLD: 1 > 2? NO → missed LOP ❌
     *   NEW: 3 > 2? YES → LOP triggered ✅
     */
    private void finalizeLeave(LeaveApplication leave,
                               LeaveStatus finalStatus,
                               Employee finalApprover) {

        leave.setApprovedBy(finalApprover.getId());
        leave.setApprovedRole(finalApprover.getRole());
        leave.setApprovedAt(LocalDateTime.now());
        leave.setEscalated(false);

        Employee employee = employeeRepository.findById(leave.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (finalStatus == LeaveStatus.APPROVED) {
            leaveBalanceService.applyApprovedLeave(leave);

            int year   = leave.getStartDate().getYear();
            int month  = leave.getStartDate().getMonthValue();
            Long empId = leave.getEmployeeId();

            // ✅ FIXED: Use total DAYS not application count
            double totalApprovedDays = leaveApplicationRepository
                    .getTotalApprovedDaysInMonth(empId, year, month);

            // Step 3: Monthly limit exceeded?
            if (totalApprovedDays > PolicyConstants.MONTHLY_LIMIT) {

                // Step 4: Calculate excess days
                double excessDays = totalApprovedDays - PolicyConstants.MONTHLY_LIMIT;

                // Step 5: Check CarryForward balance
                CarryForwardBalance cf = carryForwardBalanceRepository
                        .findByEmployeeIdAndYear(empId, year)
                        .orElse(null);

                double cfBalance = (cf != null) ? cf.getRemaining() : 0.0;

                if (cfBalance >= excessDays) {
                    // ✅ Scenario A: CF covers all — NO LOP
                    cf.setRemaining(cfBalance - excessDays);
                    leave.setCarryForwardUsed(leave.getCarryForwardUsed() + excessDays);
                    carryForwardBalanceRepository.save(cf);
                    leaveApplicationRepository.save(leave);

                    sendCfDeductionMessage(
                            leave, finalApprover,
                            totalApprovedDays, cfBalance, excessDays);

                } else {
                    // Step 6: CF partial or zero
                    double lopDays = excessDays;

                    if (cf != null && cfBalance > 0) {
                        // ⚠️ Scenario C: use remaining CF first
                        lopDays = excessDays - cfBalance;
                        leave.setCarryForwardUsed(leave.getCarryForwardUsed() + cfBalance);
                        cf.setRemaining(0.0);
                        carryForwardBalanceRepository.save(cf);
                        leaveApplicationRepository.save(leave);
                    }

                    handleLopConfirmation(leave.getId(), empId, true);
                    leave.setStatus(finalStatus);
                    leave.setPendingLopDays(lopDays);
                    leaveApplicationRepository.save(leave);

                    sendLopConfirmationMessage(
                            leave, finalApprover,
                            totalApprovedDays, cfBalance, lopDays);

                    return; // stop here — wait for employee response
                }
            }
        }

        notifyEmployee(leave, finalApprover, finalStatus, null);
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void sendCfDeductionMessage(LeaveApplication leave,
                                        Employee approver,
                                        double totalApprovedDays,
                                        double cfBalance,
                                        double excessDays) {

        Employee emp = employeeRepository
                .findById(leave.getEmployeeId()).orElse(null);
        if (emp == null) return;

        String message = "✅ Leave Approved — Carry Forward Deducted\n\n"
                + "Monthly Leave Limit     : " + PolicyConstants.MONTHLY_LIMIT + " days\n"
                + "Total Days Used         : " + String.format("%.1f", totalApprovedDays) + " days\n"
                + "Excess Days             : " + String.format("%.1f", excessDays) + " days\n"
                + "Carry Forward Used      : " + String.format("%.1f", excessDays) + " days\n"
                + "Carry Forward Remaining : " + String.format("%.1f", cfBalance - excessDays) + " days\n\n"
                + "No Loss of Pay applied ✅";

        notificationService.createNotification(
                emp.getId(),
                approver.getEmail(),
                emp.getEmail(),
                EventType.LEAVE_APPROVED,
                emp.getRole(),
                Channel.EMAIL,
                message
        );
    }

    private void sendLopConfirmationMessage(LeaveApplication leave,
                                            Employee approver,
                                            double totalApprovedDays,
                                            double cfBalance,
                                            double lopDays) {

        Employee emp = employeeRepository
                .findById(leave.getEmployeeId()).orElse(null);
        if (emp == null) return;

        double lopPercent = lopDays * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY;

        String message = "⚠️ Action Required — Loss of Pay Notice\n\n"
                + "Your leave has been approved by management.\n"
                + "However your monthly leave limit has been exceeded:\n\n"
                + "Monthly Leave Limit    : " + PolicyConstants.MONTHLY_LIMIT + " days\n"
                + "Total Days Used        : " + String.format("%.1f", totalApprovedDays) + " days\n"
                + "Carry Forward Balance  : " + String.format("%.1f", cfBalance) + " days\n"
                + "Excess Days (uncovered): " + String.format("%.1f", lopDays) + " days\n\n"
                + "💸 Loss of Pay: " + String.format("%.2f%%", lopPercent)
                + " deducted from your salary\n\n";

        notificationService.createNotification(
                emp.getId(),
                approver.getEmail(),
                emp.getEmail(),
                EventType.LOP_CONFIRMATION_REQUIRED,
                emp.getRole(),
                Channel.EMAIL,
                message
        );
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
        Employee manager = employeeRepository.findById(leave.getManagerId()).orElse(null);
        if (manager == null) return;

        Employee employee = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
        String empName = employee != null ? employee.getName() : "Employee";

        notificationService.createNotification(
                manager.getId(),
                teamLeader.getEmail(),
                manager.getEmail(),
                EventType.LEAVE_APPLIED,
                manager.getRole(),
                Channel.EMAIL,
                empName + "'s leave (" + leave.getStartDate() + " to " + leave.getEndDate()
                        + ") has been approved by Team Leader and now requires your approval."
        );
    }

    private void notifyHr(LeaveApplication leave, Employee manager) {
        List<Employee> hrList = employeeRepository.findAllHr();
        if (hrList.isEmpty()) return;

        Employee employee = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
        String empName = employee != null ? employee.getName() : "Employee";

        for (Employee hr : hrList) {
            notificationService.createNotification(
                    hr.getId(),
                    manager.getEmail(),
                    hr.getEmail(),
                    EventType.LEAVE_APPLIED,
                    hr.getRole(),
                    Channel.EMAIL,
                    empName + "'s leave (" + leave.getStartDate() + " to " + leave.getEndDate()
                            + ") has been approved by Manager and now requires HR approval."
            );
        }
    }

    private void notifyEmployee(LeaveApplication leave, Employee approver,
                                LeaveStatus decision, String comments) {
        Employee employee = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
        if (employee == null) return;

        String context;
        if (decision == LeaveStatus.APPROVED) {
            context = "Your leave from " + leave.getStartDate() + " to " + leave.getEndDate()
                    + " has been fully approved.";
        } else if (decision == LeaveStatus.REJECTED) {
            context = "Your leave from " + leave.getStartDate() + " to " + leave.getEndDate()
                    + " has been rejected. Reason: " + (comments != null ? comments : "N/A");
        } else {
            context = "A meeting is required regarding your leave from " + leave.getStartDate()
                    + " to " + leave.getEndDate() + ".";
        }

        notificationService.createNotification(
                employee.getId(),
                approver.getEmail(),
                employee.getEmail(),
                mapEventType(decision),
                employee.getRole(),
                Channel.EMAIL,
                context
        );
    }

    private void notifyEmployeeProgress(LeaveApplication leave, Employee approver, String message) {
        Employee employee = employeeRepository.findById(leave.getEmployeeId()).orElse(null);
        if (employee == null) return;

        notificationService.createNotification(
                employee.getId(),
                approver.getEmail(),
                employee.getEmail(),
                EventType.LEAVE_IN_PROGRESS,
                employee.getRole(),
                Channel.EMAIL,
                message + " (Leave: " + leave.getStartDate() + " to " + leave.getEndDate() + ")"
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void validateDecision(LeaveStatus decision) {
        if (decision != LeaveStatus.APPROVED
                && decision != LeaveStatus.REJECTED
                && decision != LeaveStatus.MEETING_REQUIRED) {
            throw new BadRequestException("Invalid decision: " + decision);
        }
    }

    private EventType mapEventType(LeaveStatus status) {
        return switch (status) {
            case APPROVED -> EventType.LEAVE_APPROVED;
            case REJECTED -> EventType.LEAVE_REJECTED;
            case MEETING_REQUIRED -> EventType.MEETING_REQUIRED;
            default -> EventType.LEAVE_APPLIED;
        };
    }

    private <T> Page<T> toPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<T> pageContent = (start > list.size()) ? List.of() : list.subList(start, end);
        return new PageImpl<>(pageContent, pageable, list.size());
    }
}