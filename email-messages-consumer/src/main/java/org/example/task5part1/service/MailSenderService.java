package org.example.task5part1.service;

import org.example.profitsoft.kafka.messages.SimpleEmailDto;

public interface MailSenderService {
    void send(SimpleEmailDto emailDto);
}
