package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.CompOffBalanceDetailsDTO;
import com.example.employeeLeaveApplication.dto.CompOffRequestDTO;
import com.example.employeeLeaveApplication.entity.CompOff;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.service.CompOffService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/compoff")
public class CompOffController {

    private final CompOffService compOffService;

    public CompOffController(CompOffService compOffService) {
        this.compOffService = compOffService;
    }

    // ==================== EMPLOYEE REQUEST COMP-OFF ====================
    @PostMapping("/request")
    public ResponseEntity<String> employeeRequestCompOff(@RequestBody CompOffRequestDTO request) {
        validateRequest(request);
        compOffService.requestBulkCompOff(request, false);
        return ResponseEntity.ok("Comp-Off request submitted and is now PENDING.");
    }

    // ==================== ADMIN REQUEST COMP-OFF (AUTO-APPROVED) - NEW ====================
    @PostMapping("/admin/request")
    public ResponseEntity<String> adminRequestCompOff(@RequestBody CompOffRequestDTO request) {
        validateRequest(request);
        compOffService.requestBulkCompOff(request, true);
        return ResponseEntity.ok("Comp-Off recorded and APPROVED by Admin successfully.");
    }

    // ==================== GET ALL COMP-OFF REQUESTS FOR EMPLOYEE - NEW ====================
    @GetMapping("/requests/{employeeId}")
    public Page<CompOff> getEmployeeCompOffRequests(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getEmployeeCompOffRequests(employeeId, status, pageable);
    }
    // ==================== GET ALL PENDING COMP-OFF APPROVALS - NEW ====================
    @GetMapping("/pending/approvals")
    public Page<CompOff> getPendingCompOffApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getPendingCompOffApprovals(pageable);
    }

    // ==================== GET SINGLE COMP-OFF REQUEST - NEW ====================
    @GetMapping("/{id}")
    public CompOff getCompOffRequest(@PathVariable Long id) {
        return compOffService.getCompOffRequest(id);
    }

    // ==================== APPROVE COMP-OFF ====================
    @PatchMapping("/approve/{id}")
    public ResponseEntity<String> approveCompOff(@PathVariable Long id) {
        compOffService.approveCompOff(id);
        return ResponseEntity.ok("Comp-Off credit approved.");
    }

    // ============ ======== REJECT COMP-OFF - NEW ====================
    @PatchMapping("/reject/{id}")
    public ResponseEntity<String> rejectCompOff(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        compOffService.rejectCompOff(id, reason);
        return ResponseEntity.ok("Comp-Off request rejected.");
    }

    // ==================== DELETE COMP-OFF REQUEST - NEW ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCompOffRequest(
            @PathVariable Long id,
            @RequestParam Long employeeId
    ) {
        compOffService.deleteCompOffRequest(id, employeeId);
        return ResponseEntity.ok("Comp-Off request deleted successfully.");
    }

    // ==================== GET COMP-OFF BALANCE ====================
    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(compOffService.getAvailableCompOffDays(employeeId));
    }

    // ==================== GET COMP-OFF BALANCE DETAILS - NEW ====================
    @GetMapping("/balance/{employeeId}/details")
    public ResponseEntity<CompOffBalanceDetailsDTO> getBalanceDetails(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(compOffService.getCompOffBalanceDetails(employeeId, year));
    }

    // ==================== GET COMP-OFF HISTORY - NEW ====================
    @GetMapping("/history/{employeeId}")
    public Page<CompOff> getCompOffHistory(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getCompOffHistory(employeeId, year, pageable);
    }

    // ==================== VALIDATION ====================
    private void validateRequest(CompOffRequestDTO request) {
        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new BadRequestException("Error: JSON must include an 'entries' array.");
        }
    }
}