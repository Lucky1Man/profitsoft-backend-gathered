package org.example.task5part1.scheduler;

import lombok.SneakyThrows;
import org.example.task5part1.Task5Part1Application;
import org.example.task5part1.config.TestElasticsearchConfiguration;
import org.example.task5part1.document.SendMailRequest;
import org.example.task5part1.document.SendStatus;
import org.example.task5part1.repository.SendMailRequestRepository;
import org.example.task5part1.service.MailService;
import org.example.task5part1.service.TimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.MailParseException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "application.resend-failed-mails-cron=*/1 * * * * *",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.cloud.consul.enabled=false"
})
@ContextConfiguration(classes = {Task5Part1Application.class, TestElasticsearchConfiguration.class})
class FailedMailsHandlerSchedulerTest {

    @MockBean
    JavaMailSender javaMailSender;

    @SpyBean
    MailService mailService;

    @SpyBean
    SendMailRequestRepository mailRequestRepository;

    @SpyBean
    TimeService timeService;

    @AfterEach
    void resetEnvironment() {
        Mockito.reset(javaMailSender, mailService, mailRequestRepository, timeService);
        mailRequestRepository.deleteAll();
    }

    @SneakyThrows
    @Test
    void resentAllFailedMessages_shouldChangeDbObject_ifResendWasSuccessful() {
        //given
        Instant instant = Instant.from(ZonedDateTime.of(LocalDateTime.of(200, 1, 1, 0, 0), ZoneId.systemDefault()));
        Date date = Date.from(instant);
        Instant expectedLastAttempt = instant.plus(5, ChronoUnit.MINUTES);
        doReturn(expectedLastAttempt).when(timeService).instantUtcNow();
        SendMailRequest saved = mailRequestRepository.save(new SendMailRequest(
                UUID.randomUUID(),
                "test@from",
                List.of("test@to"),
                "testSubject",
                "testContent",
                SendStatus.ERROR,
                "test error",
                instant,
                1,
                date
        ));
        doCallRealMethod().doNothing().when(mailService).resendAllFailedMails();
        //then
        await()
                .atMost(1, TimeUnit.MINUTES)
                .untilAsserted(() ->
                        verify(mailService, atLeast(1)).resendAllFailedMails()
                );
        await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> mailRequestRepository.findAll().iterator().hasNext());
        await()
                .atMost(1, TimeUnit.MINUTES)
                .untilAsserted(() -> {
                            SendMailRequest actualRequest = mailRequestRepository.findAll().iterator().next();
                            assertEquals(saved.getNumSendAttempts() + 1, actualRequest.getNumSendAttempts());
                            assertEquals(saved.getFrom(), actualRequest.getFrom());
                            assertEquals(saved.getTo(), actualRequest.getTo());
                            assertEquals(saved.getSubject(), actualRequest.getSubject());
                            assertEquals(saved.getContent(), actualRequest.getContent());
                            assertEquals(saved.getSentDate(), actualRequest.getSentDate());
                            assertEquals(saved.getErrorMessage(), actualRequest.getErrorMessage());
                            assertEquals(SendStatus.SENT, actualRequest.getSendStatus());
                            assertEquals(expectedLastAttempt, actualRequest.getLastSendAttempt());
                        }

                );
    }

    @SneakyThrows
    @Test
    void resentAllFailedMessages_shouldChangeDbObject_ifResendWasFailed() {
        //given
        Instant instant = Instant.from(ZonedDateTime.of(LocalDateTime.of(200, 1, 1, 0, 0), ZoneId.systemDefault()));
        Date date = Date.from(instant);
        Instant expectedLastAttempt = instant.plus(5, ChronoUnit.MINUTES);
        doReturn(expectedLastAttempt).when(timeService).instantUtcNow();
        SendMailRequest saved = mailRequestRepository.save(new SendMailRequest(
                UUID.randomUUID(),
                "test@from",
                List.of("test@to"),
                "testSubject",
                "testContent",
                SendStatus.ERROR,
                "test error",
                instant,
                1,
                date
        ));
        String expectedMessage = "123";
        doThrow(new MailParseException(expectedMessage)).when(javaMailSender).send(any(SimpleMailMessage.class));
        doCallRealMethod().doNothing().when(mailService).resendAllFailedMails();
        //then
        await()
                .atMost(1, TimeUnit.MINUTES)
                .untilAsserted(() ->
                        verify(mailService, atLeast(1)).resendAllFailedMails()
                );
        await()
                .atMost(1, TimeUnit.MINUTES)
                .untilAsserted(() ->
                        {
                            SendMailRequest actualRequest = mailRequestRepository.findAll().iterator().next();
                            assertEquals(saved.getNumSendAttempts() + 1, actualRequest.getNumSendAttempts());
                            assertEquals(saved.getFrom(), actualRequest.getFrom());
                            assertEquals(saved.getTo(), actualRequest.getTo());
                            assertEquals(saved.getSubject(), actualRequest.getSubject());
                            assertEquals(saved.getContent(), actualRequest.getContent());
                            assertEquals(saved.getSentDate(), actualRequest.getSentDate());
                            assertEquals(expectedMessage, actualRequest.getErrorMessage());
                            assertEquals(saved.getNumSendAttempts() + 1, actualRequest.getNumSendAttempts());
                            assertEquals(SendStatus.ERROR, actualRequest.getSendStatus());
                            assertEquals(expectedLastAttempt, actualRequest.getLastSendAttempt());
                        }
                );

    }

}
