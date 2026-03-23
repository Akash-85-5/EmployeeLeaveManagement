package com.example.employeeLeaveApplication.feature.leave.compoff.service;

import com.example.employeeLeaveApplication.feature.holiday.utils.HolidayChecker;
import com.example.employeeLeaveApplication.feature.leave.compoff.dto.CompOffBalanceDetailsDTO;
import com.example.employeeLeaveApplication.feature.leave.compoff.dto.CompOffPendingDTO;
import com.example.employeeLeaveApplication.feature.leave.compoff.dto.CompOffRequestDTO;
import com.example.employeeLeaveApplication.feature.leave.compoff.entity.CompOff;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.shared.enums.CompOffStatus;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.feature.leave.compoff.repository.CompOffRepository;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CompOffService {

    // ===================== EXISTING =====================
    private final CompOffRepository compOffRepository;
    private final HolidayChecker holidayChecker;
    private final CompOffBalanceService balanceService;
    private final EmployeeRepository employeeRepository;

    // ===================== EXISTING =====================
    public CompOffService(CompOffRepository compOffRepository,
                          HolidayChecker holidayChecker,
                          CompOffBalanceService balanceService,
                          EmployeeRepository employeeRepository) {
        this.compOffRepository = compOffRepository;
        this.holidayChecker = holidayChecker;
        this.balanceService = balanceService;
        this.employeeRepository = employeeRepository;
    }

    // ===================== EXISTING =====================
    @Transactional
    public void requestBulkCompOff(CompOffRequestDTO request) {
        if (request.getEmployeeId() == null) {
            throw new BadRequestException("Employee ID is required");
        }

        for (CompOffRequestDTO.CompOffEntry entry : request.getEntries()) {
            if (!holidayChecker.isNonWorkingDay(entry.getWorkedDate())) {
                throw new BadRequestException(
                        "Date " + entry.getWorkedDate()
                                + " is not a holiday/weekend.");
            }

            if (compOffRepository.existsByEmployeeIdAndWorkedDate(
                    request.getEmployeeId(), entry.getWorkedDate())) {
                throw new BadRequestException(
                        "Comp-Off already banked for date: "
                                + entry.getWorkedDate());
            }

            CompOff compOff = new CompOff();
            compOff.setEmployeeId(request.getEmployeeId());
            compOff.setWorkedDate(entry.getWorkedDate());
            compOff.setPlannedLeaveDate(entry.getPlannedLeaveDate());

            Employee employee = employeeRepository
                    .findById(request.getEmployeeId())
                    .orElseThrow(() ->
                            new RuntimeException("Employee Not found"));
            compOff.setManagerId(employee.getManagerId());
            compOff.setDays(entry.getDays());
            compOffRepository.save(compOff);
        }
    }

    // ===================== EXISTING =====================
    @Transactional
    public void approveCompOff(Long id) {
        CompOff compOff = compOffRepository.findById(id)
                .orElseThrow(() ->
                        new BadRequestException("CompOff record not found"));

        if (compOff.getStatus() != CompOffStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING requests can be approved.");
        }

        compOff.setStatus(CompOffStatus.EARNED);
        compOffRepository.save(compOff);
        balanceService.addEarned(compOff.getEmployeeId(), compOff.getDays());
    }

    // ===================== EXISTING =====================
    public BigDecimal getAvailableCompOffDays(Long employeeId) {
        if (employeeId == null) return BigDecimal.ZERO;

        BigDecimal earned = compOffRepository
                .sumDaysByEmployeeAndStatus(employeeId, CompOffStatus.EARNED);
        BigDecimal used = compOffRepository
                .sumDaysByEmployeeAndStatus(employeeId, CompOffStatus.USED);

        earned = (earned != null) ? earned : BigDecimal.ZERO;
        used   = (used   != null) ? used   : BigDecimal.ZERO;

        return earned.subtract(used);
    }

    // ===================== EXISTING =====================
    @Transactional
    public void restoreCompOffBalance(Long employeeId, BigDecimal days) {
        balanceService.restoreUsed(employeeId, days);
    }

    // ===================== EXISTING =====================
    @Transactional
    public void useCompOff(Long employeeId,
                           BigDecimal daysToDeduct,
                           Long leaveApplicationId) {
        BigDecimal remaining = daysToDeduct;

        List<CompOff> earnedList = compOffRepository
                .findByEmployeeIdAndStatusOrderByWorkedDateAsc(
                        employeeId, CompOffStatus.EARNED);

        for (CompOff compOff : earnedList) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal available = compOff.getDays();

            if (available.compareTo(remaining) <= 0) {
                compOff.setStatus(CompOffStatus.USED);
                compOff.setUsedLeaveApplicationId(leaveApplicationId);
                remaining = remaining.subtract(available);
                compOffRepository.save(compOff);
            } else {
                CompOff leftover = new CompOff();
                leftover.setEmployeeId(employeeId);
                leftover.setWorkedDate(compOff.getWorkedDate());
                leftover.setPlannedLeaveDate(compOff.getPlannedLeaveDate());
                leftover.setDays(available.subtract(remaining));
                leftover.setStatus(CompOffStatus.EARNED);
                compOffRepository.save(leftover);

                compOff.setDays(remaining);
                compOff.setStatus(CompOffStatus.USED);
                compOff.setUsedLeaveApplicationId(leaveApplicationId);
                compOffRepository.save(compOff);

                remaining = BigDecimal.ZERO;
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException(
                    "Insufficient balance to deduct "
                            + daysToDeduct + " days.");
        }
        balanceService.addUsed(employeeId, daysToDeduct);
    }

    // ===================== EXISTING =====================
    public CompOff getCompOffRequest(Long id) {
        return compOffRepository.findById(id)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Comp-Off request not found"));
    }

    // ===================== EXISTING =====================
    public void rejectCompOff(Long id, String reason) {
        CompOff compOff = compOffRepository.findById(id)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Comp-Off request not found"));

        if (compOff.getStatus() != CompOffStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING requests can be rejected.");
        }

        compOff.setStatus(CompOffStatus.REJECTED);
        compOff.setRejectionReason(reason);
        compOffRepository.save(compOff);
    }

    // ===================== EXISTING =====================
    public void deleteCompOffRequest(Long id, Long employeeId) {
        CompOff compOff = compOffRepository.findById(id)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Comp-Off request not found"));

        if (!compOff.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException(
                    "You can only delete your own Comp-Off request.");
        }

        if (compOff.getStatus() == CompOffStatus.EARNED) {
            throw new BadRequestException(
                    "Approved Comp-Off cannot be deleted.");
        }

        compOffRepository.delete(compOff);
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added usedDays to response
    // Reason: Show earned, used, available together in one response
    public CompOffBalanceDetailsDTO getCompOffBalanceDetails(
            Long employeeId, Integer year) {

        // ===================== EXISTING =====================
        List<CompOff> approvedList = compOffRepository
                .findListByEmployeeIdAndStatus(
                        employeeId, CompOffStatus.EARNED);

        BigDecimal totalApproved = approvedList.stream()
                .map(CompOff::getDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ NEW: Calculate used days
        // Reason: usedDays was not being returned before
        BigDecimal usedDays = compOffRepository
                .sumDaysByEmployeeAndStatus(
                        employeeId, CompOffStatus.USED);
        usedDays = (usedDays != null) ? usedDays : BigDecimal.ZERO;

        // ===================== EXISTING =====================
        BigDecimal available = getAvailableCompOffDays(employeeId);

        // ✅ UPDATED: Added usedDays param
        return new CompOffBalanceDetailsDTO(
                employeeId,
                year,
                totalApproved,
                usedDays,       // ✅ NEW PARAM
                available
        );
    }

    // ===================== EXISTING =====================
    public Page<CompOff> getCompOffHistory(Long employeeId,
                                           Integer year,
                                           Pageable pageable) {
        if (year != null) {
            return compOffRepository
                    .findByEmployeeIdAndYear(employeeId, year, pageable);
        }
        return compOffRepository.findByEmployeeId(employeeId, pageable);
    }

    // ===================== EXISTING =====================
    public Page<CompOff> getEmployeeCompOffRequests(
            Long employeeId,
            String status,
            Pageable pageable) {

        if (status != null && !status.isBlank()) {
            CompOffStatus compOffStatus;
            try {
                compOffStatus = CompOffStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status value");
            }
            return compOffRepository
                    .findByEmployeeIdAndStatus(
                            employeeId, compOffStatus, pageable);
        }
        return compOffRepository.findByEmployeeId(employeeId, pageable);
    }

    // ===================== EXISTING =====================
    public Page<CompOffPendingDTO> getPendingCompOffApprovals(
            Long managerId, Pageable pageable) {

        return compOffRepository
                .findByManagerIdAndStatus(
                        managerId, CompOffStatus.PENDING, pageable)
                .map(compOff -> {
                    CompOffPendingDTO dto = new CompOffPendingDTO();
                    dto.setCompoffId(compOff.getId());
                    dto.setEmployeeId(compOff.getEmployeeId());
                    Employee employee = employeeRepository
                            .findById(compOff.getEmployeeId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Employee Not Found"));
                    dto.setEmployeeName(employee.getName());
                    dto.setWorkedDate(compOff.getWorkedDate());
                    dto.setStatus(compOff.getStatus());
                    dto.setDays(compOff.getDays());
                    dto.setCreatedAt(compOff.getCreatedAt());
                    return dto;
                });
    }
}