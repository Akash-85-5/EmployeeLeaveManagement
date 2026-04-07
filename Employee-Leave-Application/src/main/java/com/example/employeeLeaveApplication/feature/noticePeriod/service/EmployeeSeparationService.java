package com.example.employeeLeaveApplication.feature.noticePeriod.service;

import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.noticePeriod.entity.EmployeeSeparation;
import com.example.employeeLeaveApplication.feature.noticePeriod.repository.EmployeeSeparationRepository;
import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.leave.lop.repository.LopRecordRepository;
import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import com.example.employeeLeaveApplication.shared.enums.SeparationType;
import com.example.employeeLeaveApplication.shared.enums.TerminationGrounds;
import com.example.employeeLeaveApplication.shared.enums.TerminationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * EmployeeSeparationService
 *
 * Handles all 4 leaving types:
 *   1. RESIGNATION
 *   2. TERMINATION
 *   3. DEATH_IN_SERVICE
 *   4. ABSCONDING
 *
 * ══════════════════════════════════════════════════════
 * APPROVAL CHAINS
 * ══════════════════════════════════════════════════════
 *
 * RESIGNATION:
 *   Employee / Team Leader → PENDING_MANAGER
 *     Manager approves     → APPROVED → notify CEO, HR, Admin, TL
 *   Manager                → PENDING_HR
 *     HR approves          → APPROVED → notify CEO, Admin
 *   HR                     → PENDING_CEO
 *     CEO approves         → APPROVED → notify Admin
 *
 * TERMINATION (Manager files):
 *   → PENDING_HR → HR approves → APPROVED → notify CEO, Admin, TL
 *
 * TERMINATION (HR files):
 *   → APPROVED immediately → notify Manager, CEO, Admin, TL
 *
 * TERMINATION of a Manager (HR files):
 *   → APPROVED immediately → notify CEO, Admin
 *
 * TERMINATION of HR (CEO files):
 *   → APPROVED immediately → notify Admin
 *
 * DEATH IN SERVICE (Manager files):
 *   → PENDING_HR → HR approves → APPROVED → notify CEO, Admin
 *
 * DEATH IN SERVICE (HR files):
 *   → APPROVED immediately → notify CEO, Admin
 *
 * ABSCONDING (Manager files):
 *   → PENDING_HR → HR approves → APPROVED → deactivate → notify CEO, Admin
 *
 * ABSCONDING (HR files):
 *   → APPROVED immediately → deactivate → notify CEO, Admin
 *
 * ══════════════════════════════════════════════════════
 * NOTICE PERIOD RULES
 * ══════════════════════════════════════════════════════
 *   - 3 calendar months from start date
 *   - No leave or WFH allowed (enforced by NoticePeriodGuard)
 *   - Each LOP absent day extends the end date by 1 day
 *   - Salary paid at end of 3rd month (CFO module handles this)
 *   - Admin fills exit checklist at end (laptop / ID card / KT)
 *   - CFO confirms payslip generated → status = RELIEVED
 *   - Admin deactivates account
 *
 * WITHOUT NOTICE PERIOD (termination only):
 *   - Allowed only if grounds = PROBATION / INTERNSHIP / MISCONDUCT
 *   - Account deactivated immediately on approval
 *   - No exit checklist, no payslip from this module
 */
@Service
@Transactional
public class EmployeeSeparationService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeSeparationService.class);

    private final EmployeeSeparationRepository  separationRepo;
    private final EmployeeRepository            employeeRepo;
    private final LopRecordRepository           lopRepo;
    private final SeparationNotificationService notificationService;

    public EmployeeSeparationService(
            EmployeeSeparationRepository  separationRepo,
            EmployeeRepository            employeeRepo,
            LopRecordRepository           lopRepo,
            SeparationNotificationService notificationService) {

        this.separationRepo      = separationRepo;
        this.employeeRepo        = employeeRepo;
        this.lopRepo             = lopRepo;
        this.notificationService = notificationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. RESIGNATION
    // ═══════════════════════════════════════════════════════════════

    public EmployeeSeparation submitResignation(Long employeeId, String reason) {

        Employee emp = findEmployee(employeeId);
        guardNoActiveSeparation(employeeId);

        EmployeeSeparation s = new EmployeeSeparation();
        s.setEmployeeId(employeeId);
        s.setEmployeeName(emp.getName());
        s.setEmployeeRole(emp.getRole().name());
        s.setInitiatorId(employeeId);
        s.setInitiatorRole(emp.getRole().name());
        s.setSeparationType(SeparationType.RESIGNATION);
        s.setReason(reason);

        switch (emp.getRole()) {
            case EMPLOYEE, TEAM_LEADER -> s.setStatus(SeparationStatus.PENDING_MANAGER);
            case MANAGER               -> s.setStatus(SeparationStatus.PENDING_HR);
            case HR                    -> s.setStatus(SeparationStatus.PENDING_CEO);
            default -> throw new RuntimeException(
                    "Role " + emp.getRole() + " cannot submit resignation via this flow.");
        }

        EmployeeSeparation saved = separationRepo.save(s);
        log.info("Resignation submitted by employee {} ({}), status: {}",
                employeeId, emp.getRole(), saved.getStatus());
        return saved;
    }

    // Manager approves resignation (Employee / Team Leader resigning)
    public EmployeeSeparation managerApproveResignation(Long separationId,
                                                        Long managerId,
                                                        String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.RESIGNATION, SeparationStatus.PENDING_MANAGER);

        s.setApprovedByManager(managerId);
        s.setManagerActionAt(LocalDateTime.now());
        s.setManagerRemarks(remarks);
        s.setStatus(SeparationStatus.APPROVED);

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyResignationApprovedByManager(saved);
        return saved;
    }

    public EmployeeSeparation managerRejectResignation(Long separationId,
                                                       Long managerId,
                                                       String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.RESIGNATION, SeparationStatus.PENDING_MANAGER);

        s.setApprovedByManager(managerId);
        s.setManagerActionAt(LocalDateTime.now());
        s.setManagerRemarks(remarks);
        s.setStatus(SeparationStatus.REJECTED);
        return separationRepo.save(s);
    }

    // HR approves resignation (Manager resigning)
    public EmployeeSeparation hrApproveResignation(Long separationId,
                                                   Long hrId,
                                                   String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.RESIGNATION, SeparationStatus.PENDING_HR);

        s.setApprovedByHr(hrId);
        s.setHrActionAt(LocalDateTime.now());
        s.setHrRemarks(remarks);
        s.setStatus(SeparationStatus.APPROVED);

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyManagerResignationApprovedByHR(saved);
        return saved;
    }

    public EmployeeSeparation hrRejectResignation(Long separationId,
                                                  Long hrId,
                                                  String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.RESIGNATION, SeparationStatus.PENDING_HR);

        s.setApprovedByHr(hrId);
        s.setHrActionAt(LocalDateTime.now());
        s.setHrRemarks(remarks);
        s.setStatus(SeparationStatus.REJECTED);
        return separationRepo.save(s);
    }

    // CEO approves resignation (HR resigning)
    public EmployeeSeparation ceoApproveResignation(Long separationId,
                                                    Long ceoId,
                                                    String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.RESIGNATION, SeparationStatus.PENDING_CEO);

        s.setApprovedByCeo(ceoId);
        s.setCeoActionAt(LocalDateTime.now());
        s.setCeoRemarks(remarks);
        s.setStatus(SeparationStatus.APPROVED);

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyHrResignationApprovedByCEO(saved);
        return saved;
    }

    public EmployeeSeparation ceoRejectResignation(Long separationId,
                                                   Long ceoId,
                                                   String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.RESIGNATION, SeparationStatus.PENDING_CEO);

        s.setApprovedByCeo(ceoId);
        s.setCeoActionAt(LocalDateTime.now());
        s.setCeoRemarks(remarks);
        s.setStatus(SeparationStatus.REJECTED);
        return separationRepo.save(s);
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. TERMINATION
    // ═══════════════════════════════════════════════════════════════

    public EmployeeSeparation initiateTermination(Long employeeId,
                                                  Long initiatorId,
                                                  TerminationType terminationType,
                                                  TerminationGrounds terminationGrounds,
                                                  String reason) {

        Employee emp       = findEmployee(employeeId);
        Employee initiator = findEmployee(initiatorId);
        guardNoActiveSeparation(employeeId);

        // WITHOUT_NOTICE_PERIOD requires a grounds value
        if (terminationType == TerminationType.WITHOUT_NOTICE_PERIOD
                && terminationGrounds == null) {
            throw new RuntimeException(
                    "terminationGrounds is required for WITHOUT_NOTICE_PERIOD. " +
                            "Allowed values: PROBATION, INTERNSHIP, MISCONDUCT");
        }

        EmployeeSeparation s = new EmployeeSeparation();
        s.setEmployeeId(employeeId);
        s.setEmployeeName(emp.getName());
        s.setEmployeeRole(emp.getRole().name());
        s.setInitiatorId(initiatorId);
        s.setInitiatorRole(initiator.getRole().name());
        s.setSeparationType(SeparationType.TERMINATION);
        s.setTerminationType(terminationType);
        s.setTerminationGrounds(terminationGrounds);
        s.setReason(reason);

        Role initiatorRole = initiator.getRole();
        Role targetRole    = emp.getRole();

        if (initiatorRole == Role.MANAGER) {
            // Manager files → needs HR approval
            s.setStatus(SeparationStatus.PENDING_HR);

        } else if (initiatorRole == Role.HR) {
            // HR files → directly approved
            s.setApprovedByHr(initiatorId);
            s.setHrActionAt(LocalDateTime.now());
            approveAndHandleNoticePeriod(s, initiatorId, terminationType);

        } else if (initiatorRole == Role.CEO && targetRole == Role.HR) {
            // CEO terminates HR → directly approved
            s.setApprovedByCeo(initiatorId);
            s.setCeoActionAt(LocalDateTime.now());
            approveAndHandleNoticePeriod(s, initiatorId, terminationType);

        } else {
            throw new RuntimeException(
                    "Initiator role " + initiatorRole + " cannot terminate role " + targetRole);
        }

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyTerminationInitiated(saved);
        return saved;
    }

    // HR approves termination (when Manager filed it)
    public EmployeeSeparation hrApproveTermination(Long separationId,
                                                   Long hrId,
                                                   String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.TERMINATION, SeparationStatus.PENDING_HR);

        s.setApprovedByHr(hrId);
        s.setHrActionAt(LocalDateTime.now());
        s.setHrRemarks(remarks);
        approveAndHandleNoticePeriod(s, hrId, s.getTerminationType());

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyTerminationApproved(saved);
        return saved;
    }

    public EmployeeSeparation hrRejectTermination(Long separationId,
                                                  Long hrId,
                                                  String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.TERMINATION, SeparationStatus.PENDING_HR);

        s.setApprovedByHr(hrId);
        s.setHrActionAt(LocalDateTime.now());
        s.setHrRemarks(remarks);
        s.setStatus(SeparationStatus.REJECTED);
        return separationRepo.save(s);
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. DEATH IN SERVICE
    // ═══════════════════════════════════════════════════════════════

    public EmployeeSeparation fileDeathInService(Long employeeId,
                                                 Long initiatorId,
                                                 LocalDate deathDate) {

        Employee emp       = findEmployee(employeeId);
        Employee initiator = findEmployee(initiatorId);
        guardNoActiveSeparation(employeeId);

        EmployeeSeparation s = new EmployeeSeparation();
        s.setEmployeeId(employeeId);
        s.setEmployeeName(emp.getName());
        s.setEmployeeRole(emp.getRole().name());
        s.setInitiatorId(initiatorId);
        s.setInitiatorRole(initiator.getRole().name());
        s.setSeparationType(SeparationType.DEATH_IN_SERVICE);
        s.setReason("Death in service");
        s.setDeathDate(deathDate);

        Role initiatorRole = initiator.getRole();

        if (initiatorRole == Role.MANAGER || initiatorRole == Role.TEAM_LEADER) {
            s.setStatus(SeparationStatus.PENDING_HR);

        } else if (initiatorRole == Role.HR) {
            s.setStatus(SeparationStatus.APPROVED);
            s.setApprovedByHr(initiatorId);
            s.setHrActionAt(LocalDateTime.now());
            notificationService.notifyDeathInService(s);

        } else {
            throw new RuntimeException("Only Manager or HR can file death in service.");
        }

        return separationRepo.save(s);
    }

    // HR approves death in service (when Manager filed it)
    public EmployeeSeparation hrApproveDeathInService(Long separationId,
                                                      Long hrId,
                                                      String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.DEATH_IN_SERVICE, SeparationStatus.PENDING_HR);

        s.setApprovedByHr(hrId);
        s.setHrActionAt(LocalDateTime.now());
        s.setHrRemarks(remarks);
        s.setStatus(SeparationStatus.APPROVED);

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyDeathInService(saved);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. ABSCONDING
    // ═══════════════════════════════════════════════════════════════

    public EmployeeSeparation fileAbsconding(Long employeeId,
                                             Long initiatorId,
                                             String reason) {

        Employee emp       = findEmployee(employeeId);
        Employee initiator = findEmployee(initiatorId);
        guardNoActiveSeparation(employeeId);

        EmployeeSeparation s = new EmployeeSeparation();
        s.setEmployeeId(employeeId);
        s.setEmployeeName(emp.getName());
        s.setEmployeeRole(emp.getRole().name());
        s.setInitiatorId(initiatorId);
        s.setInitiatorRole(initiator.getRole().name());
        s.setSeparationType(SeparationType.ABSCONDING);
        s.setTerminationType(TerminationType.WITHOUT_NOTICE_PERIOD);
        s.setReason(reason);

        Role initiatorRole = initiator.getRole();

        if (initiatorRole == Role.HR) {
            s.setStatus(SeparationStatus.APPROVED);
            s.setApprovedByHr(initiatorId);
            s.setHrActionAt(LocalDateTime.now());
            deactivateImmediately(s, initiatorId);
            notificationService.notifyAbsconding(s);

        } else if (initiatorRole == Role.MANAGER || initiatorRole == Role.TEAM_LEADER) {
            s.setStatus(SeparationStatus.PENDING_HR);

        } else {
            throw new RuntimeException("Only HR or Manager can file absconding.");
        }

        return separationRepo.save(s);
    }

    public EmployeeSeparation hrApproveAbsconding(Long separationId,
                                                  Long hrId,
                                                  String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.ABSCONDING, SeparationStatus.PENDING_HR);

        s.setApprovedByHr(hrId);
        s.setHrActionAt(LocalDateTime.now());
        s.setHrRemarks(remarks);
        s.setStatus(SeparationStatus.APPROVED);
        deactivateImmediately(s, hrId);

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyAbsconding(saved);
        return saved;
    }

    public EmployeeSeparation hrRejectAbsconding(Long separationId,
                                                 Long hrId,
                                                 String remarks) {
        EmployeeSeparation s = findAndValidate(separationId,
                SeparationType.ABSCONDING, SeparationStatus.PENDING_HR);

        s.setApprovedByHr(hrId);
        s.setHrActionAt(LocalDateTime.now());
        s.setHrRemarks(remarks);
        s.setStatus(SeparationStatus.REJECTED);
        return separationRepo.save(s);
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTICE PERIOD — Admin starts it after approval
    // ═══════════════════════════════════════════════════════════════

    public EmployeeSeparation startNoticePeriodByAdmin(Long separationId, Long adminId) {

        EmployeeSeparation s = findSeparation(separationId);

        if (s.getStatus() != SeparationStatus.APPROVED) {
            throw new RuntimeException(
                    "Cannot start notice period. Status must be APPROVED. Current: " + s.getStatus());
        }

        LocalDate start = LocalDate.now();
        LocalDate end   = start.plusMonths(3);

        s.setNoticePeriodStart(start);
        s.setNoticePeriodEnd(end);
        s.setStatus(SeparationStatus.NOTICE_PERIOD);

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyNoticePeriodStarted(saved);    // Notify CFO
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTICE PERIOD — Scheduler calls these daily
    // ═══════════════════════════════════════════════════════════════

    // Extends notice period end date for each new LOP absence
    @Transactional
    public void extendNoticePeriodForLop(EmployeeSeparation s) {
        LocalDate start = s.getNoticePeriodStart();
        LocalDate today = LocalDate.now();

        Double lopTotal = lopRepo.sumLopDaysForPeriod(s.getEmployeeId(), start, today);
        int newLopTotal = lopTotal != null ? lopTotal.intValue() : 0;

        if (newLopTotal > s.getLopDaysInNotice()) {
            int extra   = newLopTotal - s.getLopDaysInNotice();
            LocalDate newEnd = s.getNoticePeriodEnd().plusDays(extra);
            s.setLopDaysInNotice(newLopTotal);
            s.setNoticePeriodEnd(newEnd);
            separationRepo.save(s);
            log.info("Notice period extended by {} days for emp {}. New end: {}",
                    extra, s.getEmployeeId(), newEnd);
        }
    }

    // Called when notice period end date has passed
    @Transactional
    public void markNoticeCompleted(EmployeeSeparation s) {
        s.setStatus(SeparationStatus.NOTICE_COMPLETED);
        separationRepo.save(s);
        notificationService.notifyNoticeCompleted(s);   // Notify Admin
    }

    // ═══════════════════════════════════════════════════════════════
    // EXIT CHECKLIST — Admin fills at end of notice period
    // ═══════════════════════════════════════════════════════════════

    public EmployeeSeparation completeExitChecklist(Long separationId,
                                                    Long adminId,
                                                    boolean laptopReturned,
                                                    boolean idCardReturned,
                                                    boolean ktCompleted) {

        EmployeeSeparation s = findSeparation(separationId);

        if (s.getStatus() != SeparationStatus.NOTICE_COMPLETED) {
            throw new RuntimeException(
                    "Exit checklist can only be filled when status is NOTICE_COMPLETED. Current: "
                            + s.getStatus());
        }

        s.setLaptopReturned(laptopReturned);
        s.setIdCardReturned(idCardReturned);
        s.setKtCompleted(ktCompleted);
        s.setExitChecklistCompletedBy(adminId);
        s.setExitChecklistCompletedAt(LocalDateTime.now());

        if (laptopReturned && idCardReturned && ktCompleted) {
            s.setStatus(SeparationStatus.EXIT_CHECKLIST_DONE);
            EmployeeSeparation saved = separationRepo.save(s);
            notificationService.notifyExitChecklistComplete(saved);   // Notify CFO
            return saved;
        }

        return separationRepo.save(s);
    }

    // ═══════════════════════════════════════════════════════════════
    // CFO — confirms payslip generated (actual payslip = friend's module)
    // ═══════════════════════════════════════════════════════════════

    public EmployeeSeparation markFinalPayslipGenerated(Long separationId, Long cfoId) {

        EmployeeSeparation s = findSeparation(separationId);

        if (s.getStatus() != SeparationStatus.EXIT_CHECKLIST_DONE) {
            throw new RuntimeException(
                    "Payslip can only be confirmed after exit checklist is done. Current: "
                            + s.getStatus());
        }

        s.setFinalPayslipGenerated(true);
        s.setFinalPayslipGeneratedAt(LocalDateTime.now());
        s.setFinalPayslipGeneratedBy(cfoId);
        s.setStatus(SeparationStatus.RELIEVED);

        EmployeeSeparation saved = separationRepo.save(s);
        notificationService.notifyPayslipGeneratedAndRelieve(saved);  // Notify Admin
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN — deactivates account at the very end
    // ═══════════════════════════════════════════════════════════════

    public EmployeeSeparation deactivateByAdmin(Long separationId, Long adminId) {

        EmployeeSeparation s = findSeparation(separationId);

        s.setAccountDeactivated(true);
        s.setAccountDeactivatedAt(LocalDateTime.now());
        s.setDeactivatedBy(adminId);

        Employee emp = findEmployee(s.getEmployeeId());
        emp.setActive(false);
        employeeRepo.save(emp);

        return separationRepo.save(s);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<EmployeeSeparation> getPendingForManager() {
        return separationRepo.findByStatusOrderByCreatedAtAsc(SeparationStatus.PENDING_MANAGER);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSeparation> getPendingForHR() {
        return separationRepo.findByStatusOrderByCreatedAtAsc(SeparationStatus.PENDING_HR);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSeparation> getPendingForCEO() {
        return separationRepo.findByStatusOrderByCreatedAtAsc(SeparationStatus.PENDING_CEO);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSeparation> getAllInNoticePeriod() {
        return separationRepo.findAllInNoticePeriod();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSeparation> getReadyForExitChecklist() {
        return separationRepo.findReadyForExitChecklist();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSeparation> getPayslipPending() {
        return separationRepo.findPayslipPending();
    }

    @Transactional(readOnly = true)
    public EmployeeSeparation getById(Long id) {
        return findSeparation(id);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSeparation> getByEmployeeId(Long employeeId) {
        return separationRepo.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public List<Long> getPotentialAbsconders() {
        return separationRepo.findPotentialAbsconders(LocalDate.now().minusDays(7));
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private EmployeeSeparation findSeparation(Long id) {
        return separationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Separation record not found: " + id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
    }

    private void guardNoActiveSeparation(Long employeeId) {
        List<EmployeeSeparation> active = separationRepo.findActiveByEmployeeId(employeeId);
        if (!active.isEmpty()) {
            throw new RuntimeException(
                    "Employee " + employeeId + " already has an active separation record. " +
                            "id=" + active.get(0).getId() + ", status=" + active.get(0).getStatus());
        }
    }

    private EmployeeSeparation findAndValidate(Long separationId,
                                               SeparationType expectedType,
                                               SeparationStatus expectedStatus) {
        EmployeeSeparation s = findSeparation(separationId);
        if (s.getSeparationType() != expectedType) {
            throw new RuntimeException(
                    "Expected type " + expectedType + " but found " + s.getSeparationType());
        }
        if (s.getStatus() != expectedStatus) {
            throw new RuntimeException(
                    "Expected status " + expectedStatus + " but found " + s.getStatus());
        }
        return s;
    }

    // Sets status APPROVED and decides notice period vs immediate deactivation
    private void approveAndHandleNoticePeriod(EmployeeSeparation s,
                                              Long actorId,
                                              TerminationType terminationType) {
        if (terminationType == TerminationType.WITH_NOTICE_PERIOD) {
            s.setStatus(SeparationStatus.APPROVED);
            // Admin will call startNoticePeriodByAdmin() separately
        } else {
            // WITHOUT_NOTICE_PERIOD → deactivate immediately
            deactivateImmediately(s, actorId);
        }
    }

    // Immediate deactivation — used for WITHOUT_NOTICE_PERIOD and ABSCONDING
    private void deactivateImmediately(EmployeeSeparation s, Long actorId) {
        s.setStatus(SeparationStatus.RELIEVED);
        s.setAccountDeactivated(true);
        s.setAccountDeactivatedAt(LocalDateTime.now());
        s.setDeactivatedBy(actorId);

        Employee emp = findEmployee(s.getEmployeeId());
        emp.setActive(false);
        employeeRepo.save(emp);
    }
}