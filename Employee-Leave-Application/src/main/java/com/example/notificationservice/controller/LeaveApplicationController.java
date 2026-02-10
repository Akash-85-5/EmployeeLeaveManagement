package com.example.notificationservice.controller;

import com.example.notificationservice.dto.LeaveResponse;
import com.example.notificationservice.entity.LeaveAllocation;
import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.entity.LeaveAttachment;
import com.example.notificationservice.enums.HalfDayType;
import com.example.notificationservice.enums.LeaveType;
import com.example.notificationservice.service.LeaveAllocationService;
import com.example.notificationservice.service.LeaveApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
                                      LeaveAllocationService leaveAllocationService){
        this.leaveApplicationService=leaveApplicationService;
        this.leaveAllocationService=leaveAllocationService;
    }


    @Value("${file.upload-dir:uploads/leaves}")
    private String uploadDir;


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

        // 🔹 Set status to PENDING to match DB constraints
        leave.setStatus(com.example.notificationservice.enums.LeaveStatus.PENDING);

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

        // Clean circular references for JSON
        if (response.getLeaveApplication() != null && response.getLeaveApplication().getAttachments() != null) {
            response.getLeaveApplication().getAttachments().forEach(a -> a.setLeaveApplication(null));
        }

        return response;
    }


    @GetMapping("/employee/{employeeId}")
    public List<LeaveApplication> getEmployeeLeaves(@PathVariable Long employeeId) {
        return leaveApplicationService.getLeavesByEmployee(employeeId);
    }

    @PostMapping ("/cancel/{id}")
    public ResponseEntity<String> cancelEmployeeLeave(
            @PathVariable Long id,
            @RequestParam Long employeeId
    ) {
        leaveApplicationService.cancelEmployeeLeave(id, employeeId);
        return ResponseEntity.ok("Leave cancelled successfully.");
    }

    @PostMapping("/allocations")
    public LeaveAllocation create(@RequestBody LeaveAllocation leaveAllocation){
        return leaveAllocationService.createEmployeeAllocation(leaveAllocation);
    }
}
