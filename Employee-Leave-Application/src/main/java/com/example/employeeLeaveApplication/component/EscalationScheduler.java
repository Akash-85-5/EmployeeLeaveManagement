package com.example.employeeLeaveApplication.component;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class EscalationScheduler {

    private final com.example.employeeLeaveApplication.service.EscalationService escalationService;

    public EscalationScheduler(com.example.employeeLeaveApplication.service.EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void checkEscalations() {
        escalationService.escalatePendingLeaves();
    }
}
