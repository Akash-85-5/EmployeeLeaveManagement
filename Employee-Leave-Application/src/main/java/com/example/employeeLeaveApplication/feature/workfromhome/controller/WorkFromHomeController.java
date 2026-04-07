package com.example.employeeLeaveApplication.feature.workfromhome.controller;

import com.example.employeeLeaveApplication.feature.workfromhome.dto.WorkFromHomeRequest;
import com.example.employeeLeaveApplication.feature.workfromhome.entity.WorkFromHome;
import com.example.employeeLeaveApplication.feature.workfromhome.service.WorkFromHomeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping("/my-requests/{employeeId}")
    public List<WorkFromHome> getMyWFH(@PathVariable Long employeeId) {
        return service.getEmployeeWFH(employeeId);
    }
    @GetMapping("/pending/tl")
    public List<WorkFromHome> getPendingForTL() {
        return service.getPendingForTL();
    }
    // Manager
    @GetMapping("/pending/manager")
    public List<WorkFromHome> getManagerPending() {
        return service.getPendingForManager();
    }
    // HR
    @GetMapping("/pending/hr")
    public List<WorkFromHome> getHRPending() {
        return service.getPendingForHR();
    }
    @PutMapping("/tl/approve/{id}")
    public WorkFromHome approveByTL(@PathVariable Long id) {
        return service.approveByTL(id);
    }

    @PutMapping("/manager/approve/{id}")
    public WorkFromHome approveByManager(@PathVariable Long id) {
        return service.approveByManager(id);
    }

    @PutMapping("/hr/approve/{id}")
    public WorkFromHome approveByHR(@PathVariable Long id) {
        return service.approveByHR(id);
    }
    // TL Reject
    @PutMapping("/tl/reject/{id}")
    public WorkFromHome rejectByTL(@PathVariable Long id) {
        return service.rejectByTL(id);
    }

    // Manager Reject
    @PutMapping("/manager/reject/{id}")
    public WorkFromHome rejectByManager(@PathVariable Long id) {
        return service.rejectByManager(id);
    }

    // HR Reject
    @PutMapping("/hr/reject/{id}")
    public WorkFromHome rejectByHR(@PathVariable Long id) {
        return service.rejectByHR(id);
    }
}