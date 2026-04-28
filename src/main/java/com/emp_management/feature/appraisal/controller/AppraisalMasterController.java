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

    @PostMapping("/metrics")
    public ResponseEntity<AppraisalMetricMaster> createMetric(
            @Valid @RequestBody MetricMasterRequest req,
            @RequestHeader("X-Employee-Id") String employeeId) {
        return ResponseEntity.ok(masterService.createMetric(req, employeeId));
    }

    @PutMapping("/metrics/{id}")
    public ResponseEntity<AppraisalMetricMaster> updateMetric(
            @PathVariable Long id,
            @Valid @RequestBody MetricMasterRequest req,
            @RequestHeader("X-Employee-Id") String employeeId) {
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

    @PatchMapping("/metrics/{id}/toggle")
    public ResponseEntity<Void> toggleMetric(
            @PathVariable Long id,
            @RequestParam boolean active,
            @RequestHeader("X-Employee-Id") String employeeId) {
        masterService.toggleMetricActive(id, active, employeeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/questions")
    public ResponseEntity<AppraisalQuestionMaster> createQuestion(
            @Valid @RequestBody QuestionMasterRequest req,
            @RequestHeader("X-Employee-Id") String employeeId) {
        return ResponseEntity.ok(masterService.createQuestion(req, employeeId));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<AppraisalQuestionMaster> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionMasterRequest req,
            @RequestHeader("X-Employee-Id") String employeeId) {
        return ResponseEntity.ok(masterService.updateQuestion(id, req, employeeId));
    }

    @GetMapping("/questions")
    public ResponseEntity<List<AppraisalQuestionMaster>> getAllQuestions() {
        return ResponseEntity.ok(masterService.getAllActiveQuestions());
    }

    @GetMapping("/questions/by-metric/{metricId}")
    public ResponseEntity<List<AppraisalQuestionMaster>> getByMetric(@PathVariable Long metricId) {
        return ResponseEntity.ok(masterService.getQuestionsByMetric(metricId));
    }

    @PatchMapping("/questions/{id}/toggle")
    public ResponseEntity<Void> toggleQuestion(
            @PathVariable Long id,
            @RequestParam boolean active,
            @RequestHeader("X-Employee-Id") String employeeId) {
        masterService.toggleQuestionActive(id, active, employeeId);
        return ResponseEntity.ok().build();
    }
}