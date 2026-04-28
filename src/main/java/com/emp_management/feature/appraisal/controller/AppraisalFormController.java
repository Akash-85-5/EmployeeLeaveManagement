package com.emp_management.feature.appraisal.controller;

import com.emp_management.feature.appraisal.dto.AppraisalFormSaveRequest;
import com.emp_management.feature.appraisal.dto.AppraisalFormResponse;
import com.emp_management.feature.appraisal.service.AppraisalFormService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appraisal/form")
public class AppraisalFormController {

    private final AppraisalFormService formService;

    public AppraisalFormController(AppraisalFormService formService) {
        this.formService = formService;
    }

    @GetMapping("/{definitionId}")
    public ResponseEntity<AppraisalFormResponse> getForm(
            @PathVariable Long definitionId,
            @RequestHeader("X-Employee-Id") String employeeId,
            @RequestHeader(value = "X-L1-Reviewer-Id", required = false) String l1ReviewerId,
            @RequestHeader(value = "X-L2-Reviewer-Id", required = false) String l2ReviewerId) {
        return ResponseEntity.ok(
                formService.getOrInitForm(definitionId, employeeId, l1ReviewerId, l2ReviewerId));
    }

    @PostMapping("/save")
    public ResponseEntity<AppraisalFormResponse> saveDraft(
            @Valid @RequestBody AppraisalFormSaveRequest req,
            @RequestHeader("X-Employee-Id") String employeeId) {
        return ResponseEntity.ok(formService.saveDraft(req, employeeId));
    }

    @PostMapping("/submit")
    public ResponseEntity<AppraisalFormResponse> submitForm(
            @Valid @RequestBody AppraisalFormSaveRequest req,
            @RequestHeader("X-Employee-Id") String employeeId) {
        return ResponseEntity.ok(formService.submitForm(req, employeeId));
    }
}