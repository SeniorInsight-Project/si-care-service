package com.seniorinsight.backendcore.config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import com.seniorinsight.backendcore.security.ConversorRolesKeycloak;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Resource server: valida o JWT emitido pelo Keycloak (realm seniorinsight), configurado em
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}. O care-service não tem login próprio,
 * não emite token e não guarda senha.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/",
            "/api/v1/health",
            "/actuator/health", "/actuator/health/**",
            "/v3/api-docs", "/v3/api-docs/**",
            "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint naoAutorizado = (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                {
                  "timestamp": "%s",
                  "status": 401,
                  "error": "Não Autorizado",
                  "message": "Acesso não autorizado. Token JWT ausente, inválido ou expirado.",
                  "path": "%s"
                }
                """.formatted(Instant.now().toString(), request.getRequestURI()));
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorAutenticacao()))
                        .authenticationEntryPoint(naoAutorizado)
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(naoAutorizado));

        return http.build();
    }

    /** Authorities = scopes do token ({@code SCOPE_*}) + roles de realm do Keycloak ({@code ROLE_*}). */
    static JwtAuthenticationConverter conversorAutenticacao() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        ConversorRolesKeycloak roles = new ConversorRolesKeycloak();

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            authorities.addAll(roles.convert(jwt));
            return authorities;
        });
        return conversor;
    }
}
