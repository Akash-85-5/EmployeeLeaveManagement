package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.CompOffBalanceDetailsDTO;
import com.example.employeeLeaveApplication.dto.CompOffPendingDTO;
import com.example.employeeLeaveApplication.dto.CompOffRequestDTO;
import com.example.employeeLeaveApplication.entity.CompOff;
import com.example.employeeLeaveApplication.entity.CompOffBalance;         // ✅ NEW IMPORT
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.service.CompOffBalanceService; // ✅ NEW IMPORT
import com.example.employeeLeaveApplication.service.CompOffService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List; // ✅ NEW IMPORT
import java.util.Map;  // ✅ NEW IMPORT

// ===================== EXISTING =====================
@RestController
@RequestMapping("/api/compoff")
public class CompOffController {

    // ===================== EXISTING =====================
    private final CompOffService compOffService;

    // ✅ NEW FIELD
    // Reason: Needed to call balance methods for HR/Admin/Manager
    private final CompOffBalanceService compOffBalanceService;

    // ===================== EXISTING (UPDATED) =====================
    // Added CompOffBalanceService to constructor
    public CompOffController(CompOffService compOffService,
                             CompOffBalanceService compOffBalanceService) {
        this.compOffService = compOffService;
        this.compOffBalanceService = compOffBalanceService; // ✅ NEW LINE
    }

    // ===================== EXISTING =====================
    @PostMapping("/request")
    public ResponseEntity<String> employeeRequestCompOff(
            @RequestBody CompOffRequestDTO request) {
        validateRequest(request);
        compOffService.requestBulkCompOff(request);
        return ResponseEntity.ok(
                "Comp-Off request submitted and is now PENDING.");
    }

    // ===================== EXISTING =====================
    @GetMapping("/requests/{employeeId}")
    public Page<CompOff> getEmployeeCompOffRequests(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getEmployeeCompOffRequests(
                employeeId, status, pageable);
    }

    // ===================== EXISTING =====================
    @GetMapping("/pending/{managerId}/approvals")
    @PreAuthorize("#managerId == authentication.principal.user.id")
    public Page<CompOffPendingDTO> getPendingCompOffApprovals(
            @PathVariable Long managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getPendingCompOffApprovals(
                managerId, pageable);
    }

    // ===================== EXISTING =====================
    @GetMapping("/record/{id}")
    public CompOff getCompOffRequest(@PathVariable Long id) {
        return compOffService.getCompOffRequest(id);
    }


    // ===================== EXISTING (UPDATED) =====================
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
    // ===================== EXISTING =====================
    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("#id == authentication.principal.user.id")
    public ResponseEntity<String> deleteCompOffRequest(
            @PathVariable Long id,
            @RequestParam Long employeeId) {
        compOffService.deleteCompOffRequest(id, employeeId);
        return ResponseEntity.ok("Comp-Off request deleted successfully.");
    }

    // ===================== EXISTING (UPDATED) =====================
    // Was: #employeeId == authentication.principal.user.id (employee only)
    // Now: Employee(own) + HR + ADMIN + MANAGER can all view balance
    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(
                compOffService.getAvailableCompOffDays(employeeId));
    }

    // ===================== EXISTING (UPDATED) =====================
    // Was: #employeeId == authentication.principal.user.id (employee only)
    // Now: Employee(own) + HR + ADMIN + MANAGER can all view details
    // Also: Now returns usedDays in response too
    @GetMapping("/balance/{employeeId}/details")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<CompOffBalanceDetailsDTO> getBalanceDetails(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(
                compOffService.getCompOffBalanceDetails(employeeId, year));
    }

    // ===================== EXISTING (UPDATED) =====================
    // Was: #employeeId == authentication.principal.user.id (employee only)
    // Now: Employee(own) + HR + ADMIN + MANAGER can all view history
    @GetMapping("/history/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id " +
            "or hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    public Page<CompOff> getCompOffHistory(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getCompOffHistory(employeeId, year, pageable);
    }

    // ===================== EXISTING =====================
    private void validateRequest(CompOffRequestDTO request) {
        if (request.getEntries() == null
                || request.getEntries().isEmpty()) {
            throw new BadRequestException(
                    "Error: JSON must include an 'entries' array.");
        }
    }
}