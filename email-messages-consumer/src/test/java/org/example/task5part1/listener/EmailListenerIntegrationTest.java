package org.example.task5part1.listener;

import lombok.SneakyThrows;
import org.example.profitsoft.kafka.messages.SimpleEmailDto;
import org.example.task5part1.Task5Part1Application;
import org.example.task5part1.config.TestElasticsearchConfiguration;
import org.example.task5part1.document.SendMailRequest;
import org.example.task5part1.document.SendStatus;
import org.example.task5part1.repository.SendMailRequestRepository;
import org.example.task5part1.service.MailSenderService;
import org.example.task5part1.service.MailService;
import org.example.task5part1.service.TimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.bootstrap-servers=localhost:9099",
        "spring.cloud.consul.enabled=false"
})
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9099",
        "port=9099"})
@ContextConfiguration(classes = {Task5Part1Application.class, TestElasticsearchConfiguration.class})
class EmailListenerIntegrationTest {

    @Autowired
    KafkaOperations<String, SimpleEmailDto> kafkaOperations;

    @MockBean
    JavaMailSender javaMailSender;

    @MockBean
    MailSenderService mailSenderService;

    @SpyBean
    MailService mailService;

    @Autowired
    SendMailRequestRepository mailRequestRepository;


    @Autowired
    @Qualifier("simpleEmailTopicText")
    String simpleMailTopic;

    @SpyBean
    TimeService timeService;

    @AfterEach
    void resetEnvironment() {
        Mockito.reset(javaMailSender, mailService, timeService, mailSenderService);
        mailRequestRepository.deleteAll();
    }

    @SneakyThrows
    @Test
    void messageShouldBeSentAndSavedToDb_ifSentRequestHadNoErrors() {
        //given
        Date date = Date.from(Instant.from(ZonedDateTime.of(LocalDateTime.of(200, 1, 1, 0, 0), ZoneId.systemDefault())));
        SimpleEmailDto expectedMail = new SimpleEmailDto(
                "some@from", List.of("some@to"), "test subject", "test text", date
        );
        doReturn(date.toInstant()).when(timeService).instantUtcNow();
        doNothing().when(mailService).resendAllFailedMails();
        //when
        Thread.sleep(1000);
        // мінус 1 година часу на те щоб пофіксити неконсистентність в тестах 😐😐😐.
        // Виявилося, що навіть якщо kafkaOperations вже заінжекчений це не привід розраховувати на те,
        // що сам кафка може приймати повідомлення 😭😭😭😭😭.
        kafkaOperations.send(simpleMailTopic, UUID.randomUUID().toString(), expectedMail);
        //then
        verify(mailService, after(1000).times(1)).send(expectedMail);
        await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> mailRequestRepository.findAll().iterator().hasNext());
        SendMailRequest actualRequest = mailRequestRepository.findAll().iterator().next();
        assertEquals(expectedMail.getFrom(), actualRequest.getFrom());
        assertEquals(expectedMail.getTo(), actualRequest.getTo());
        assertEquals(expectedMail.getSubject(), actualRequest.getSubject());
        assertEquals(expectedMail.getText(), actualRequest.getContent());
        assertEquals(expectedMail.getSentDate(), actualRequest.getSentDate());
        assertNull(actualRequest.getErrorMessage());
        assertEquals(1, actualRequest.getNumSendAttempts());
        assertEquals(SendStatus.SENT, actualRequest.getSendStatus());
        assertEquals(date.toInstant(), actualRequest.getLastSendAttempt());
    }

    @SneakyThrows
    @Test
    void messageShouldBeSentAndSavedToDb_ifErrorsOccurredWhenSending() {
        //given
        Date date = Date.from(Instant.from(ZonedDateTime.of(LocalDateTime.of(200, 1, 1, 0, 0), ZoneId.systemDefault())));
        SimpleEmailDto expectedMail = new SimpleEmailDto(
                "some@from", List.of("some@to"), "test subject", "test text", date
        );
        String expectedMessage = "123";
        doReturn(date.toInstant()).when(timeService).instantUtcNow();
        doThrow(new MailParseException(expectedMessage)).when(mailSenderService).send(any());
        doNothing().when(mailService).resendAllFailedMails();
        //when
        Thread.sleep(1000);
        kafkaOperations.send(simpleMailTopic, UUID.randomUUID().toString(), expectedMail);
        //then
        verify(mailService, after(1000).times(1)).send(expectedMail);
        await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> mailRequestRepository.findAll().iterator().hasNext());
        SendMailRequest actualRequest = mailRequestRepository.findAll().iterator().next();
        assertEquals(expectedMail.getFrom(), actualRequest.getFrom());
        assertEquals(expectedMail.getTo(), actualRequest.getTo());
        assertEquals(expectedMail.getSubject(), actualRequest.getSubject());
        assertEquals(expectedMail.getText(), actualRequest.getContent());
        assertEquals(expectedMail.getSentDate(), actualRequest.getSentDate());
        assertEquals(expectedMessage, actualRequest.getErrorMessage());
        assertEquals(1, actualRequest.getNumSendAttempts());
        assertEquals(SendStatus.ERROR, actualRequest.getSendStatus());
        assertEquals(date.toInstant(), actualRequest.getLastSendAttempt());
    }

}
