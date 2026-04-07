package com.emp_management.feature.workfromhome.controller;

import com.emp_management.feature.workfromhome.entity.WfhRequest;
import com.emp_management.feature.workfromhome.repository.WfhRepository;
import com.emp_management.feature.workfromhome.service.WfhService;
import com.emp_management.shared.enums.HalfDayType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/wfh")
public class WfhController {

    private final WfhService service;


    public WfhController(WfhService service) {
        this.service = service;
    }

    // APPLY
    @PostMapping("/apply")
    public WfhRequest apply(
            @RequestParam String empId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam String reason,
            @RequestParam(required = false) HalfDayType halfDayType
    ) {

        return service.apply(
                empId,
                LocalDate.parse(fromDate),
                LocalDate.parse(toDate),
                reason,
                halfDayType
        );
    }

    // APPROVE
    @PostMapping("/approve/{id}")
    public WfhRequest approve(@PathVariable Long id,
                              @RequestParam String approverId) {
        return service.approve(id, approverId);
    }

    // REJECT
    @PostMapping("/reject/{id}")
    public WfhRequest reject(@PathVariable Long id,
                             @RequestParam String approverId,
                             @RequestParam String remarks) {
        return service.reject(id, approverId, remarks);
    }

    // EMPLOYEE VIEW
    @GetMapping("/my")
    public List<WfhRequest> my(@RequestParam String empId) {
        return service.getMyRequests(empId);
    }

    // MANAGER VIEW
    @GetMapping("/pending")
    public List<WfhRequest> pending(@RequestParam String approverId) {
        return service.getPending(approverId);
    }
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelWfh(
            @PathVariable Long id,
            Principal principal
    ) {

        String employeeId = principal.getName(); // comes from JWT

        String response = service.cancelWfh(id, employeeId);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/employee/wfh-history")
    public List<WfhRequest> getEmployeeHistory(@RequestParam String empId) {
        return service.getEmployeeHistory(empId);
    }

    @GetMapping("/manager/wfh-history")
    public List<WfhRequest> getManagerHistory(@RequestParam String managerId) {
        return service.getManagerHistory(managerId);
    }
}



