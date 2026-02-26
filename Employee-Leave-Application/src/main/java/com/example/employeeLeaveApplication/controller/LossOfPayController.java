package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.ApplyLopRequest;
import com.example.employeeLeaveApplication.entity.LossOfPayRecord;
import com.example.employeeLeaveApplication.service.LossOfPayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lop")
@RequiredArgsConstructor
public class  LossOfPayController {

    private final LossOfPayService lossOfPayService;


    @PostMapping("/apply")
    public ResponseEntity<String> apply(@Valid @RequestBody ApplyLopRequest req) {
        lossOfPayService.applyLossOfPay(
                req.getEmployeeId(),
                req.getYear(),
                req.getMonth(),
                req.getExcessDays()
        );
        return ResponseEntity.ok("LOP applied");
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LossOfPayRecord>> listByEmployee(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(lossOfPayService.getAllForEmployee(employeeId));
    }

    @GetMapping("/employee/{employeeId}/year/{year}")
    public ResponseEntity<List<LossOfPayRecord>> listByEmployeeYear(
            @PathVariable Long employeeId,
            @PathVariable Integer year) {
        return ResponseEntity.ok(lossOfPayService.getForEmployeeAndYear(employeeId, year));
    }

    // ✅ ADDED: Missing endpoint for restoreLossOfPay() service method
    @DeleteMapping("/employee/{employeeId}/year/{year}/month/{month}")
    public ResponseEntity<String> restoreLop(
            @PathVariable Long employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month) {
        lossOfPayService.restoreLossOfPay(employeeId, year, month);
        return ResponseEntity.ok("LOP record restored successfully");
    }
}