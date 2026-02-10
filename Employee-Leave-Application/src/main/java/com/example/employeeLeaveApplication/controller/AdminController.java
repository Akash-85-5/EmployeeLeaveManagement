package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.service.CarryForwardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Slf4j

public class AdminController {

    private final CarryForwardService carryForwardService;

    public AdminController(CarryForwardService carryForwardService){
        this.carryForwardService=carryForwardService;
    }

    @PostMapping("/carry-forward")
    public ResponseEntity<String> processCarryForward(
            @RequestParam Integer fromYear) {

        log.info("[API] POST carry-forward: fromYear={}", fromYear);

        try {
            carryForwardService.processYearEndCarryForward(fromYear);
            return ResponseEntity.ok(
                    "Carry forward processed successfully for year " + fromYear);
        } catch (Exception e) {
            log.error("[API] Carry forward failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/carry-forward/{employeeId}")
    public ResponseEntity<String> processEmployeeCarryForward(
            @PathVariable Long employeeId,
            @RequestParam Integer fromYear) {

        log.info("[API] POST carry-forward: employee={}, fromYear={}", employeeId, fromYear);

        try {
            carryForwardService.processEmployeeCarryForward(employeeId, fromYear);
            return ResponseEntity.ok(
                    "Carry forward processed for employee " + employeeId);
        } catch (Exception e) {
            log.error("[API] Carry forward failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Carry forward failed: " + e.getMessage());
        }
    }
}
