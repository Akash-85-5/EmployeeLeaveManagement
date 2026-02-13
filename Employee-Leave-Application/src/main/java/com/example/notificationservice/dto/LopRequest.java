package com.example.notificationservice.dto;

public class LopRequest {

    private Long leaveId;
    private boolean acceptLop;

    // Default constructor
    public LopRequest() {
    }

    // All-args constructor
    public LopRequest(Long leaveId, boolean acceptLop) {
        this.leaveId = leaveId;
        this.acceptLop = acceptLop;
    }

    // Getter for leaveId
    public Long getLeaveId() {
        return leaveId;
    }

    // Setter for leaveId
    public void setLeaveId(Long leaveId) {
        this.leaveId = leaveId;
    }

    // Getter for acceptLop
    public boolean isAcceptLop() {
        return acceptLop;
    }

    // Setter for acceptLop
    public void setAcceptLop(boolean acceptLop) {
        this.acceptLop = acceptLop;
    }
}
