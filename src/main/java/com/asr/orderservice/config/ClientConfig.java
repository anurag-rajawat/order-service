package com.asr.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientConfig {
    @Bean
    WebClient webClient(ClientProperties clientProperties, WebClient.Builder builder) {
        return builder
                .baseUrl(clientProperties.catalogServiceUri().toString())
                .build();
    }
}
