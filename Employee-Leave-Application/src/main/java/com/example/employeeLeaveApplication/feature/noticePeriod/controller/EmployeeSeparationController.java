package com.example.employeeLeaveApplication.feature.noticePeriod.controller;

import com.example.employeeLeaveApplication.feature.noticePeriod.dto.SeparationResponse;
import com.example.employeeLeaveApplication.feature.noticePeriod.entity.EmployeeSeparation;
import com.example.employeeLeaveApplication.shared.enums.TerminationGrounds;
import com.example.employeeLeaveApplication.shared.enums.TerminationType;
import com.example.employeeLeaveApplication.feature.noticePeriod.service.EmployeeSeparationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EmployeeSeparationController
 * Base URL: /api/separation
 *
 * ══════════════════════════════════════════════════════════════
 * RESIGNATION
 * POST   /api/separation/resign
 *        Params: employeeId, reason
 *
 * PUT    /api/separation/{id}/manager/approve
 *        Params: actorId, remarks (optional)
 *
 * PUT    /api/separation/{id}/manager/reject
 *        Params: actorId, remarks (optional)
 *
 * PUT    /api/separation/{id}/hr/approve-resignation
 *        Params: actorId, remarks (optional)
 *
 * PUT    /api/separation/{id}/hr/reject-resignation
 *        Params: actorId, remarks (optional)
 *
 * PUT    /api/separation/{id}/ceo/approve-resignation
 *        Params: actorId, remarks (optional)
 *
 * PUT    /api/separation/{id}/ceo/reject-resignation
 *        Params: actorId, remarks (optional)
 *
 * ══════════════════════════════════════════════════════════════
 * TERMINATION
 * POST   /api/separation/terminate
 *        Params: employeeId, initiatorId, terminationType,
 *                terminationGrounds (only if WITHOUT_NOTICE_PERIOD), reason
 *
 * PUT    /api/separation/{id}/hr/approve-termination
 *        Params: actorId, remarks (optional)
 *
 * PUT    /api/separation/{id}/hr/reject-termination
 *        Params: actorId, remarks (optional)
 *
 * ══════════════════════════════════════════════════════════════
 * DEATH IN SERVICE
 * POST   /api/separation/death-in-service
 *        Params: employeeId, initiatorId, deathDate (yyyy-MM-dd)
 *
 * PUT    /api/separation/{id}/hr/approve-death
 *        Params: actorId, remarks (optional)
 *
 * ══════════════════════════════════════════════════════════════
 * ABSCONDING
 * POST   /api/separation/absconding
 *        Params: employeeId, initiatorId, reason
 *
 * PUT    /api/separation/{id}/hr/approve-absconding
 *        Params: actorId, remarks (optional)
 *
 * PUT    /api/separation/{id}/hr/reject-absconding
 *        Params: actorId, remarks (optional)
 *
 * ══════════════════════════════════════════════════════════════
 * NOTICE PERIOD & EXIT WORKFLOW
 * PUT    /api/separation/{id}/admin/start-notice-period
 *        Params: adminId
 *
 * PUT    /api/separation/{id}/admin/exit-checklist
 *        Params: adminId, laptopReturned, idCardReturned, ktCompleted
 *
 * PUT    /api/separation/{id}/cfo/payslip-generated
 *        Params: cfoId
 *
 * PUT    /api/separation/{id}/admin/deactivate
 *        Params: adminId
 *
 * ══════════════════════════════════════════════════════════════
 * QUERIES
 * GET    /api/separation/pending/manager
 * GET    /api/separation/pending/hr
 * GET    /api/separation/pending/ceo
 * GET    /api/separation/notice-period/active
 * GET    /api/separation/notice-period/exit-checklist-pending
 * GET    /api/separation/payslip-pending
 * GET    /api/separation/employee/{employeeId}
 * GET    /api/separation/{id}
 */
@RestController
@RequestMapping("/api/separation")
public class EmployeeSeparationController {

    private final EmployeeSeparationService service;

    public EmployeeSeparationController(EmployeeSeparationService service) {
        this.service = service;
    }

    // ═══════════════════════════════════════════════════════════════
    // RESIGNATION
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/resign")
    public ResponseEntity<SeparationResponse> submitResignation(
            @RequestParam Long   employeeId,
            @RequestParam String reason) {
        return ok(service.submitResignation(employeeId, reason));
    }

    @PutMapping("/{id}/manager/approve")
    public ResponseEntity<SeparationResponse> managerApproveResignation(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.managerApproveResignation(id, actorId, remarks));
    }

    @PutMapping("/{id}/manager/reject")
    public ResponseEntity<SeparationResponse> managerRejectResignation(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.managerRejectResignation(id, actorId, remarks));
    }

    @PutMapping("/{id}/hr/approve-resignation")
    public ResponseEntity<SeparationResponse> hrApproveResignation(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.hrApproveResignation(id, actorId, remarks));
    }

    @PutMapping("/{id}/hr/reject-resignation")
    public ResponseEntity<SeparationResponse> hrRejectResignation(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.hrRejectResignation(id, actorId, remarks));
    }

    @PutMapping("/{id}/ceo/approve-resignation")
    public ResponseEntity<SeparationResponse> ceoApproveResignation(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.ceoApproveResignation(id, actorId, remarks));
    }

    @PutMapping("/{id}/ceo/reject-resignation")
    public ResponseEntity<SeparationResponse> ceoRejectResignation(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.ceoRejectResignation(id, actorId, remarks));
    }

    // ═══════════════════════════════════════════════════════════════
    // TERMINATION
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/terminate")
    public ResponseEntity<SeparationResponse> initiateTermination(
            @RequestParam Long             employeeId,
            @RequestParam Long             initiatorId,
            @RequestParam TerminationType  terminationType,
            @RequestParam(required = false) TerminationGrounds terminationGrounds,
            @RequestParam String           reason) {
        return ok(service.initiateTermination(
                employeeId, initiatorId, terminationType, terminationGrounds, reason));
    }

    @PutMapping("/{id}/hr/approve-termination")
    public ResponseEntity<SeparationResponse> hrApproveTermination(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.hrApproveTermination(id, actorId, remarks));
    }

    @PutMapping("/{id}/hr/reject-termination")
    public ResponseEntity<SeparationResponse> hrRejectTermination(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.hrRejectTermination(id, actorId, remarks));
    }

    // ═══════════════════════════════════════════════════════════════
    // DEATH IN SERVICE
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/death-in-service")
    public ResponseEntity<SeparationResponse> fileDeathInService(
            @RequestParam Long   employeeId,
            @RequestParam Long   initiatorId,
            @RequestParam String deathDate) {
        return ok(service.fileDeathInService(
                employeeId, initiatorId, LocalDate.parse(deathDate)));
    }

    @PutMapping("/{id}/hr/approve-death")
    public ResponseEntity<SeparationResponse> hrApproveDeathInService(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.hrApproveDeathInService(id, actorId, remarks));
    }

    // ═══════════════════════════════════════════════════════════════
    // ABSCONDING
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/absconding")
    public ResponseEntity<SeparationResponse> fileAbsconding(
            @RequestParam Long   employeeId,
            @RequestParam Long   initiatorId,
            @RequestParam String reason) {
        return ok(service.fileAbsconding(employeeId, initiatorId, reason));
    }

    @PutMapping("/{id}/hr/approve-absconding")
    public ResponseEntity<SeparationResponse> hrApproveAbsconding(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.hrApproveAbsconding(id, actorId, remarks));
    }

    @PutMapping("/{id}/hr/reject-absconding")
    public ResponseEntity<SeparationResponse> hrRejectAbsconding(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam(required = false) String remarks) {
        return ok(service.hrRejectAbsconding(id, actorId, remarks));
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTICE PERIOD & EXIT WORKFLOW
    // ═══════════════════════════════════════════════════════════════

    @PutMapping("/{id}/admin/start-notice-period")
    public ResponseEntity<SeparationResponse> startNoticePeriod(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        return ok(service.startNoticePeriodByAdmin(id, adminId));
    }

    @PutMapping("/{id}/admin/exit-checklist")
    public ResponseEntity<SeparationResponse> completeExitChecklist(
            @PathVariable Long    id,
            @RequestParam Long    adminId,
            @RequestParam boolean laptopReturned,
            @RequestParam boolean idCardReturned,
            @RequestParam boolean ktCompleted) {
        return ok(service.completeExitChecklist(
                id, adminId, laptopReturned, idCardReturned, ktCompleted));
    }

    @PutMapping("/{id}/cfo/payslip-generated")
    public ResponseEntity<SeparationResponse> markPayslipGenerated(
            @PathVariable Long id,
            @RequestParam Long cfoId) {
        return ok(service.markFinalPayslipGenerated(id, cfoId));
    }

    @PutMapping("/{id}/admin/deactivate")
    public ResponseEntity<SeparationResponse> deactivateAccount(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        return ok(service.deactivateByAdmin(id, adminId));
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/pending/manager")
    public ResponseEntity<List<SeparationResponse>> getPendingForManager() {
        return okList(service.getPendingForManager());
    }

    @GetMapping("/pending/hr")
    public ResponseEntity<List<SeparationResponse>> getPendingForHR() {
        return okList(service.getPendingForHR());
    }

    @GetMapping("/pending/ceo")
    public ResponseEntity<List<SeparationResponse>> getPendingForCEO() {
        return okList(service.getPendingForCEO());
    }

    @GetMapping("/notice-period/active")
    public ResponseEntity<List<SeparationResponse>> getAllInNoticePeriod() {
        return okList(service.getAllInNoticePeriod());
    }

    @GetMapping("/notice-period/exit-checklist-pending")
    public ResponseEntity<List<SeparationResponse>> getReadyForExitChecklist() {
        return okList(service.getReadyForExitChecklist());
    }

    @GetMapping("/payslip-pending")
    public ResponseEntity<List<SeparationResponse>> getPayslipPending() {
        return okList(service.getPayslipPending());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<SeparationResponse>> getByEmployee(
            @PathVariable Long employeeId) {
        return okList(service.getByEmployeeId(employeeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeparationResponse> getById(@PathVariable Long id) {
        return ok(service.getById(id));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private ResponseEntity<SeparationResponse> ok(EmployeeSeparation s) {
        return ResponseEntity.ok(SeparationResponse.from(s));
    }

    private ResponseEntity<List<SeparationResponse>> okList(List<EmployeeSeparation> list) {
        return ResponseEntity.ok(
                list.stream().map(SeparationResponse::from).collect(Collectors.toList()));
    }
}