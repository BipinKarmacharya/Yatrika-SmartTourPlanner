package com.yatrika.subscription.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KhaltiConfig {

    @Value("${khalti.api.base-url:https://a.khalti.com/api/v2}")
    private String baseUrl;

    @Value("${khalti.api.secret-key}")
    private String secretKey;

    @Bean
    public WebClient khaltiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Key " + secretKey)
                .build();
    }
}