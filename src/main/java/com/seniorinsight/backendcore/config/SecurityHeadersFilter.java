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
 * Aplica cabeçalhos defensivos essenciais em todas as respostas da API,
 * com ajuste específico de CSP para Swagger UI e tela de teste do Google SSO.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            // Permite scripts e estilos do Swagger UI
            response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");
        } else if (path.equals("/test-login.html")) {
            // Permite carregar o SDK oficial do Google Sign-In e exibir o popup de autenticação
            response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' https://accounts.google.com/gsi/client; style-src 'self' 'unsafe-inline'; frame-src https://accounts.google.com; connect-src 'self' https://accounts.google.com;");
        } else {
            // CSP estrito para todas as rotas da API REST
            response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';");
        }

        filterChain.doFilter(request, response);
    }
}
