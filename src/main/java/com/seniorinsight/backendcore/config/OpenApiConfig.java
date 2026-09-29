package com.seniorinsight.backendcore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configuração da documentação interativa OpenAPI 3 / Swagger UI.
 * Adiciona suporte a autenticação Bearer JWT (botão Authorize).
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SeniorInsight API")
                        .version("1.0.0")
                        .description("API REST do Backend Core da plataforma SeniorInsight - Épico 1: Acesso e Segurança")
                        .contact(new Contact()
                                .name("SeniorInsight Engineering")
                                .email("dev@seniorinsight.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://seniorinsight.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT gerado no endpoint /api/v1/auth/sso")));
    }
}
