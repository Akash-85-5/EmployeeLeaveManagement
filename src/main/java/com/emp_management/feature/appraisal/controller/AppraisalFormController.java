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

    @GetMapping("/{definitionId}/{employeeId}")
    public ResponseEntity<AppraisalFormResponse> getForm(
            @PathVariable Long definitionId,
            @PathVariable String employeeId,
            @RequestParam(required = false) String l1ReviewerId,
            @RequestParam(required = false) String l2ReviewerId) {
        return ResponseEntity.ok(
                formService.getOrInitForm(definitionId, employeeId, l1ReviewerId, l2ReviewerId));
    }

    @PostMapping("/save/{employeeId}")
    public ResponseEntity<AppraisalFormResponse> saveDraft(
            @PathVariable String employeeId,
            @Valid @RequestBody AppraisalFormSaveRequest req) {
        return ResponseEntity.ok(formService.saveDraft(req, employeeId));
    }

    @PostMapping("/submit/{employeeId}")
    public ResponseEntity<AppraisalFormResponse> submitForm(
            @PathVariable String employeeId,
            @Valid @RequestBody AppraisalFormSaveRequest req) {
        return ResponseEntity.ok(formService.submitForm(req, employeeId));
    }
}