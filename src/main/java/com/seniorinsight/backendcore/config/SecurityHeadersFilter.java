package com.seniorinsight.backendcore.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de Segurança HTTP (OWASP Secure Headers).
 * Aplica cabeçalhos defensivos essenciais em todas as respostas da API.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Previne que o navegador tente inferir o tipo de mídia (MIME-sniffing)
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Bloqueia embutimento da aplicação em iframes (Prevenção contra Clickjacking)
        response.setHeader("X-Frame-Options", "DENY");

        // Força conexões HTTPS estritas (HSTS) por 1 ano com inclusão de subdomínios
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Content Security Policy defensiva para API REST (restringe fontes e impede frames)
        response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';");

        // Controla o envio de dados de referência (Referrer)
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Restringe acesso a recursos sensíveis do cliente (câmera, microfone, geolocalização)
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        filterChain.doFilter(request, response);
    }
}
