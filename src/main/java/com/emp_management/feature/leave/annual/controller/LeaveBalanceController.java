package com.emp_management.feature.leave.annual.controller;

import com.emp_management.feature.leave.annual.dto.AnnualLeaveBalanceResponse;
import com.emp_management.feature.leave.annual.dto.LeaveBalanceSummaryResponse;
import com.emp_management.feature.leave.annual.dto.SickLeaveBalanceResponse;
import com.emp_management.feature.leave.annual.service.AnnualLeaveBalanceService;
import com.emp_management.feature.leave.annual.service.SickLeaveBalanceService;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardBalanceResponse;
import com.emp_management.feature.leave.carryforward.mapper.CarryForwardBalanceMapper;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave/balance")
public class LeaveBalanceController {

    private final AnnualLeaveBalanceService annualService;
    private final SickLeaveBalanceService sickService;
    private final CarryForwardBalanceRepository carryForwardRepo;

    public LeaveBalanceController(AnnualLeaveBalanceService annualService,
                                  SickLeaveBalanceService sickService,
                                  CarryForwardBalanceRepository carryForwardRepo) {
        this.annualService = annualService;
        this.sickService = sickService;
        this.carryForwardRepo = carryForwardRepo;
    }

    // ═══════════════════════════════════════════════════════════════
    // ANNUAL LEAVE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/{employeeId}/annual")
    public ResponseEntity<AnnualLeaveBalanceResponse> getAnnualBalance(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        LocalDate today = LocalDate.now();
        int resolvedYear = (year != null) ? year : today.getYear();
        int resolvedMonth = (month != null) ? month : today.getMonthValue();

        // Uses new DTO method (same logic internally)
        AnnualLeaveBalanceResponse response =
                annualService.getSingleMonthDTO(employeeId, resolvedYear, resolvedMonth);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}/annual/summary")
    public ResponseEntity<List<AnnualLeaveBalanceResponse>> getAnnualYearSummary(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {

        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();

        List<AnnualLeaveBalanceResponse> response =
                annualService.getYearSummaryDTO(employeeId, resolvedYear);

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════
    // SICK LEAVE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/{employeeId}/sick")
    public ResponseEntity<SickLeaveBalanceResponse> getSickBalance(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        LocalDate today = LocalDate.now();
        int resolvedYear = (year != null) ? year : today.getYear();
        int resolvedMonth = (month != null) ? month : today.getMonthValue();

        // Uses new DTO method
        SickLeaveBalanceResponse response =
                sickService.getSingleMonthDTO(employeeId, resolvedYear, resolvedMonth);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}/sick/summary")
    public ResponseEntity<List<SickLeaveBalanceResponse>> getSickYearSummary(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {

        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();

        List<SickLeaveBalanceResponse> response =
                sickService.getYearSummaryDTO(employeeId, resolvedYear);

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════
    // COMBINED SUMMARY
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/{employeeId}/summary")
    public ResponseEntity<LeaveBalanceSummaryResponse> getCombinedSummary(
            @PathVariable String employeeId,
            @RequestParam(required = false) Integer year) {

        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();

        List<AnnualLeaveBalanceResponse> annualList =
                annualService.getYearSummaryDTO(employeeId, resolvedYear);

        List<SickLeaveBalanceResponse> sickList =
                sickService.getYearSummaryDTO(employeeId, resolvedYear);

        CarryForwardBalanceResponse carryForwardDto =
                carryForwardRepo
                        .findByEmployee_EmpIdAndYear(employeeId, resolvedYear)
                        .map(CarryForwardBalanceMapper::toDTO)
                        .orElseGet(() -> {
                            CarryForwardBalanceResponse empty = new CarryForwardBalanceResponse();
                            empty.setEmployeeId(employeeId);
                            empty.setYear(resolvedYear);
                            empty.setRemaining(0.0);
                            return empty;
                        });

        LeaveBalanceSummaryResponse summary = new LeaveBalanceSummaryResponse(
                employeeId,
                resolvedYear,
                annualList,
                sickList,
                carryForwardDto
        );

        return ResponseEntity.ok(summary);
    }
}