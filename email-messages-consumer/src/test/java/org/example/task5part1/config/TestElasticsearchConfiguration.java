package org.example.task5part1.config;

import org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.List;

@TestConfiguration
@EnableAutoConfiguration(exclude = MailHealthContributorAutoConfiguration.class)
public class TestElasticsearchConfiguration extends ElasticsearchConfiguration {

    @Bean(destroyMethod = "stop")
    public ElasticsearchContainer elasticsearchContainer() {
        ElasticsearchContainer container = new ElasticsearchContainer(
                "docker.elastic.co/elasticsearch/elasticsearch:8.6.1");
        container.setEnv(List.of(
                "discovery.type=single-node",
                "ES_JAVA_OPTS=-Xms1g -Xmx1g",
                "xpack.security.enabled=false"));
        container.start();
        return container;
    }

    @Bean @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(elasticsearchContainer().getHttpHostAddress())
                .build();
    }}

