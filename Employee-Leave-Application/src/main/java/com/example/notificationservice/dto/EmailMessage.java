package com.example.notificationservice.dto;

public class EmailMessage {

    private final String subject;
    private final String body;

    public EmailMessage(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}
