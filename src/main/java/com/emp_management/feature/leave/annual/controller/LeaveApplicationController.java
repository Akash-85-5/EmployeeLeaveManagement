package com.emp_management.feature.leave.annual.controller;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.dto.LeaveResponse;
import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.entity.LeaveAttachment;
import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.feature.leave.annual.repository.LeaveTypeRepository;
import com.emp_management.feature.leave.annual.service.LeaveApplicationService;
import com.emp_management.feature.leave.annual.service.LeaveAttachmentService;
import com.emp_management.shared.enums.HalfDayType;
import com.emp_management.shared.enums.RequestStatus;
import com.emp_management.shared.exceptions.BadRequestException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public LeaveApplicationController(LeaveApplicationService leaveApplicationService,
                                      LeaveAttachmentService leaveAttachmentService,
                                      EmployeeRepository employeeRepository,
                                      LeaveTypeRepository leaveTypeRepository) {
        this.leaveApplicationService = leaveApplicationService;
        this.leaveAttachmentService  = leaveAttachmentService;
        this.employeeRepository      = employeeRepository;
        this.leaveTypeRepository     = leaveTypeRepository;
    }

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LeaveResponse> applyLeave(
            @RequestParam String employeeId,              // now String (emp_code)
            @RequestParam Long leaveTypeId,               // FK to LeaveType entity
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reason,
            @RequestParam(required = false) String halfDayType,
            @RequestParam(required = false) String startDateHalfDayType,
            @RequestParam(required = false) String endDateHalfDayType,
            @RequestParam(defaultValue = "false") boolean isAppointment,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found: " + employeeId));

        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid leaveTypeId: " + leaveTypeId));

        if (startDate == null) throw new BadRequestException("Start date is required");

        LocalDate today   = LocalDate.now(IST);
        String   typeName = leaveType.getLeaveType().toUpperCase();

        // ── Date rules ────────────────────────────────────────────
        if ("SICK".equals(typeName)) {
            if (startDate.isBefore(today))
                throw new BadRequestException("Sick leave cannot be applied for past dates.");
            if (startDate.isAfter(today)) {
                if (!isAppointment)
                    throw new BadRequestException(
                            "Sick leave for future dates requires isAppointment=true.");
                if (files == null || files.length == 0)
                    throw new BadRequestException(
                            "An attachment (appointment proof) is required for future sick leave.");
            }
        } else if (!"MATERNITY".equals(typeName) && !"PATERNITY".equals(typeName)) {
            if (startDate.isBefore(today))
                throw new BadRequestException("Leave cannot be applied for past dates.");
        }

        LeaveApplication leave = new LeaveApplication();
        leave.setEmployee(employee);
        leave.setLeaveType(leaveType);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);
        leave.setIsAppointment(isAppointment);

        // ── Half-day parsing ──────────────────────────────────────
        if (startDateHalfDayType != null && !startDateHalfDayType.isBlank()) {
            try { leave.setStartDateHalfDayType(HalfDayType.valueOf(startDateHalfDayType.toUpperCase())); }
            catch (IllegalArgumentException e) { throw new BadRequestException("Invalid startDateHalfDayType: " + startDateHalfDayType); }
        }
        if (endDateHalfDayType != null && !endDateHalfDayType.isBlank()) {
            try { leave.setEndDateHalfDayType(HalfDayType.valueOf(endDateHalfDayType.toUpperCase())); }
            catch (IllegalArgumentException e) { throw new BadRequestException("Invalid endDateHalfDayType: " + endDateHalfDayType); }
        }
        // Backward-compatible: single halfDayType param
        if (halfDayType != null && !halfDayType.isBlank()
                && leave.getStartDateHalfDayType() == null
                && leave.getEndDateHalfDayType()   == null) {
            try {
                HalfDayType hdt = HalfDayType.valueOf(halfDayType.toUpperCase());
                leave.setStartDateHalfDayType(hdt);
                leave.setHalfDayType(hdt);
            } catch (IllegalArgumentException e) { throw new BadRequestException("Invalid halfDayType: " + halfDayType); }
        }

        LeaveResponse response = leaveApplicationService.applyLeave(leave);

        if (response.getLeaveApplication() != null && files != null && files.length > 0) {
            try {
                leaveAttachmentService.uploadAttachments(
                        response.getLeaveApplication().getId(), employeeId, files);
            } catch (Exception e) {
                response.setWarning("Leave applied but file upload failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{leaveId}/attachments")
    public ResponseEntity<List<LeaveAttachment>> getAttachments(@PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveAttachmentService.getAttachments(leaveId));
    }

    @GetMapping("/attachments/download/{filename}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String filename) {
        try {
            Path filePath   = leaveAttachmentService.getFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists())
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
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

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<String> deleteAttachment(
            @PathVariable Long attachmentId,
            @RequestParam String employeeId) {
        leaveAttachmentService.deleteAttachment(attachmentId, employeeId);
        return ResponseEntity.ok("Attachment deleted successfully");
    }

    @GetMapping("/{id}")
    public LeaveApplication getLeaveById(@PathVariable Long id) {
        return leaveApplicationService.getLeaveById(id);
    }

    @GetMapping("/employee/{employeeId}")
    public List<LeaveApplication> getEmployeeLeaves(
            @PathVariable String employeeId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return leaveApplicationService.getLeavesByEmployee(employeeId, pageable);
    }

    @PutMapping("/{id}")
    public LeaveResponse updateLeave(
            @PathVariable Long id,
            @RequestParam String employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String startDateHalfDayType,
            @RequestParam(required = false) String endDateHalfDayType) {
        return leaveApplicationService.updateLeave(
                id, employeeId, startDate, endDate, reason,
                startDateHalfDayType, endDateHalfDayType);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<String> cancelEmployeeLeave(
            @PathVariable Long id,
            @RequestParam String employeeId) {
        leaveApplicationService.cancelEmployeeLeave(id, employeeId);
        return ResponseEntity.ok("Leave cancelled successfully.");
    }
}
