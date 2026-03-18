package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.WorkFromHomeRequest;
import com.example.employeeLeaveApplication.entity.WorkFromHome;
import com.example.employeeLeaveApplication.service.WorkFromHomeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wfh")
public class WorkFromHomeController {

    private final WorkFromHomeService service;

    public WorkFromHomeController(WorkFromHomeService service) {
        this.service = service;
    }

    @PostMapping("/apply")
    public WorkFromHome applyWFH(@RequestBody WorkFromHomeRequest request,
                                 @RequestParam Long employeeId) {

        return service.applyWFH(employeeId, request);
    }
}