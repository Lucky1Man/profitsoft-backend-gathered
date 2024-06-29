package org.example.task5part1.service;

import org.example.profitsoft.kafka.messages.SimpleEmailDto;

public interface MailService {
    void send(SimpleEmailDto emailDto);

    void resendAllFailedMails();
}
