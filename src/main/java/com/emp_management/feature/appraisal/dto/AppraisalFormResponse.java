package com.emp_management.feature.appraisal.dto;

import com.emp_management.shared.enums.AppraisalStatus;
import com.emp_management.shared.enums.MetricType;

import java.time.LocalDate;
import java.util.List;

public class AppraisalFormResponse {

    private Long responseId;
    private Long definitionId;
    private String appraisalYear;
    private String title;
    private LocalDate openDate;
    private LocalDate closeDate;
    private AppraisalStatus status;
    private String employeeOverallRemarks;
    private String l1OverallRemarks;
    private String l2OverallRemarks;
    private List<MetricSection> sections;

    public static class MetricSection {
        private Long metricId;
        private MetricType metricType;
        private String metricDescription;
        private Integer minRating;
        private Integer maxRating;
        private List<QuestionItem> questions;

        public Long getMetricId() { return metricId; }
        public void setMetricId(Long metricId) { this.metricId = metricId; }
        public MetricType getMetricType() { return metricType; }
        public void setMetricType(MetricType metricType) { this.metricType = metricType; }
        public String getMetricDescription() { return metricDescription; }
        public void setMetricDescription(String metricDescription) { this.metricDescription = metricDescription; }
        public Integer getMinRating() { return minRating; }
        public void setMinRating(Integer minRating) { this.minRating = minRating; }
        public Integer getMaxRating() { return maxRating; }
        public void setMaxRating(Integer maxRating) { this.maxRating = maxRating; }
        public List<QuestionItem> getQuestions() { return questions; }
        public void setQuestions(List<QuestionItem> questions) { this.questions = questions; }
    }

    public static class QuestionItem {
        private Long itemId;
        private Long questionId;
        private String questionText;
        private Integer employeeSelfRating;
        private String employeeRemarks;
        private Integer l1RevisedRating;
        private String l1RevisedRemarks;
        private Integer l2FinalRating;
        private String l2FinalRemarks;

        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public Integer getEmployeeSelfRating() { return employeeSelfRating; }
        public void setEmployeeSelfRating(Integer employeeSelfRating) { this.employeeSelfRating = employeeSelfRating; }
        public String getEmployeeRemarks() { return employeeRemarks; }
        public void setEmployeeRemarks(String employeeRemarks) { this.employeeRemarks = employeeRemarks; }
        public Integer getL1RevisedRating() { return l1RevisedRating; }
        public void setL1RevisedRating(Integer l1RevisedRating) { this.l1RevisedRating = l1RevisedRating; }
        public String getL1RevisedRemarks() { return l1RevisedRemarks; }
        public void setL1RevisedRemarks(String l1RevisedRemarks) { this.l1RevisedRemarks = l1RevisedRemarks; }
        public Integer getL2FinalRating() { return l2FinalRating; }
        public void setL2FinalRating(Integer l2FinalRating) { this.l2FinalRating = l2FinalRating; }
        public String getL2FinalRemarks() { return l2FinalRemarks; }
        public void setL2FinalRemarks(String l2FinalRemarks) { this.l2FinalRemarks = l2FinalRemarks; }
    }

    public Long getResponseId() { return responseId; }
    public void setResponseId(Long responseId) { this.responseId = responseId; }
    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getAppraisalYear() { return appraisalYear; }
    public void setAppraisalYear(String appraisalYear) { this.appraisalYear = appraisalYear; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }
    public AppraisalStatus getStatus() { return status; }
    public void setStatus(AppraisalStatus status) { this.status = status; }
    public String getEmployeeOverallRemarks() { return employeeOverallRemarks; }
    public void setEmployeeOverallRemarks(String r) { this.employeeOverallRemarks = r; }
    public String getL1OverallRemarks() { return l1OverallRemarks; }
    public void setL1OverallRemarks(String l1OverallRemarks) { this.l1OverallRemarks = l1OverallRemarks; }
    public String getL2OverallRemarks() { return l2OverallRemarks; }
    public void setL2OverallRemarks(String l2OverallRemarks) { this.l2OverallRemarks = l2OverallRemarks; }
    public List<MetricSection> getSections() { return sections; }
    public void setSections(List<MetricSection> sections) { this.sections = sections; }
}