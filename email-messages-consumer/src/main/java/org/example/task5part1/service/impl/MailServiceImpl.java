package org.example.task5part1.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.profitsoft.kafka.messages.SimpleEmailDto;
import org.example.task5part1.document.SendMailRequest;
import org.example.task5part1.document.SendStatus;
import org.example.task5part1.repository.SendMailRequestRepository;
import org.example.task5part1.service.MailSenderService;
import org.example.task5part1.service.MailService;
import org.example.task5part1.service.TimeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MailServiceImpl implements MailService {

    private final MailSenderService mailSenderService;
    private final SendMailRequestRepository mailRequestRepository;
    private final TimeService timeService;
    private final Integer maxIterationsForFailedMailsResent;

    public MailServiceImpl(MailSenderService mailSenderService, SendMailRequestRepository mailRequestRepository,
                           TimeService timeService,
                           @Qualifier("maxIterationsForFailedMailsResent") Integer maxIterationsForFailedMailsResent) {
        this.mailSenderService = mailSenderService;
        this.mailRequestRepository = mailRequestRepository;
        this.timeService = timeService;
        this.maxIterationsForFailedMailsResent = maxIterationsForFailedMailsResent;
    }

    @Override
    public void send(SimpleEmailDto emailDto) {
        SendMailRequest saved = mailRequestRepository.save(mapToRequest(emailDto));
        try {
            mailSenderService.send(emailDto);
            saved.setSendStatus(SendStatus.SENT);
        } catch (RuntimeException ex) {
            saved.setSendStatus(SendStatus.ERROR);
            saved.setErrorMessage(ex.getMessage());
        } finally {
            mailRequestRepository.save(saved);
        }
    }

    private SendMailRequest mapToRequest(SimpleEmailDto emailDto) {
        return SendMailRequest.builder()
                .withId(UUID.randomUUID())
                .withFrom(emailDto.getFrom())
                .withTo(emailDto.getTo())
                .withSubject(emailDto.getSubject())
                .withContent(emailDto.getText())
                .withSendStatus(SendStatus.PENDING)
                .withNumSendAttempts(1)
                .withLastSendAttempt(timeService.instantUtcNow())
                .withSentDate(emailDto.getSentDate())
                .build();
    }

    @Override
    public void resendAllFailedMails() {
        Pageable next50mails = Pageable.ofSize(50);
        Page<SendMailRequest> failedMailRequests = mailRequestRepository.getAllFailedMailRequests(next50mails);
        mailRequestRepository.updateAll(handleFailedMails(failedMailRequests.getContent()));
        int iterations = 0;
        while(iterations < maxIterationsForFailedMailsResent && failedMailRequests.hasNext()) {
            failedMailRequests = mailRequestRepository.getAllFailedMailRequests(next50mails);
            handleFailedMails(failedMailRequests.getContent());
            iterations++;
        }
    }

    public List<SendMailRequest> handleFailedMails(List<SendMailRequest> failedMails) {
        for(SendMailRequest mail : failedMails) {
            try {
                mail.setLastSendAttempt(timeService.instantUtcNow());
                mail.setNumSendAttempts(mail.getNumSendAttempts() + 1);
                mailSenderService.send(new SimpleEmailDto(
                        mail.getFrom(),
                        mail.getTo(),
                        mail.getSubject(),
                        mail.getContent(),
                        mail.getSentDate()
                ));
                mail.setSendStatus(SendStatus.SENT);
            } catch (MailException ex) {
                mail.setSendStatus(SendStatus.ERROR);
                mail.setErrorMessage(ex.getMessage());
            }
        }
        return failedMails;
    }

}
