package com.emp_management.feature.birthday.scheduler;

import com.emp_management.feature.birthday.service.BirthdayService;
import com.emp_management.feature.birthday.service.BirthdayEmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BirthdayScheduler {

    private final BirthdayService birthdayService;
    private final BirthdayEmailService birthdayEmailService;

    public BirthdayScheduler(BirthdayService birthdayService,
                             BirthdayEmailService birthdayEmailService) {
        this.birthdayService = birthdayService;
        this.birthdayEmailService = birthdayEmailService;
    }

    /** Runs every day at 12:00 AM — saves system wishes to DB */
    @Scheduled(cron = "0 0 0 * * *")
    public void sendDailyBirthdayWishes() {
        birthdayService.sendSystemWishes();
    }

    /** Runs every day at 12:00 AM — sends birthday email (only to today's birthdays) */
    @Scheduled(cron = "10 0 0 * * *")
    public void sendBirthdayEmails() {
        birthdayEmailService.sendBirthdayEmails();
    }
}