package com.example.employeeLeaveApplication.feature.leave.annual.controller;

import com.example.employeeLeaveApplication.feature.leave.annual.dto.LeaveResponse;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.shared.enums.HalfDayType;
import com.example.employeeLeaveApplication.shared.enums.LeaveStatus;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.service.LeaveApplicationService;
import com.example.employeeLeaveApplication.feature.leave.annual.service.LeaveAttachmentService;
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
    private final LeaveAttachmentService  leaveAttachmentService;
    private final EmployeeRepository      employeeRepository;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public LeaveApplicationController(LeaveApplicationService leaveApplicationService,
                                      LeaveAttachmentService leaveAttachmentService,
                                      EmployeeRepository employeeRepository) {
        this.leaveApplicationService = leaveApplicationService;
        this.leaveAttachmentService  = leaveAttachmentService;
        this.employeeRepository      = employeeRepository;
    }

    // ── Apply leave ───────────────────────────────────────────────
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
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        LeaveType type;
        try {
            type = LeaveType.valueOf(leaveType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave type: " + leaveType);
        }

        if (startDate == null) {
            throw new BadRequestException("Start date is required");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LocalDate today = LocalDate.now(IST);

        // ── Date rules per leave type ─────────────────────────────
        if (type == LeaveType.SICK) {
            if (startDate.isBefore(today)) {
                throw new BadRequestException("Sick leave cannot be applied for past dates.");
            }
            if (startDate.isAfter(today)) {
                if (!isAppointment) {
                    throw new BadRequestException(
                            "Sick leave for future dates requires isAppointment=true "
                                    + "and an attachment as proof.");
                }
                if (files == null || files.length == 0) {
                    throw new BadRequestException(
                            "An attachment (appointment proof) is required for future sick leave.");
                }
            }
        } else if (type == LeaveType.MATERNITY || type == LeaveType.PATERNITY) {
            // Maternity/Paternity can be applied for future dates (planned)
            // No past-date restriction here; service validates allocation
        } else {
            // ANNUAL_LEAVE, COMP_OFF: no past dates
            if (startDate.isBefore(today)) {
                throw new BadRequestException("Leave cannot be applied for past dates.");
            }
        }

        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(employeeId);
        leave.setEmployeeName(employee.getName());
        leave.setLeaveType(type);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);
        leave.setIsAppointment(isAppointment);

        // ── Half-day parsing (logic unchanged) ───────────────────
        if (startDateHalfDayType != null && !startDateHalfDayType.isBlank()) {
            try {
                leave.setStartDateHalfDayType(
                        HalfDayType.valueOf(startDateHalfDayType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid startDateHalfDayType: " + startDateHalfDayType);
            }
        }
        if (endDateHalfDayType != null && !endDateHalfDayType.isBlank()) {
            try {
                leave.setEndDateHalfDayType(
                        HalfDayType.valueOf(endDateHalfDayType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid endDateHalfDayType: " + endDateHalfDayType);
            }
        }
        // Backward-compatible: if only halfDayType is provided
        if (halfDayType != null && !halfDayType.isBlank()
                && leave.getStartDateHalfDayType() == null
                && leave.getEndDateHalfDayType() == null) {
            try {
                HalfDayType hdt = HalfDayType.valueOf(halfDayType.toUpperCase());
                leave.setStartDateHalfDayType(hdt);
                leave.setHalfDayType(hdt);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid half day type: " + halfDayType);
            }
        }

        LeaveResponse response = leaveApplicationService.applyLeave(leave);

        // Upload attachments if any
        if (response.getLeaveApplication() != null && files != null && files.length > 0) {
            Long leaveId = response.getLeaveApplication().getId();
            try {
                leaveAttachmentService.uploadAttachments(leaveId, employeeId, files);
            } catch (Exception e) {
                response.setWarning("Leave applied but file upload failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    // ── Get attachments for a leave ───────────────────────────────
    @GetMapping("/{leaveId}/attachments")
    public ResponseEntity<List<LeaveAttachment>> getAttachments(@PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveAttachmentService.getAttachments(leaveId));
    }

    // ── Download attachment ───────────────────────────────────────
    @GetMapping("/attachments/download/{filename}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String filename) {
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading file");
        }
    }

    // ── Delete attachment ─────────────────────────────────────────
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

    // ── Get employee's leaves ─────────────────────────────────────
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

    // ── Update leave (before approval) ───────────────────────────
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