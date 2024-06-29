package org.example.task2restapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurableValuesConfig {

    @Value("${configuration.endpoints.execution-fact._list.max-page-size}")
    private Integer maxPageSize;

    @Bean(name = "executionFactsMaxPageSize")
    public Integer getExecutionFactsMaxPageSize() {
        return maxPageSize;
    }
}
