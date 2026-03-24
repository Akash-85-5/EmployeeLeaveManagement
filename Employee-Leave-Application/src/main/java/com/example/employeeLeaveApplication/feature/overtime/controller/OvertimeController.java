package com.example.employeeLeaveApplication.feature.overtime.controller;

import com.example.employeeLeaveApplication.feature.overtime.entity.OvertimeRequest;
import com.example.employeeLeaveApplication.feature.overtime.service.OvertimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @PostMapping("/submit")
    public OvertimeRequest create(@ModelAttribute OvertimeRequest request,
                                  @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
        // Replace these with actual session user details
        Long currentUserId = 101L;
        String currentUserRole = "EMPLOYEE";

        return overtimeService.submitRequest(currentUserId, currentUserRole, request, file);
    }

    @PostMapping("/approve/{id}")
    public OvertimeRequest approve(@PathVariable Long id) {
        // Replace with actual session user details
        Long approverId = 202L;
        String approverRole = "TL";

        return overtimeService.approve(id, approverId, approverRole);
    }
}