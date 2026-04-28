package com.emp_management.feature.appraisal.service;

import com.emp_management.feature.appraisal.dto.AppraisalReviewRequest;
import com.emp_management.feature.appraisal.entity.AppraisalResponse;
import com.emp_management.feature.appraisal.entity.AppraisalResponseItem;
import com.emp_management.feature.appraisal.entity.AppraisalReviewLog;
import com.emp_management.shared.enums.AppraisalReviewLevel;
import com.emp_management.shared.enums.AppraisalStatus;
import com.emp_management.feature.appraisal.repository.AppraisalResponseItemRepository;
import com.emp_management.feature.appraisal.repository.AppraisalResponseRepository;
import com.emp_management.feature.appraisal.repository.AppraisalReviewLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppraisalReviewService {

    private final AppraisalResponseRepository responseRepo;
    private final AppraisalResponseItemRepository responseItemRepo;
    private final AppraisalReviewLogRepository reviewLogRepo;

    public AppraisalReviewService(AppraisalResponseRepository responseRepo,
                                  AppraisalResponseItemRepository responseItemRepo,
                                  AppraisalReviewLogRepository reviewLogRepo) {
        this.responseRepo = responseRepo;
        this.responseItemRepo = responseItemRepo;
        this.reviewLogRepo = reviewLogRepo;
    }

    public AppraisalResponse submitL1Review(AppraisalReviewRequest req, String reviewerId) {
        AppraisalResponse response = validateReviewer(req.getResponseId(),
                reviewerId, AppraisalReviewLevel.L1, AppraisalStatus.SUBMITTED);
        if (Boolean.TRUE.equals(req.getRevoke())) {
            return revokeToEmployee(response, reviewerId, req.getOverallRemarks(),
                    AppraisalReviewLevel.L1, AppraisalStatus.SUBMITTED);
        }
        applyL1ItemReviews(response, req.getItems());
        response.setL1OverallRemarks(req.getOverallRemarks());
        AppraisalStatus from = response.getStatus();
        response.setStatus(AppraisalStatus.L2_REVIEW);
        responseRepo.save(response);
        logAction(response.getId(), reviewerId, AppraisalReviewLevel.L1,
                from, AppraisalStatus.L2_REVIEW, req.getOverallRemarks());
        return response;
    }

    public AppraisalResponse submitL2Review(AppraisalReviewRequest req, String reviewerId) {
        AppraisalResponse response = validateReviewer(req.getResponseId(),
                reviewerId, AppraisalReviewLevel.L2, AppraisalStatus.L2_REVIEW);
        if (Boolean.TRUE.equals(req.getRevoke())) {
            return revokeToEmployee(response, reviewerId, req.getOverallRemarks(),
                    AppraisalReviewLevel.L2, AppraisalStatus.L2_REVIEW);
        }
        applyL2ItemReviews(response, req.getItems());
        response.setL2OverallRemarks(req.getOverallRemarks());
        AppraisalStatus from = response.getStatus();
        response.setStatus(AppraisalStatus.PUBLISHED);
        responseRepo.save(response);
        logAction(response.getId(), reviewerId, AppraisalReviewLevel.L2,
                from, AppraisalStatus.PUBLISHED, req.getOverallRemarks());
        return response;
    }

    @Transactional(readOnly = true)
    public List<AppraisalResponse> getPendingL1Reviews(String reviewerId) {
        return responseRepo.findByL1ReviewerIdAndStatus(reviewerId, AppraisalStatus.SUBMITTED);
    }

    @Transactional(readOnly = true)
    public List<AppraisalResponse> getPendingL2Reviews(String reviewerId) {
        return responseRepo.findByL2ReviewerIdAndStatus(reviewerId, AppraisalStatus.L2_REVIEW);
    }

    @Transactional(readOnly = true)
    public List<AppraisalReviewLog> getAuditTrail(Long responseId) {
        return reviewLogRepo.findByResponseIdOrderByActedAtAsc(responseId);
    }

    private AppraisalResponse validateReviewer(Long responseId, String reviewerId,
                                               AppraisalReviewLevel level,
                                               AppraisalStatus requiredStatus) {
        AppraisalResponse response = responseRepo.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Response not found: " + responseId));
        if (response.getStatus() != requiredStatus) {
            throw new IllegalStateException(
                    "Response is not in " + requiredStatus + " status (current: " + response.getStatus() + ")");
        }
        String expectedReviewer = level == AppraisalReviewLevel.L1
                ? response.getL1ReviewerId() : response.getL2ReviewerId();
        if (!reviewerId.equals(expectedReviewer)) {
            throw new SecurityException("You are not the assigned " + level + " reviewer for this response");
        }
        return response;
    }

    private AppraisalResponse revokeToEmployee(AppraisalResponse response, String reviewerId,
                                               String comments, AppraisalReviewLevel level,
                                               AppraisalStatus from) {
        response.setStatus(AppraisalStatus.REVOKED);
        responseRepo.save(response);
        logAction(response.getId(), reviewerId, level, from, AppraisalStatus.REVOKED, comments);
        return response;
    }

    private void applyL1ItemReviews(AppraisalResponse response,
                                    List<AppraisalReviewRequest.ItemReview> itemReviews) {
        List<AppraisalResponseItem> items = responseItemRepo.findByResponseId(response.getId());
        Map<Long, AppraisalResponseItem> byId = items.stream()
                .collect(Collectors.toMap(AppraisalResponseItem::getId, i -> i));
        for (AppraisalReviewRequest.ItemReview ir : itemReviews) {
            AppraisalResponseItem item = byId.get(ir.getItemId());
            if (item != null) {
                item.setL1RevisedRating(ir.getRevisedRating());
                item.setL1RevisedRemarks(ir.getRevisedRemarks());
            }
        }
        responseItemRepo.saveAll(items);
    }

    private void applyL2ItemReviews(AppraisalResponse response,
                                    List<AppraisalReviewRequest.ItemReview> itemReviews) {
        List<AppraisalResponseItem> items = responseItemRepo.findByResponseId(response.getId());
        Map<Long, AppraisalResponseItem> byId = items.stream()
                .collect(Collectors.toMap(AppraisalResponseItem::getId, i -> i));
        for (AppraisalReviewRequest.ItemReview ir : itemReviews) {
            AppraisalResponseItem item = byId.get(ir.getItemId());
            if (item != null) {
                item.setL2FinalRating(ir.getRevisedRating());
                item.setL2FinalRemarks(ir.getRevisedRemarks());
            }
        }
        responseItemRepo.saveAll(items);
    }

    private void logAction(Long responseId, String actorId, AppraisalReviewLevel level,
                           AppraisalStatus from, AppraisalStatus to, String comments) {
        AppraisalReviewLog log = new AppraisalReviewLog();
        log.setResponseId(responseId);
        log.setActorId(actorId);
        log.setReviewLevel(level);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setComments(comments);
        reviewLogRepo.save(log);
    }
}