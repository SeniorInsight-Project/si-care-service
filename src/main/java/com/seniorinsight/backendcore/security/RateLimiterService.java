package com.seniorinsight.backendcore.security;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Serviço de Rate Limiting baseado em Redis para proteção contra ataques de força bruta
 * em fluxos críticos de autenticação (HU-002 Login, HU-003 Recuperação de Senha, HU-004 MFA).
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Tenta registrar uma requisição e verifica se o limite da janela de tempo foi excedido.
     *
     * @param key Identificador da operação (ex: "login:" + ip ou "mfa:" + email)
     * @param maxRequests Limite máximo de tentativas na janela
     * @param windowSeconds Tamanho da janela temporal em segundos
     * @return true se a requisição é permitida, false se o limite foi atingido
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        try {
            String redisKey = "ratelimit:" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);

            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }

            return count != null && count <= maxRequests;
        } catch (Exception ex) {
            // Degradação graciosa: se o Redis estiver inacessível, não bloqueia o fluxo do usuário
            log.warn("Falha de comunicação com Redis no Rate Limiting (chave: {}). Prosseguindo com degradação graciosa: {}",
                    key, ex.getMessage());
            return true;
        }
    }
}
