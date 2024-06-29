package org.example.task5part1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
public class ApplicationConfig {

    @Value("${kafka.topic.simpleEmail}")
    private String simpleEmailTopic;

    @Value("${application.max-background-iterations-on-failed-messages-resend}")
    private Integer maxIterationsForFailedMailsResent;

    @Bean
    public String simpleEmailTopicText() {
        return simpleEmailTopic;
    }

    @Bean
    public Integer maxIterationsForFailedMailsResent() {
        return maxIterationsForFailedMailsResent;
    }

}
