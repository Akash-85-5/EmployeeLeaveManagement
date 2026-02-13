package com.example.employeeLeaveApplication.dto;


import com.example.employeeLeaveApplication.enums.Channel;
import com.example.employeeLeaveApplication.enums.EventType;
import com.example.employeeLeaveApplication.enums.RecipientType;


public class NotificationRequest {

    private Long userId;
    private EventType eventType;
    private Channel channel;
    private RecipientType recipientType;
    private String context;


    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(RecipientType recipientType) {
        this.recipientType = recipientType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }


}
