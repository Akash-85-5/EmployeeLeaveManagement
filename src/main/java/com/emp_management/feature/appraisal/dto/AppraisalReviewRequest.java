package com.emp_management.feature.appraisal.dto;

import com.emp_management.shared.enums.AppraisalReviewLevel;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AppraisalReviewRequest {

    @NotNull
    private Long responseId;

    @NotNull
    private AppraisalReviewLevel reviewLevel;

    private String overallRemarks;
    private Boolean revoke = false;

    @NotNull
    private List<ItemReview> items;

    public static class ItemReview {
        private Long itemId;
        private Integer revisedRating;
        private String revisedRemarks;

        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public Integer getRevisedRating() { return revisedRating; }
        public void setRevisedRating(Integer revisedRating) { this.revisedRating = revisedRating; }
        public String getRevisedRemarks() { return revisedRemarks; }
        public void setRevisedRemarks(String revisedRemarks) { this.revisedRemarks = revisedRemarks; }
    }

    public Long getResponseId() { return responseId; }
    public void setResponseId(Long responseId) { this.responseId = responseId; }
    public AppraisalReviewLevel getReviewLevel() { return reviewLevel; }
    public void setReviewLevel(AppraisalReviewLevel reviewLevel) { this.reviewLevel = reviewLevel; }
    public String getOverallRemarks() { return overallRemarks; }
    public void setOverallRemarks(String overallRemarks) { this.overallRemarks = overallRemarks; }
    public Boolean getRevoke() { return revoke; }
    public void setRevoke(Boolean revoke) { this.revoke = revoke; }
    public List<ItemReview> getItems() { return items; }
    public void setItems(List<ItemReview> items) { this.items = items; }
}