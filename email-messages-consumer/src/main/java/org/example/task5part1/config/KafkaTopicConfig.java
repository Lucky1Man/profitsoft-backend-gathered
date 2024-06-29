package org.example.task5part1.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    private final String simpleEmailTopic;

    public KafkaTopicConfig(@Qualifier("simpleEmailTopicText") String simpleEmailTopic) {
        this.simpleEmailTopic = simpleEmailTopic;
    }

    @Bean
    public NewTopic simpleEmailTopic() {
        return new NewTopic(simpleEmailTopic, 2, (short) 1);
    }

}
