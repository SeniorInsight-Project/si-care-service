package com.seniorinsight.backendcore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.AuthResponseDTO;
import com.seniorinsight.backendcore.dto.SsoLoginRequestDTO;
import com.seniorinsight.backendcore.service.GoogleAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller REST para os fluxos de autenticação do SeniorInsight (HU-001).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação de usuários, SSO e controle de acesso")
public class AuthController {

    private final GoogleAuthService googleAuthService;

    public AuthController(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @Operation(
            summary = "Login / Cadastro via Single Sign-On (Google)",
            description = "Valida o Google ID Token fornecido pelo cliente, realiza o provisionamento Just-In-Time (JIT) " +
                    "na base de dados caso seja o primeiro acesso e retorna o token JWT assinado da aplicação."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticação realizada com sucesso. Retorna o token JWT e status de perfil.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros da requisição ausentes, inválidos ou provedor não suportado."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token do Google inválido, expirado, forjado ou com e-mail não verificado."
            )
    })
    @PostMapping("/sso")
    public ResponseEntity<AuthResponseDTO> loginSso(@Valid @RequestBody SsoLoginRequestDTO request) {
        if (!"GOOGLE".equalsIgnoreCase(request.provider())) {
            throw new IllegalArgumentException("Provedor SSO não suportado no momento: " + request.provider() + ". Utilize 'GOOGLE'.");
        }

        AuthResponseDTO response = googleAuthService.authenticateWithGoogle(request.idToken());
        return ResponseEntity.ok(response);
    }
}
