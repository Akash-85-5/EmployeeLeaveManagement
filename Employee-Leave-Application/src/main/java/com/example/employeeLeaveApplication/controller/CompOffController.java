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

    @PostMapping("/request")
    public ResponseEntity<String> employeeRequestCompOff(@RequestBody CompOffRequestDTO request) {
        validateRequest(request);
        compOffService.    requestBulkCompOff(request);
        return ResponseEntity.ok("Comp-Off request submitted and is now PENDING.");
    }

//    @PostMapping("/admin/request")
//    public ResponseEntity<String> adminRequestCompOff(@RequestBody CompOffRequestDTO request) {
//        validateRequest(request);
//        compOffService.requestBulkCompOff(request);
//        return ResponseEntity.ok("Comp-Off recorded and APPROVED by Admin successfully.");
//    }

    @GetMapping("/requests/{employeeId}")
    public Page<CompOff> getEmployeeCompOffRequests(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getEmployeeCompOffRequests(employeeId, status, pageable);
    }

    @GetMapping("/pending/{managerId}/approvals")
    public Page<CompOff> getPendingCompOffApprovals(
            @PathVariable Long managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getPendingCompOffApprovals(managerId,pageable);
    }


    // ✅ FIXED: renamed from /{id} to /record/{id} to avoid conflict with /balance/{id}
    @GetMapping("/record/{id}")
    public CompOff getCompOffRequest(@PathVariable Long id) {
        return compOffService.getCompOffRequest(id);
    }

    @PatchMapping("/approve/{id}")
    public ResponseEntity<String> approveCompOff(@PathVariable Long id) {
        compOffService.approveCompOff(id);
        return ResponseEntity.ok("Comp-Off credit approved.");
    }

    @PatchMapping("/reject/{id}")
    public ResponseEntity<String> rejectCompOff(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        compOffService.rejectCompOff(id, reason);
        return ResponseEntity.ok("Comp-Off request rejected.");
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<String> deleteCompOffRequest(
            @PathVariable Long id,
            @RequestParam Long employeeId) {
        compOffService.deleteCompOffRequest(id, employeeId);
        return ResponseEntity.ok("Comp-Off request deleted successfully.");
    }

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(compOffService.getAvailableCompOffDays(employeeId));
    }

    @GetMapping("/balance/{employeeId}/details")
    public ResponseEntity<CompOffBalanceDetailsDTO> getBalanceDetails(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(compOffService.getCompOffBalanceDetails(employeeId, year));
    }

    @GetMapping("/history/{employeeId}")
    public Page<CompOff> getCompOffHistory(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return compOffService.getCompOffHistory(employeeId, year, pageable);
    }

    private void validateRequest(CompOffRequestDTO request) {
        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new BadRequestException("Error: JSON must include an 'entries' array.");
        }
    }
}