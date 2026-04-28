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

    @GetMapping("/pending/l1/{reviewerId}")
    public ResponseEntity<List<AppraisalResponse>> pendingL1Reviews(
            @PathVariable String reviewerId) {
        return ResponseEntity.ok(reviewService.getPendingL1Reviews(reviewerId));
    }

    @GetMapping("/pending/l2/{reviewerId}")
    public ResponseEntity<List<AppraisalResponse>> pendingL2Reviews(
            @PathVariable String reviewerId) {
        return ResponseEntity.ok(reviewService.getPendingL2Reviews(reviewerId));
    }

    @PostMapping("/l1/{reviewerId}")
    public ResponseEntity<AppraisalResponse> submitL1Review(
            @PathVariable String reviewerId,
            @Valid @RequestBody AppraisalReviewRequest req) {
        return ResponseEntity.ok(reviewService.submitL1Review(req, reviewerId));
    }

    @PostMapping("/l2/{reviewerId}")
    public ResponseEntity<AppraisalResponse> submitL2Review(
            @PathVariable String reviewerId,
            @Valid @RequestBody AppraisalReviewRequest req) {
        return ResponseEntity.ok(reviewService.submitL2Review(req, reviewerId));
    }

    @GetMapping("/audit/{responseId}")
    public ResponseEntity<List<AppraisalReviewLog>> getAuditTrail(
            @PathVariable Long responseId) {
        return ResponseEntity.ok(reviewService.getAuditTrail(responseId));
    }
}