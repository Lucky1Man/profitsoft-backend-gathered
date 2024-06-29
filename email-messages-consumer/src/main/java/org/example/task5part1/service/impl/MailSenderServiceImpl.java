package org.example.task5part1.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.profitsoft.kafka.messages.SimpleEmailDto;
import org.example.task5part1.service.MailSenderService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class MailSenderServiceImpl implements MailSenderService {

    private final JavaMailSender javaMailSender;

    @Override
    public void send(SimpleEmailDto emailDto) {
        var mail = new SimpleMailMessage();
        mail.setFrom(emailDto.getFrom());
        mail.setTo(emailDto.getTo().toArray(String[]::new));
        mail.setSubject(emailDto.getSubject());
        mail.setText(emailDto.getText());
        mail.setSentDate(emailDto.getSentDate());
        javaMailSender.send(mail);
    }

}
