package com.emp_management.feature.appraisal.controller;

import com.emp_management.feature.appraisal.dto.MetricMasterRequest;
import com.emp_management.feature.appraisal.dto.QuestionMasterRequest;
import com.emp_management.feature.appraisal.entity.AppraisalMetricMaster;
import com.emp_management.feature.appraisal.entity.AppraisalQuestionMaster;
import com.emp_management.feature.appraisal.service.AppraisalMasterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appraisal/master")
public class AppraisalMasterController {

    private final AppraisalMasterService masterService;

    public AppraisalMasterController(AppraisalMasterService masterService) {
        this.masterService = masterService;
    }

    // ── Metric endpoints ──────────────────────────────────────────

    @PostMapping("/metrics/{employeeId}")
    public ResponseEntity<AppraisalMetricMaster> createMetric(
            @PathVariable String employeeId,
            @Valid @RequestBody MetricMasterRequest req) {
        return ResponseEntity.ok(masterService.createMetric(req, employeeId));
    }

    @PutMapping("/metrics/{id}/{employeeId}")
    public ResponseEntity<AppraisalMetricMaster> updateMetric(
            @PathVariable Long id,
            @PathVariable String employeeId,
            @Valid @RequestBody MetricMasterRequest req) {
        return ResponseEntity.ok(masterService.updateMetric(id, req, employeeId));
    }

    @GetMapping("/metrics")
    public ResponseEntity<List<AppraisalMetricMaster>> getAllMetrics(
            @RequestParam(defaultValue = "false") boolean all) {
        List<AppraisalMetricMaster> metrics = all
                ? masterService.getAllMetrics()
                : masterService.getAllActiveMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PatchMapping("/metrics/{id}/toggle/{employeeId}")
    public ResponseEntity<Void> toggleMetric(
            @PathVariable Long id,
            @PathVariable String employeeId,
            @RequestParam boolean active) {
        masterService.toggleMetricActive(id, active, employeeId);
        return ResponseEntity.ok().build();
    }

    // ── Question endpoints ────────────────────────────────────────

    @PostMapping("/questions/{employeeId}")
    public ResponseEntity<AppraisalQuestionMaster> createQuestion(
            @PathVariable String employeeId,
            @Valid @RequestBody QuestionMasterRequest req) {
        return ResponseEntity.ok(masterService.createQuestion(req, employeeId));
    }

    @PutMapping("/questions/{id}/{employeeId}")
    public ResponseEntity<AppraisalQuestionMaster> updateQuestion(
            @PathVariable Long id,
            @PathVariable String employeeId,
            @Valid @RequestBody QuestionMasterRequest req) {
        return ResponseEntity.ok(masterService.updateQuestion(id, req, employeeId));
    }

    @GetMapping("/questions")
    public ResponseEntity<List<AppraisalQuestionMaster>> getAllQuestions() {
        return ResponseEntity.ok(masterService.getAllActiveQuestions());
    }

    @GetMapping("/questions/by-metric/{metricId}")
    public ResponseEntity<List<AppraisalQuestionMaster>> getByMetric(
            @PathVariable Long metricId) {
        return ResponseEntity.ok(masterService.getQuestionsByMetric(metricId));
    }

    @PatchMapping("/questions/{id}/toggle/{employeeId}")
    public ResponseEntity<Void> toggleQuestion(
            @PathVariable Long id,
            @PathVariable String employeeId,
            @RequestParam boolean active) {
        masterService.toggleQuestionActive(id, active, employeeId);
        return ResponseEntity.ok().build();
    }
}