package com.seniorinsight.backendcore.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuração de cliente HTTP (RestTemplate) para integração com a plataforma OpenWearables.
 * Configura timeouts de conexão e leitura para garantir resiliência e não bloquear threads do pool.
 */
@Configuration
public class OpenWearablesConfig {

    @Bean
    public RestTemplate openWearablesRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
