package org.example.task5part1.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.task5part1.service.MailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FailedMailsHandlerScheduler {

    private final MailService mailService;

    @Scheduled(cron = "${application.resend-failed-mails-cron}")
    public void scheduleFailedMailsResend() {
        mailService.resendAllFailedMails();
    }

}
