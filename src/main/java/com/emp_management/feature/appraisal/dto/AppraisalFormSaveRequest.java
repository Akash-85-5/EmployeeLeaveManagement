package com.emp_management.feature.appraisal.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AppraisalFormSaveRequest {

    @NotNull
    private Long definitionId;

    private String employeeOverallRemarks;

    @NotNull
    private List<ItemRequest> items;

    public static class ItemRequest {
        private Long questionId;
        private Integer selfRating;
        private String remarks;

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        public Integer getSelfRating() { return selfRating; }
        public void setSelfRating(Integer selfRating) { this.selfRating = selfRating; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getEmployeeOverallRemarks() { return employeeOverallRemarks; }
    public void setEmployeeOverallRemarks(String employeeOverallRemarks) { this.employeeOverallRemarks = employeeOverallRemarks; }
    public List<ItemRequest> getItems() { return items; }
    public void setItems(List<ItemRequest> items) { this.items = items; }
}