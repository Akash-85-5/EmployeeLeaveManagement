package com.emp_management.feature.appraisal.controller;

import com.emp_management.feature.appraisal.dto.AppraisalDefinitionRequest;
import com.emp_management.feature.appraisal.entity.AppraisalDefinition;
import com.emp_management.feature.appraisal.service.AppraisalDefinitionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appraisal/definitions")
public class AppraisalDefinitionController {

    private final AppraisalDefinitionService definitionService;

    public AppraisalDefinitionController(AppraisalDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @PostMapping("/{employeeId}")
    public ResponseEntity<AppraisalDefinition> create(
            @PathVariable String employeeId,
            @Valid @RequestBody AppraisalDefinitionRequest req) {
        return ResponseEntity.ok(definitionService.createDefinition(req, employeeId));
    }

    @PutMapping("/{id}/{employeeId}")
    public ResponseEntity<AppraisalDefinition> update(
            @PathVariable Long id,
            @PathVariable String employeeId,
            @Valid @RequestBody AppraisalDefinitionRequest req) {
        return ResponseEntity.ok(definitionService.updateDefinition(id, req, employeeId));
    }

    @PatchMapping("/{id}/publish/{employeeId}")
    public ResponseEntity<AppraisalDefinition> publish(
            @PathVariable Long id,
            @PathVariable String employeeId) {
        return ResponseEntity.ok(definitionService.publishDefinition(id, employeeId));
    }

    @GetMapping
    public ResponseEntity<List<AppraisalDefinition>> getAll() {
        return ResponseEntity.ok(definitionService.getAll());
    }

    @GetMapping("/published")
    public ResponseEntity<List<AppraisalDefinition>> getPublished() {
        return ResponseEntity.ok(definitionService.getAllPublished());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppraisalDefinition> getById(@PathVariable Long id) {
        return ResponseEntity.ok(definitionService.getById(id));
    }
}