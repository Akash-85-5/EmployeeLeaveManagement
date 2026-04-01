package com.emp_management.feature.leave.compoff.controller;

import com.emp_management.feature.leave.compoff.dto.CompOffBalanceDetailsDTO;
import com.emp_management.feature.leave.compoff.dto.CompOffPendingDTO;
import com.emp_management.feature.leave.compoff.dto.CompOffRequestDTO;
import com.emp_management.feature.leave.compoff.entity.CompOff;
import com.emp_management.feature.leave.compoff.entity.CompOffBalance;
import com.emp_management.feature.leave.compoff.service.CompOffBalanceService;
import com.emp_management.feature.leave.compoff.service.CompOffService;
import com.emp_management.shared.exceptions.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/compoff")
public class CompOffController {

    private final CompOffService compOffService;
    private final CompOffBalanceService compOffBalanceService;

    public CompOffController(CompOffService compOffService,
                             CompOffBalanceService compOffBalanceService) {
        this.compOffService = compOffService;
        this.compOffBalanceService = compOffBalanceService;
    }

    // ==================== SUBMIT REQUEST ====================

    // ✅ EMPLOYEE, TEAM_LEADER, MANAGER, ADMIN can submit
    @PostMapping("/request")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('TEAM_LEADER') " +
            "or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<String> employeeRequestCompOff(
            @RequestBody CompOffRequestDTO request) {
        validateRequest(request);
        compOffService.requestBulkCompOff(request);
        return ResponseEntity.ok(
                "Comp-Off request submitted and is now PENDING.");
    }

    // ==================== VIEW REQUESTS ====================

    @GetMapping("/requests/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public Page<CompOff> getEmployeeCompOffRequests(
            @PathVariable String employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getEmployeeCompOffRequests(
                employeeId, status, pageable);
    }

    // ==================== PENDING APPROVALS ====================

    @GetMapping("/pending/{managerId}/approvals")
    @PreAuthorize("hasRole('MANAGER') and " +
            "#managerId == authentication.principal.user.id")
    public Page<CompOffPendingDTO> getPendingCompOffApprovals(
            @PathVariable String  managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getPendingCompOffApprovals(
                managerId, pageable);
    }

    // ==================== VIEW SINGLE RECORD ====================

    @GetMapping("/record/{id}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public CompOff getCompOffRequest(@PathVariable Long id) {
        return compOffService.getCompOffRequest(id);
    }

    // ==================== APPROVE / REJECT ====================

    @PatchMapping("/approve/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<String> approveCompOff(@PathVariable Long id) {
        compOffService.approveCompOff(id);
        return ResponseEntity.ok("Comp-Off credit approved.");
    }

    @PatchMapping("/reject/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<String> rejectCompOff(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        compOffService.rejectCompOff(id, reason);
        return ResponseEntity.ok("Comp-Off request rejected.");
    }

    // ==================== CANCEL ====================

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('TEAM_LEADER')")
    public ResponseEntity<String> deleteCompOffRequest(
            @PathVariable Long id,
            @RequestParam Long employeeId) {
        compOffService.deleteCompOffRequest(id, employeeId);
        return ResponseEntity.ok(
                "Comp-Off request deleted successfully.");
    }

    // ==================== BALANCE ====================

    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable String employeeId) {
        return ResponseEntity.ok(
                compOffService.getAvailableCompOffDays(employeeId));
    }

    @GetMapping("/balance/{employeeId}/details")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<CompOffBalanceDetailsDTO> getBalanceDetails(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(
                compOffService.getCompOffBalanceDetails(employeeId, year));
    }

    // ==================== HISTORY ====================

    @GetMapping("/history/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public Page<CompOff> getCompOffHistory(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getCompOffHistory(
                employeeId, year, pageable);
    }

    // ==================== BALANCE BY YEAR ====================

    @GetMapping("/balance/{employeeId}/year/{year}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<CompOffBalance> getBalanceByYear(
            @PathVariable String employeeId,
            @PathVariable Integer year) {
        return ResponseEntity.ok(
                compOffBalanceService.getBalance(employeeId, year));
    }

    @GetMapping("/balance/{employeeId}/all-years")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') " +
            "or hasRole('MANAGER') or hasRole('TEAM_LEADER')")
    public ResponseEntity<List<CompOffBalance>> getAllBalances(
            @PathVariable String  employeeId) {
        return ResponseEntity.ok(
                compOffBalanceService.getAllByEmployee(employeeId));
    }

    // ==================== PRIVATE ====================

    private void validateRequest(CompOffRequestDTO request) {
        if (request.getEntries() == null
                || request.getEntries().isEmpty()) {
            throw new BadRequestException(
                    "Error: JSON must include an 'entries' array.");
        }
    }
}