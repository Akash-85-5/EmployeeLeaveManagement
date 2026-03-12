package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.ODRequest;
import com.example.employeeLeaveApplication.service.ODService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/od")
@CrossOrigin(origins = "*") // Adjust this for your frontend URL
public class ODRequestController {

    private final ODService odService;

    public ODRequestController(ODService odService) {
        this.odService = odService;
    }

    // 1. Create a new OD Request
    // POST /api/od/request?employeeId=1
    @PostMapping("/request")
    public ResponseEntity<ODRequest> createOD(@RequestParam Long employeeId,
                                              @RequestBody ODRequest request) {
        return ResponseEntity.ok(odService.createOD(employeeId, request));
    }

    // 2. Approve an OD Request
    // PUT /api/od/approve/5?approverId=2
    @PutMapping("/approve/{odId}")
    public ResponseEntity<ODRequest> approveOD(@PathVariable Long odId,
                                               @RequestParam Long approverId) {
        return ResponseEntity.ok(odService.approveOD(odId, approverId));
    }

    // 3. Reject an OD Request
    // PUT /api/od/reject/5?approverId=2
    @PutMapping("/reject/{odId}")
    public ResponseEntity<ODRequest> rejectOD(@PathVariable Long odId,
                                              @RequestParam Long approverId) {
        return ResponseEntity.ok(odService.rejectOD(odId, approverId));
    }

    // 4. Cancel an OD Request (by User or Approver)
    // PUT /api/od/cancel/5?userId=1
    @PutMapping("/cancel/{odId}")
    public ResponseEntity<ODRequest> cancelOD(@PathVariable Long odId,
                                              @RequestParam Long userId) {
        return ResponseEntity.ok(odService.cancelOD(odId, userId));
    }
}