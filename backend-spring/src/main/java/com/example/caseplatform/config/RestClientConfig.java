package com.example.caseplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RestClientConfig {

    @Bean
    public WebClient auditWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${audit.base-url:http://localhost:8081/api}") String auditBaseUrl) {
        return webClientBuilder.baseUrl(auditBaseUrl).build();
    }
}
