package com.emp_management.feature.notification.dto;


import com.emp_management.shared.enums.Channel;
import com.emp_management.shared.enums.EventType;

public class NotificationRequest {

    private Long userId;
    private EventType eventType;
    private Channel channel;
    private String context;


    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
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
