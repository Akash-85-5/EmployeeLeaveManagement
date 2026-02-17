package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.LeaveResponse;
import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.enums.HalfDayType;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.service.LeaveAllocationService;
import com.example.employeeLeaveApplication.service.LeaveApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaves")
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;
    private final LeaveAllocationService leaveAllocationService;

    public LeaveApplicationController(LeaveApplicationService leaveApplicationService,
                                      LeaveAllocationService leaveAllocationService) {
        this.leaveApplicationService = leaveApplicationService;
        this.leaveAllocationService = leaveAllocationService;
    }

    @Value("${file.upload-dir:uploads/leaves}")
    private String uploadDir;

    // ==================== APPLY LEAVE ====================
    @PostMapping(value = "/apply", consumes = "multipart/form-data")
    public LeaveResponse applyLeave(
            @RequestParam Long employeeId,
            @RequestParam String leaveType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reason,
            @RequestParam(required = false) String halfDayType,
            @RequestParam(defaultValue = "false") boolean confirmLossOfPay,
            @RequestParam(required = false) MultipartFile[] files
    ) throws IOException {

        LeaveType type;
        try {
            type = LeaveType.valueOf(leaveType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave type");
        }

        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(employeeId);
        leave.setLeaveType(type);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);

        if (halfDayType != null && !halfDayType.isEmpty()) {
            leave.setHalfDayType(HalfDayType.valueOf(halfDayType.toUpperCase()));
        }

        // File handling
        if (files != null && files.length > 0) {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            List<LeaveAttachment> attachments = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Files.write(uploadPath.resolve(uniqueName), file.getBytes());
                LeaveAttachment attachment = new LeaveAttachment();
                attachment.setFileUrl(uniqueName);
                attachment.setLeaveApplication(leave);
                attachments.add(attachment);
            }
            leave.setAttachments(attachments);
        }

        LeaveResponse response = leaveApplicationService.applyLeave(leave, confirmLossOfPay);

        // Clean up circular reference for JSON response
        if (response.getLeaveApplication() != null && response.getLeaveApplication().getAttachments() != null) {
            response.getLeaveApplication().getAttachments().forEach(a -> a.setLeaveApplication(null));
        }

        return response;
    }

    // ==================== GET ALL LEAVES (WITH PAGINATION & FILTERS) - NEW ====================
    @GetMapping
    public Page<LeaveApplication> getAllLeaves(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) LeaveType leaveType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return leaveApplicationService.getAllLeaves(employeeId, status, leaveType, startDate, endDate, year, pageable);
    }

    // ==================== GET SINGLE LEAVE BY ID - NEW ====================
    @GetMapping("/{id}")
    public LeaveApplication getLeaveById(@PathVariable Long id) {
        return leaveApplicationService.getLeaveById(id);
    }

    // ==================== GET EMPLOYEE LEAVES (WITH PAGINATION & FILTERS) - UPDATED ====================
    @GetMapping("/employee/{employeeId}")
    public Page<LeaveApplication> getEmployeeLeaves(
            @PathVariable Long employeeId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) LeaveType leaveType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return leaveApplicationService.getLeavesByEmployee(employeeId, status, leaveType, startDate, endDate, year, pageable);
    }

    // ==================== UPDATE LEAVE (BEFORE APPROVAL) - NEW ====================





    @PutMapping("/{id}")
    public LeaveResponse updateLeave(
            @PathVariable Long id,
            @RequestParam Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String halfDayType
    ) {
        return leaveApplicationService.updateLeave(id, employeeId, startDate, endDate, reason, halfDayType);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<String> cancelEmployeeLeave(
            @PathVariable Long id,
            @RequestParam Long employeeId
    ) {
        leaveApplicationService.cancelEmployeeLeave(id, employeeId);
        return ResponseEntity.ok("Leave cancelled successfully.");
    }

    @GetMapping("/attachments/{filename}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading file");
        }
    }

    // ==================== DELETE ATTACHMENT - NEW ====================
    @DeleteMapping("/attachments/{id}")
    public ResponseEntity<String> deleteAttachment(@PathVariable Long id) {
        leaveApplicationService.deleteAttachment(id);
        return ResponseEntity.ok("Attachment deleted successfully.");
    }

    // ==================== LEAVE ALLOCATIONS ====================
    @PostMapping("/allocations")
    public LeaveAllocation createAllocation(@RequestBody LeaveAllocation leaveAllocation) {
        return leaveAllocationService.createEmployeeAllocation(leaveAllocation);
    }

    @GetMapping("/allocations/{employeeId}")
    public List<LeaveAllocation> getEmployeeAllocations(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year
    ) {
        return leaveAllocationService.getEmployeeAllocations(employeeId, year);
    }

    @PutMapping("/allocations/{id}")
    public LeaveAllocation updateAllocation(
            @PathVariable Long id,
            @RequestBody LeaveAllocation leaveAllocation
    ) {
        return leaveAllocationService.updateAllocation(id, leaveAllocation);
    }
}