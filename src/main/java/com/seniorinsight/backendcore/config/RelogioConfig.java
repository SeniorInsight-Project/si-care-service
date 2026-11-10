package com.seniorinsight.backendcore.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio injetável, para os serviços que dependem da data atual (idade, carimbo de auditoria) serem testáveis. */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
