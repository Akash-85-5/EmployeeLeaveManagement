package com.emp_management.feature.appraisal.controller;

import com.emp_management.feature.appraisal.dto.AppraisalReviewRequest;
import com.emp_management.feature.appraisal.entity.AppraisalResponse;
import com.emp_management.feature.appraisal.entity.AppraisalReviewLog;
import com.emp_management.feature.appraisal.service.AppraisalReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appraisal/review")
public class AppraisalReviewController {

    private final AppraisalReviewService reviewService;

    public AppraisalReviewController(AppraisalReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/pending/l1")
    public ResponseEntity<List<AppraisalResponse>> pendingL1Reviews(
            @RequestHeader("X-Employee-Id") String reviewerId) {
        return ResponseEntity.ok(reviewService.getPendingL1Reviews(reviewerId));
    }

    @GetMapping("/pending/l2")
    public ResponseEntity<List<AppraisalResponse>> pendingL2Reviews(
            @RequestHeader("X-Employee-Id") String reviewerId) {
        return ResponseEntity.ok(reviewService.getPendingL2Reviews(reviewerId));
    }

    @PostMapping("/l1")
    public ResponseEntity<AppraisalResponse> submitL1Review(
            @Valid @RequestBody AppraisalReviewRequest req,
            @RequestHeader("X-Employee-Id") String reviewerId) {
        return ResponseEntity.ok(reviewService.submitL1Review(req, reviewerId));
    }

    @PostMapping("/l2")
    public ResponseEntity<AppraisalResponse> submitL2Review(
            @Valid @RequestBody AppraisalReviewRequest req,
            @RequestHeader("X-Employee-Id") String reviewerId) {
        return ResponseEntity.ok(reviewService.submitL2Review(req, reviewerId));
    }

    @GetMapping("/audit/{responseId}")
    public ResponseEntity<List<AppraisalReviewLog>> getAuditTrail(
            @PathVariable Long responseId) {
        return ResponseEntity.ok(reviewService.getAuditTrail(responseId));
    }
}