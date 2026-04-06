package com.emp_management.feature.od.dto;

import com.emp_management.shared.enums.RequestStatus;

public class ODDecisionRequest {

    private Long          odId;
    private String        approverId;
    private RequestStatus decision;
    private String        comments;

    public Long getOdId() { return odId; }
    public void setOdId(Long odId) { this.odId = odId; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public RequestStatus getDecision() { return decision; }
    public void setDecision(RequestStatus decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}