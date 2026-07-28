package com.aiincidentcommander.agent_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PrometheusClientConfig {

    @Bean("prometheusRestClient")
    public RestClient prometheusRestClient(@Value("${prometheus.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
