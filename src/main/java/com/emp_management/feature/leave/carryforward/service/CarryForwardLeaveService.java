package com.emp_management.feature.leave.carryforward.service;


import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.carryforward.component.ApprovalMatrixService;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardLeaveApplicationResponse;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardLeaveRequest;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardLeaveApplication;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardLeaveApplicationRepository;
import com.emp_management.shared.enums.RequestStatus;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Carry-forward leave lifecycle with multi-level role-based approval.
 *
 * Approval matrix (driven by ApprovalMatrixService):
 *   EMPLOYEE    → TEAM_LEADER → MANAGER   (2 levels)
 *   TEAM_LEADER → MANAGER     → HR        (2 levels)
 *   MANAGER     → HR                      (1 level)
 *   HR          → CEO                     (1 level)
 *   ADMIN       → HR                      (1 level)
 *
 * Balance deduction happens ONLY when the final approval level is cleared.
 * Rejection at ANY level terminates the flow — no balance change.
 * Cancellation restores balance only if the application was fully APPROVED.
 */
@Service
public class CarryForwardLeaveService {

    private static final Logger log = LoggerFactory.getLogger(CarryForwardLeaveService.class);

    private final CarryForwardLeaveApplicationRepository cfLeaveAppRepository;
    private final CarryForwardBalanceRepository cfBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalMatrixService approvalMatrix;

    public CarryForwardLeaveService(CarryForwardLeaveApplicationRepository cfLeaveAppRepository, CarryForwardBalanceRepository cfBalanceRepository, EmployeeRepository employeeRepository, ApprovalMatrixService approvalMatrix) {
        this.cfLeaveAppRepository = cfLeaveAppRepository;
        this.cfBalanceRepository = cfBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.approvalMatrix = approvalMatrix;
    }
    Long MAX_CARRY_FORWARD = 10L;
// ═══════════════════════════════════════════════════════════════
    // APPLY
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public CarryForwardLeaveApplicationResponse applyLeave(CarryForwardLeaveRequest request) {

        String employeeId = request.getEmployeeId();
        int  year       = request.getStartDate().getYear(); // derive year from leave start date

        log.info("[CF-LEAVE] Apply: employee={}, {} → {}",
                employeeId, request.getStartDate(), request.getEndDate());

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employeeId));

        // ── Determine applicant's role ────────────────────────────
        String applicantRole = resolveRole(employee);

        // ── Calculate requested working days ──────────────────────
        double requestedDays = calculateWorkingDays(
                request.getStartDate(), request.getEndDate(), request.getIsHalfDay());

        // ── Validate balance ──────────────────────────────────────
        CarryForwardBalance balance = cfBalanceRepository
                .findByEmployee_EmpIdAndYear(employeeId, year)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No carry-forward balance for employee " + employeeId + " in " + year));

        double remaining = balance.getRemaining() != null ? balance.getRemaining() : 0.0;
        if (remaining <= 0) {
            throw new BadRequestException("No carry-forward balance remaining for year " + year);
        }
        if (requestedDays > remaining) {
            throw new BadRequestException(String.format(
                    "Requested %.1f days exceeds remaining carry-forward balance of %.1f days",
                    requestedDays, remaining));
        }

        // ── Build approval chain ──────────────────────────────────
        int    totalLevels   = approvalMatrix.getTotalLevels(applicantRole);
        String level1Role    = approvalMatrix.getLevel1Role(applicantRole);
        String level2Role    = approvalMatrix.getLevel2Role(applicantRole); // null for 1-level flows

        // ── Persist ───────────────────────────────────────────────
        CarryForwardLeaveApplication app = new CarryForwardLeaveApplication();
        app.setEmployee(employee);
        app.setApplicantRole(applicantRole);
        app.setYear(year);
        app.setStartDate(request.getStartDate());
        app.setEndDate(request.getEndDate());
        app.setDays(BigDecimal.valueOf(requestedDays));
        app.setReason(request.getReason());
        app.setStatus(RequestStatus.PENDING);
        app.setCurrentApprovalLevel(1);
        app.setTotalApprovalLevels(totalLevels);
        app.setLevel1RequiredRole(level1Role);
        app.setLevel2RequiredRole(level2Role);

        CarryForwardLeaveApplication saved = cfLeaveAppRepository.save(app);
        log.info("[CF-LEAVE] Saved: id={}, applicantRole={}, level1={}, level2={}, days={}",
                saved.getId(), applicantRole, level1Role, level2Role, requestedDays);

        return toResponse(saved, employee, null, null, remaining, null);
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVE  — called by any approver; service validates their role
    // ═══════════════════════════════════════════════════════════════

    /**
     * Processes an approval action.
     *
     * The service validates that the approverId's role matches the role
     * required at the application's current approval level. This is the
     * authoritative check — the controller's @PreAuthorize only guards
     * access to the endpoint (must be a management-tier role), while the
     * service ensures the right person in the right sequence approves.
     *
     * @param applicationId  the application being approved
     * @param approverId     the employee performing the approval
     */
    @Transactional
    public CarryForwardLeaveApplicationResponse approveLeave(Long applicationId, String approverId) {

        log.info("[CF-LEAVE] Approve attempt: applicationId={}, approverId={}", applicationId, approverId);

        CarryForwardLeaveApplication app = getOrThrow(applicationId);

        if (app.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "Application " + applicationId + " is not PENDING: " + app.getStatus());
        }

        // ── Validate approver's role matches current level ────────
        Employee approver = employeeRepository.findByEmpId(approverId)
                .orElseThrow(() -> new BadRequestException("Approver not found: " + approverId));

        String approverRole   = resolveRole(approver);
        String requiredRole   = approvalMatrix.getRequiredRoleForLevel(
                app.getApplicantRole(), app.getCurrentApprovalLevel());

        if (!approverRole.equalsIgnoreCase(requiredRole)) {
            throw new BadRequestException(String.format(
                    "Approver role '%s' cannot act at level %d — expected role '%s'",
                    approverRole, app.getCurrentApprovalLevel(), requiredRole));
        }

        // ── Prevent self-approval ─────────────────────────────────
        if (approverId.equals(app.getEmployee().getEmpId())) {
            throw new BadRequestException("An employee cannot approve their own leave application");
        }

        int  currentLevel = app.getCurrentApprovalLevel();
        int  totalLevels  = app.getTotalApprovalLevels();
        boolean isFinalLevel = (currentLevel == totalLevels);

        if (currentLevel == 1) {
            // Record level-1 approval
            app.setLevel1ApprovedBy(approverId);
            app.setLevel1ApprovedAt(LocalDateTime.now());
            log.info("[CF-LEAVE] Level-1 approved: id={}, by={} ({})", applicationId, approverId, approverRole);
        }

        if (isFinalLevel) {
            // ── Final approval: deduct balance ────────────────────
            double requestedDays = app.getDays().doubleValue();

            CarryForwardBalance balance = cfBalanceRepository
                    .findByEmployee_EmpIdAndYear(app.getEmployee().getEmpId(), app.getYear())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Carry-forward balance missing at final approval"));

            double remaining = balance.getRemaining() != null ? balance.getRemaining() : 0.0;
            if (requestedDays > remaining) {
                throw new BadRequestException(String.format(
                        "Cannot approve: %.1f days requested but only %.1f days remain",
                        requestedDays, remaining));
            }

            double balanceBefore = remaining;
            balance.setTotalUsed(safeAdd(balance.getTotalUsed(), requestedDays));
            balance.setRemaining(remaining - requestedDays);
            cfBalanceRepository.save(balance);

            app.setStatus(RequestStatus.APPROVED);
            app.setApprovedBy(approverId);
            app.setApprovedAt(LocalDateTime.now());
            app.setCurrentApprovalLevel(currentLevel + 1); // signals "done"

            CarryForwardLeaveApplication saved = cfLeaveAppRepository.save(app);
            Employee applicant = employeeRepository.findByEmpId(app.getEmployee().getEmpId()).orElse(null);

            log.info("[CF-LEAVE] FULLY APPROVED: id={}, balanceBefore={}, balanceAfter={}",
                    applicationId, balanceBefore, balance.getRemaining());

            return toResponse(saved, applicant, approver, null, balanceBefore, balance.getRemaining());

        } else {
            // ── Intermediate approval: advance to next level ──────
            app.setCurrentApprovalLevel(currentLevel + 1);
            CarryForwardLeaveApplication saved = cfLeaveAppRepository.save(app);
            Employee applicant = employeeRepository.findByEmpId(app.getEmployee().getEmpId()).orElse(null);

            log.info("[CF-LEAVE] Level-{} approved, advancing to level-{}: id={}",
                    currentLevel, currentLevel + 1, applicationId);

            CarryForwardBalance balance = cfBalanceRepository
                    .findByEmployee_EmpIdAndYear(app.getEmployee().getEmpId(), app.getYear()).orElse(null);
            double remaining = balance != null && balance.getRemaining() != null
                    ? balance.getRemaining() : 0.0;

            return toResponse(saved, applicant, approver, null, remaining, null);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REJECT  — any approver at any level can reject
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public CarryForwardLeaveApplicationResponse rejectLeave(Long applicationId, String rejectorId, String reason) {

        log.info("[CF-LEAVE] Reject attempt: applicationId={}, rejectorId={}", applicationId, rejectorId);

        CarryForwardLeaveApplication app = getOrThrow(applicationId);

        if (app.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "Application " + applicationId + " is not PENDING: " + app.getStatus());
        }

        // ── Validate rejector's role matches current level ────────
        Employee rejector = employeeRepository.findByEmpId(rejectorId)
                .orElseThrow(() -> new EntityNotFoundException("Rejector not found: " + rejectorId));

        String rejectorRole = resolveRole(rejector);
        String requiredRole = approvalMatrix.getRequiredRoleForLevel(
                app.getApplicantRole(), app.getCurrentApprovalLevel());

        if (!rejectorRole.equalsIgnoreCase(requiredRole)) {
            throw new BadRequestException(String.format(
                    "Rejector role '%s' cannot act at level %d — expected role '%s'",
                    rejectorRole, app.getCurrentApprovalLevel(), requiredRole));
        }

        int rejectedAtLevel = app.getCurrentApprovalLevel();

        app.setStatus(RequestStatus.REJECTED);
        app.setRejectedBy(rejectorId);
        app.setRejectedAt(LocalDateTime.now());
        app.setRejectionReason(reason != null ? reason : "No reason provided");
        app.setRejectedAtLevel(rejectedAtLevel);

        CarryForwardLeaveApplication saved = cfLeaveAppRepository.save(app);
        Employee applicant = employeeRepository.findByEmpId(app.getEmployee().getEmpId()).orElse(null);

        CarryForwardBalance balance = cfBalanceRepository
                .findByEmployee_EmpIdAndYear(app.getEmployee().getEmpId(), app.getYear()).orElse(null);
        double remaining = balance != null && balance.getRemaining() != null
                ? balance.getRemaining() : 0.0;

        log.info("[CF-LEAVE] Rejected at level {}: id={}", rejectedAtLevel, applicationId);
        return toResponse(saved, applicant, null, rejector, remaining, remaining);
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public CarryForwardLeaveApplicationResponse cancelLeave(Long applicationId, String employeeId) {

        log.info("[CF-LEAVE] Cancel: applicationId={}, employee={}", applicationId, employeeId);

        CarryForwardLeaveApplication app = getOrThrow(applicationId);

        if (!app.getEmployee().getEmpId().equals(employeeId)) {
            throw new BadRequestException(
                    "Employee " + employeeId + " is not the owner of application " + applicationId);
        }
        if (app.getStatus() == RequestStatus.REJECTED || app.getStatus() == RequestStatus.CANCELLED) {
            throw new BadRequestException("Application is already " + app.getStatus());
        }

        boolean wasApproved = (app.getStatus() == RequestStatus.APPROVED);
        app.setStatus(RequestStatus.CANCELLED);
        cfLeaveAppRepository.save(app);

        // Restore balance only if fully approved
        CarryForwardBalance balance = cfBalanceRepository
                .findByEmployee_EmpIdAndYear(app.getEmployee().getEmpId(), app.getYear()).orElse(null);

        if (wasApproved && balance != null) {
            double days = app.getDays().doubleValue();
            balance.setTotalUsed(Math.max(0, safeValue(balance.getTotalUsed()) - days));
            balance.setRemaining(safeValue(balance.getRemaining()) + days);
            cfBalanceRepository.save(balance);
            log.info("[CF-LEAVE] Cancelled approved leave — restored {} days", days);
        }

        double remaining = balance != null && balance.getRemaining() != null
                ? balance.getRemaining() : 0.0;

        Employee applicant = employeeRepository.findByEmpId(app.getEmployee().getEmpId()).orElse(null);
        return toResponse(app, applicant, null, null, remaining, remaining);
    }

    // ═══════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CarryForwardLeaveApplicationResponse getApplication(Long applicationId) {
        CarryForwardLeaveApplication app = getOrThrow(applicationId);
        Employee applicant = employeeRepository.findByEmpId(app.getEmployee().getEmpId()).orElse(null);
        Employee approver  = app.getApprovedBy() != null
                ? employeeRepository.findByEmpId(app.getApprovedBy()).orElse(null) : null;
        Employee rejector  = app.getRejectedBy() != null
                ? employeeRepository.findByEmpId(app.getRejectedBy()).orElse(null) : null;
        CarryForwardBalance balance = cfBalanceRepository
                .findByEmployee_EmpIdAndYear(app.getEmployee().getEmpId(), app.getYear()).orElse(null);
        double remaining = balance != null && balance.getRemaining() != null
                ? balance.getRemaining() : 0.0;
        return toResponse(app, applicant, approver, rejector, remaining, remaining);
    }

    @Transactional(readOnly = true)
    public List<CarryForwardLeaveApplicationResponse> getMyApplications(String employeeId) {
        Employee employee = employeeRepository.findByEmpId(employeeId).orElse(null);
        return cfLeaveAppRepository.findByEmployee_EmpId(employeeId).stream()
                .map(app -> {
                    CarryForwardBalance bal = cfBalanceRepository
                            .findByEmployee_EmpIdAndYear(employeeId, app.getYear()).orElse(null);
                    double rem = bal != null && bal.getRemaining() != null ? bal.getRemaining() : 0.0;
                    return toResponse(app, employee, null, null, rem, rem);
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns pending applications where the given employee (by their role)
     * is the required approver at the current approval level.
     *
     * For MANAGER-role approvers the queue is narrowed to their direct reports;
     * for HR, CEO, etc. it is company-wide.
     */
//    @Transactional(readOnly = true)
//    public List<CarryForwardLeaveApplicationResponse> getPendingForApprover(String approverId) {
//        Employee approver = employeeRepository.findByEmpId(approverId)
//                .orElseThrow(() -> new RuntimeException("Approver not found: " + approverId));
//        String role = resolveRole(approver);
//
//        List<CarryForwardLeaveApplication> pending;
//
//        // MANAGER: restrict to direct reports to avoid showing company-wide queue
//        if ("MANAGER".equalsIgnoreCase(role)) {
//            pending = cfLeaveAppRepository.findPendingByManagerAndRole(approverId, role);
//        } else {
//            pending = cfLeaveAppRepository.findPendingByApproverRole(role);
//        }
//
//        return pending.stream()
//                .map(app -> {
//                    Employee applicant = employeeRepository.findByEmpId(app.getEmployee().getEmpId()).orElse(null);
//                    CarryForwardBalance bal = cfBalanceRepository
//                            .findByEmployee_EmpIdAndYear(app.getEmployee().getEmpId(), app.getYear()).orElse(null);
//                    double rem = bal != null && bal.getRemaining() != null ? bal.getRemaining() : 0.0;
//                    return toResponse(app, applicant, null, null, rem, null);
//                })
//                .collect(Collectors.toList());
//    }

    @Transactional(readOnly = true)
    public List<CarryForwardLeaveApplicationResponse> getAllPending() {
        return cfLeaveAppRepository.findByStatus(RequestStatus.PENDING).stream()
                .map(app -> {
                    Employee applicant = employeeRepository.findByEmpId(app.getEmployee().getEmpId()).orElse(null);
                    CarryForwardBalance bal = cfBalanceRepository
                            .findByEmployee_EmpIdAndYear(app.getEmployee().getEmpId(), app.getYear()).orElse(null);
                    double rem = bal != null && bal.getRemaining() != null ? bal.getRemaining() : 0.0;
                    return toResponse(app, applicant, null, null, rem, null);
                })
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<CarryForwardLeaveApplicationResponse> getAllApplications() {
        return cfLeaveAppRepository.findAll().stream()
                .map(app -> {
                    Employee applicant = employeeRepository
                            .findByEmpId(app.getEmployee().getEmpId())
                            .orElse(null);

                    CarryForwardBalance bal = cfBalanceRepository
                            .findByEmployee_EmpIdAndYear(app.getEmployee().getEmpId(), app.getYear())
                            .orElse(null);

                    double remaining = bal != null && bal.getRemaining() != null
                            ? bal.getRemaining()
                            : 0.0;

                    return toResponse(app, applicant, null, null, remaining, remaining);
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // YEAR-END PROCESSING
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void processYearEndCarryForward(String employeeId, Integer fromYear, double annualLeaveRemaining) {

        int toYear = fromYear + 1;
        log.info("[CF-YEAR-END] employee={}, fromYear={}, annualLeaveRemaining={}",
                employeeId, fromYear, annualLeaveRemaining);

        double carryAmount = Math.min(annualLeaveRemaining, MAX_CARRY_FORWARD);

        if (carryAmount <= 0) {
            log.info("[CF-YEAR-END] Nothing to carry forward for employee {}", employeeId);
            return;
        }

        CarryForwardBalance existing = cfBalanceRepository
                .findByEmployee_EmpIdAndYear(employeeId, toYear).orElse(null);
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(()-> new EntityNotFoundException("Employee not found"));

        if (existing != null) {
            existing.setTotalCarriedForward(carryAmount);
            existing.setRemaining(carryAmount - safeValue(existing.getTotalUsed()));
            cfBalanceRepository.save(existing);
            log.info("[CF-YEAR-END] Updated CF balance: employee={}, toYear={}, amount={}",
                    employeeId, toYear, carryAmount);
        } else {
            CarryForwardBalance newBalance = new CarryForwardBalance();
            newBalance.setEmployee(employee);
            newBalance.setYear(toYear);
            newBalance.setTotalCarriedForward(carryAmount);
            newBalance.setTotalUsed(0.0);
            newBalance.setRemaining(carryAmount);
            cfBalanceRepository.save(newBalance);
            log.info("[CF-YEAR-END] Created CF balance: employee={}, toYear={}, amount={}",
                    employeeId, toYear, carryAmount);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private CarryForwardLeaveApplication getOrThrow(Long id) {
        return cfLeaveAppRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(
                        "Carry-forward leave application not found: " + id));
    }

    /**
     * Resolves an Employee's role to a normalised uppercase string.
     * Assumes Employee has a getRoleOrRole() method returning the role string.
     * Adjust the field access to match your Employee entity.
     */
    private String resolveRole(Employee employee) {
        if (employee.getRole() == null) {
            throw new BadRequestException("Employee " + employee.getEmpId() + " has no role assigned");
        }

        return employee.getRole().getRoleName(); // enum → String
    }

    /**
     * Counts working days (Mon–Fri) between two dates inclusive.
     * Returns 0.5 for a half-day regardless of the date range.
     */
    private double calculateWorkingDays(LocalDate start, LocalDate end, Boolean isHalfDay) {
        if (Boolean.TRUE.equals(isHalfDay)) return 0.5;
        if (end.isBefore(start)) {
            throw new BadRequestException("End date must be on or after start date");
        }
        long workingDays = start.datesUntil(end.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY
                        && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
        if (workingDays <= 0) {
            throw new BadRequestException("No working days in the selected date range");
        }
        return (double) workingDays;
    }

    private double safeValue(Double value) {
        return value != null ? value : 0.0;
    }

    private double safeAdd(Double base, double amount) {
        return safeValue(base) + amount;
    }

    /**
     * Builds the "what happens next" message shown to the caller.
     */
    private String buildNextAction(CarryForwardLeaveApplication app) {
        return switch (app.getStatus()) {
            case APPROVED   -> "Fully approved. Balance has been deducted.";
            case REJECTED   -> "Rejected at level " + app.getRejectedAtLevel() + ". No balance change.";
            case CANCELLED  -> "Cancelled by employee.";
            case PENDING    -> {
                String role = approvalMatrix.getRequiredRoleForLevel(
                        app.getApplicantRole(), app.getCurrentApprovalLevel());
                yield "Awaiting level-" + app.getCurrentApprovalLevel()
                        + " approval from " + role + ".";
            }
            default -> "";
        };
    }

    private CarryForwardLeaveApplicationResponse toResponse(
            CarryForwardLeaveApplication app,
            Employee applicant,
            Employee finalApprover,
            Employee rejector,
            Double balanceBefore,
            Double balanceAfter) {

        // level-1 approver name (if recorded)
        Employee level1Approver = app.getLevel1ApprovedBy() != null
                ? employeeRepository.findByEmpId(app.getLevel1ApprovedBy()).orElse(null)
                : null;

        CarryForwardLeaveApplicationResponse r = new CarryForwardLeaveApplicationResponse();
        r.setId(app.getId());
        r.setEmployeeId(app.getEmployee().getEmpId());
        r.setEmployeeName(applicant != null ? applicant.getName() : "Unknown");
        r.setApplicantRole(app.getApplicantRole());
        r.setYear(app.getYear());
        r.setStartDate(app.getStartDate());
        r.setEndDate(app.getEndDate());
        r.setDays(app.getDays());
        r.setReason(app.getReason());
        r.setStatus(app.getStatus());
        r.setCurrentApprovalLevel(app.getCurrentApprovalLevel());
        r.setTotalApprovalLevels(app.getTotalApprovalLevels());
        r.setNextAction(buildNextAction(app));

        // Level 1
        r.setLevel1RequiredRole(app.getLevel1RequiredRole());
        r.setLevel1ApprovedBy(app.getLevel1ApprovedBy());
        r.setLevel1ApprovedByName(level1Approver != null ? level1Approver.getName() : null);
        r.setLevel1ApprovedAt(app.getLevel1ApprovedAt());

        // Level 2 / final
        r.setLevel2RequiredRole(app.getLevel2RequiredRole());
        r.setApprovedBy(app.getApprovedBy());
        r.setApprovedByName(finalApprover != null ? finalApprover.getName() : null);
        r.setApprovedAt(app.getApprovedAt());

        // Rejection
        r.setRejectedBy(app.getRejectedBy());
        r.setRejectedByName(rejector != null ? rejector.getName() : null);
        r.setRejectedAt(app.getRejectedAt());
        r.setRejectionReason(app.getRejectionReason());
        r.setRejectedAtLevel(app.getRejectedAtLevel());

        r.setCfBalanceBefore(balanceBefore);
        r.setCfBalanceAfter(balanceAfter);
        r.setCreatedAt(app.getCreatedAt());
        return r;
    }
}