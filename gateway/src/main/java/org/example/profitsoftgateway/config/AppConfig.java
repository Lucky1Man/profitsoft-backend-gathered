package org.example.profitsoftgateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${addresses.react-frontend}")
    private String reactAddress;

    @Bean
    public String reactAddress() {
        return reactAddress;
    }

}
