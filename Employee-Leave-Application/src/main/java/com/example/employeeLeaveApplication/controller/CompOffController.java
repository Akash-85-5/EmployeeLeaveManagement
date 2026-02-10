package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.CompOffRequestDTO;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.service.CompOffService;
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

//    @PostMapping("/admin/compoff/request")
//    public ResponseEntity<String> adminRequestCompOff(@RequestBody CompOffRequestDTO request) {
//        validateRequest(request);
//        // Scenario 5: Admin credit is EARNED immediately
//        compOffService.requestBulkCompOff(request, true);
//        return ResponseEntity.ok("Comp-Off recorded and APPROVED by Admin successfully.");
//    }

    @PostMapping("/request")
    public ResponseEntity<String> employeeRequestCompOff(@RequestBody CompOffRequestDTO request) {
        validateRequest(request);
        // Standard Employee request is PENDING
        compOffService.requestBulkCompOff(request, false);
        return ResponseEntity.ok("Comp-Off request submitted and is now PENDING.");
    }

    private void validateRequest(CompOffRequestDTO request) {
        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new BadRequestException("Error: JSON must include an 'entries' array.");
        }
    }

    @PatchMapping("/approve/{id}")
    public ResponseEntity<String> approveCompOff(@PathVariable Long id) {
        compOffService.approveCompOff(id);
        return ResponseEntity.ok("Comp-Off credit approved.");
    }

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(compOffService.getAvailableCompOffDays(employeeId));
    }
}
