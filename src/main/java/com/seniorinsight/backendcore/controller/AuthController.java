package com.seniorinsight.backendcore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.AuthResponseDTO;
import com.seniorinsight.backendcore.dto.CompletarPerfilRequestDTO;
import com.seniorinsight.backendcore.dto.LoginRequestDTO;
import com.seniorinsight.backendcore.dto.RegistroRequestDTO;
import com.seniorinsight.backendcore.dto.SsoLoginRequestDTO;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.service.AuthService;
import com.seniorinsight.backendcore.service.GoogleAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller REST para os fluxos de autenticação do SeniorInsight.
 * Suporta HU-001 (SSO Google) e HU-002 (Login Tradicional, Registro e Conclusão de Perfil).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação de usuários, SSO Google, login tradicional e gestão de perfis")
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final AuthService authService;

    public AuthController(GoogleAuthService googleAuthService, AuthService authService) {
        this.googleAuthService = googleAuthService;
        this.authService = authService;
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

    @Operation(
            summary = "Login Tradicional (HU-002)",
            description = "Autentica um usuário existente por e-mail e senha utilizando verificação segura de hash BCrypt. " +
                    "Retorna o token JWT e sinaliza se o perfil cadastral do usuário está completo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticação bem-sucedida. Retorna o token JWT e estado do perfil.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos ou malformados."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas ou usuário bloqueado/inativo."
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Limite de tentativas de login excedido (Rate Limiting defensivo)."
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.autenticarTradicional(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Registro de Novo Usuário e Perfil Inicial (HU-002)",
            description = "Cadastra uma nova conta no sistema com senha hasheada via BCrypt e cria o perfil físico associado " +
                    "(Familiar ou Médico), validando unicidade de e-mail, CPF e CRM. Retorna o token JWT autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário e perfil registrados com sucesso. Retorna o token JWT.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados obrigatórios ausentes ou tipo de perfil inválido."
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail, CPF ou CRM já cadastrados anteriormente no sistema."
            )
    })
    @PostMapping("/registro")
    public ResponseEntity<AuthResponseDTO> registro(@Valid @RequestBody RegistroRequestDTO request) {
        AuthResponseDTO response = authService.registrarNovoUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Completar Perfil de Usuário SSO (HU-002)",
            description = "Permite que usuários criados via Google SSO (que possuem apenas e-mail e sso_id) completem " +
                    "seu perfil como Familiar ou Médico. Rota protegida que exige o Bearer Token JWT obtido no login do Google.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil completado com sucesso. Retorna novo token JWT com status atualizado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados obrigatórios ausentes ou tipo de perfil inválido."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Acesso não autorizado. Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "O perfil deste usuário já foi preenchido, ou CPF/CRM informado já existe."
            )
    })
    @PostMapping("/completar-perfil")
    public ResponseEntity<AuthResponseDTO> completarPerfil(
            @AuthenticationPrincipal Usuario usuarioAutenticado,
            @Valid @RequestBody CompletarPerfilRequestDTO request) {
        AuthResponseDTO response = authService.completarPerfilSso(usuarioAutenticado, request);
        return ResponseEntity.ok(response);
    }
}
