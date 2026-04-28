package com.emp_management.feature.appraisal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class AppraisalDefinitionRequest {

    @NotBlank(message = "Appraisal year is required")
    private String appraisalYear;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Open date is required")
    private LocalDate openDate;

    @NotNull(message = "Close date is required")
    private LocalDate closeDate;

    private String defaultL1ReviewerId;
    private String defaultL2ReviewerId;
    private Boolean autoSubmitEnabled = true;

    @NotNull
    private List<Long> questionIds;

    public String getAppraisalYear() { return appraisalYear; }
    public void setAppraisalYear(String appraisalYear) { this.appraisalYear = appraisalYear; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }
    public String getDefaultL1ReviewerId() { return defaultL1ReviewerId; }
    public void setDefaultL1ReviewerId(String defaultL1ReviewerId) { this.defaultL1ReviewerId = defaultL1ReviewerId; }
    public String getDefaultL2ReviewerId() { return defaultL2ReviewerId; }
    public void setDefaultL2ReviewerId(String defaultL2ReviewerId) { this.defaultL2ReviewerId = defaultL2ReviewerId; }
    public Boolean getAutoSubmitEnabled() { return autoSubmitEnabled; }
    public void setAutoSubmitEnabled(Boolean autoSubmitEnabled) { this.autoSubmitEnabled = autoSubmitEnabled; }
    public List<Long> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Long> questionIds) { this.questionIds = questionIds; }
}