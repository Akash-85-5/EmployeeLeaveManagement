package com.example.notificationservice.scheduler;

import com.example.notificationservice.service.EscalationService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class EscalationScheduler {

    private final EscalationService escalationService;

    public EscalationScheduler(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void checkEscalations() {
        escalationService.escalatePendingLeaves();
    }
}
