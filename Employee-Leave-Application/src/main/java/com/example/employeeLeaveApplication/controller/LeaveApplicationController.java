package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.LeaveResponse;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.enums.HalfDayType;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.service.LeaveApplicationService;
import com.example.employeeLeaveApplication.service.LeaveAttachmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;
    private final LeaveAttachmentService leaveAttachmentService;
    private final EmployeeRepository employeeRepository;

    public LeaveApplicationController(LeaveApplicationService leaveApplicationService,
                                      LeaveAttachmentService leaveAttachmentService,
                                      EmployeeRepository employeeRepository) {
        this.leaveApplicationService = leaveApplicationService;
        this.leaveAttachmentService = leaveAttachmentService;
        this.employeeRepository = employeeRepository;
    }
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LeaveResponse> applyLeave(
            @RequestParam Long employeeId,
            @RequestParam String leaveType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reason,
            @RequestParam(required = false) String halfDayType,
            @RequestParam(required = false) String startDateHalfDayType,
            @RequestParam(required = false) String endDateHalfDayType,
            @RequestParam(defaultValue = "false") boolean isAppointment,
            @RequestParam(defaultValue = "false") boolean confirmLossOfPay,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        LeaveType type;
        try {
            type = LeaveType.valueOf(leaveType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave type");
        }

        if (startDate == null) {
            throw new BadRequestException("Start date is required");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(employeeId);
        leave.setEmployeeName(employee.getName());
        leave.setLeaveType(type);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);
        leave.setIsAppointment(isAppointment);

        if (startDateHalfDayType != null && !startDateHalfDayType.isBlank()) {
            try {
                leave.setStartDateHalfDayType(
                        HalfDayType.valueOf(startDateHalfDayType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Invalid startDateHalfDayType: " + startDateHalfDayType);
            }
        }
        if (endDateHalfDayType != null && !endDateHalfDayType.isBlank()) {
            try {
                leave.setEndDateHalfDayType(
                        HalfDayType.valueOf(endDateHalfDayType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Invalid endDateHalfDayType: " + endDateHalfDayType);
            }
        }

        if (halfDayType != null && !halfDayType.isBlank()
                && leave.getStartDateHalfDayType() == null
                && leave.getEndDateHalfDayType() == null) {
            try {
                leave.setStartDateHalfDayType(
                        HalfDayType.valueOf(halfDayType.toUpperCase()));
                leave.setHalfDayType(HalfDayType.valueOf(halfDayType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid half day type: " + halfDayType);
            }
        }

        LocalDate today = LocalDate.now(IST);
        if (type == LeaveType.SICK) {

            if (startDate.isBefore(today)) {
                // Past date: sick leave for past days is not allowed
                throw new BadRequestException(
                        "Sick leave cannot be applied for past dates.");
            }

            if (startDate.isAfter(today)) {
                // Future date: only allowed as a pre-booked appointment with proof
                if (!isAppointment) {
                    throw new BadRequestException(
                            "Sick leave cannot be applied for future dates " +
                                    "unless it is a pre-booked medical appointment. " +
                                    "Please set isAppointment=true and attach a document.");
                }
                if (files == null || files.length == 0) {
                    throw new BadRequestException(
                            "An attachment (appointment proof) is required " +
                                    "for future sick leave applications.");
                }
            }

            // startDate == today: always allowed, no extra checks needed

        } else {
            // All other leave types: cannot apply for past dates
            if (startDate.isBefore(today)) {
                throw new BadRequestException(
                        "Leave cannot be applied for past dates.");
            }
        }

        LeaveResponse response = leaveApplicationService.applyLeave(leave, confirmLossOfPay);

        if (response.getWarning() != null && response.getLeaveApplication() == null) {
            return ResponseEntity.ok(response);
        }

        if (files != null && files.length > 0) {
            Long leaveId = response.getLeaveApplication().getId();
            try {
                leaveAttachmentService.uploadAttachments(leaveId, employeeId, files);
            } catch (Exception e) {
                response.setWarning("Leave applied successfully but file upload failed: "
                        + e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    // ── Get attachments for a leave ───────────────────────────────
    @GetMapping("/{leaveId}/attachments")
    public ResponseEntity<List<LeaveAttachment>> getAttachments(
            @PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveAttachmentService.getAttachments(leaveId));
    }

    // ── Download attachment file ──────────────────────────────────
    @GetMapping("/attachments/download/{filename}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable String filename) {
        try {
            Path filePath = leaveAttachmentService.getFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading file");
        }
    }

    // ── Delete an attachment ──────────────────────────────────────
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<String> deleteAttachment(
            @PathVariable Long attachmentId,
            @RequestParam Long employeeId) {
        leaveAttachmentService.deleteAttachment(attachmentId, employeeId);
        return ResponseEntity.ok("Attachment deleted successfully");
    }

    // ── Get single leave ──────────────────────────────────────────
    @GetMapping("/{id}")
    public LeaveApplication getLeaveById(@PathVariable Long id) {
        return leaveApplicationService.getLeaveById(id);
    }

    // ── Get employee leaves ───────────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public List<LeaveApplication> getEmployeeLeaves(
            @PathVariable Long employeeId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) LeaveType leaveType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return leaveApplicationService.getLeavesByEmployee(employeeId, pageable);
    }

    // ── Update leave ──────────────────────────────────────────────
    @PutMapping("/{id}")
    public LeaveResponse updateLeave(
            @PathVariable Long id,
            @RequestParam Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String startDateHalfDayType,
            @RequestParam(required = false) String endDateHalfDayType) {
        return leaveApplicationService.updateLeave(
                id, employeeId, startDate, endDate, reason,
                startDateHalfDayType, endDateHalfDayType);
    }

    // ── Cancel leave ──────────────────────────────────────────────
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<String> cancelEmployeeLeave(
            @PathVariable Long id,
            @RequestParam Long employeeId) {
        leaveApplicationService.cancelEmployeeLeave(id, employeeId);
        return ResponseEntity.ok("Leave cancelled successfully.");
    }
}