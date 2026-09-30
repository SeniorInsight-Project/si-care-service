package com.seniorinsight.backendcore.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuração de execução assíncrona do Spring Boot (@EnableAsync).
 * Fornece um ThreadPool otimizado para o processamento em background de alarmes de emergência (HU35),
 * garantindo isolamento de threads e cumprimento rigoroso do SLA de resposta (< 100ms).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "sosTaskExecutor")
    public Executor sosTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("sos-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
