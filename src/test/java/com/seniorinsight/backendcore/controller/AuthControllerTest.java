package com.seniorinsight.backendcore.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.dto.AuthResponseDTO;
import com.seniorinsight.backendcore.dto.CompletarPerfilRequestDTO;
import com.seniorinsight.backendcore.dto.LoginRequestDTO;
import com.seniorinsight.backendcore.dto.RegistroRequestDTO;
import com.seniorinsight.backendcore.exception.CredenciaisInvalidasException;
import com.seniorinsight.backendcore.exception.EmailJaCadastradoException;
import com.seniorinsight.backendcore.repository.UsuarioRepository;
import com.seniorinsight.backendcore.security.JwtAuthenticationFilter;
import com.seniorinsight.backendcore.security.JwtService;
import com.seniorinsight.backendcore.service.AuthService;
import com.seniorinsight.backendcore.service.GoogleAuthService;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private GoogleAuthService googleAuthService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    // =========================================================================
    // POST /api/v1/auth/login (HU-002)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/auth/login deve retornar 200 e AuthResponseDTO no login bem-sucedido")
    void deveFazerLoginComSucesso() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("teste@seniorinsight.com", "SenhaForte@123");
        AuthResponseDTO response = new AuthResponseDTO("jwt.token.mockado", true);

        when(authService.autenticarTradicional(any(LoginRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.mockado"))
                .andExpect(jsonPath("$.perfilCompleto").value(true))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login deve retornar 400 se campos obrigatórios estiverem vazios")
    void deveRetornar400QuandoLoginInvalido() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Validação"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login deve retornar 401 para credenciais incorretas")
    void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("teste@seniorinsight.com", "SenhaIncorreta");

        when(authService.autenticarTradicional(any(LoginRequestDTO.class)))
                .thenThrow(new CredenciaisInvalidasException("E-mail ou senha inválidos."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais Inválidas"));
    }

    // =========================================================================
    // POST /api/v1/auth/registro (HU-002)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/auth/registro deve retornar 201 e criar novo usuário com perfil")
    void deveRegistrarUsuarioComSucesso() throws Exception {
        RegistroRequestDTO request = new RegistroRequestDTO(
                "novo@seniorinsight.com",
                "SenhaForte@123",
                "Nome Completo",
                "123.456.789-00",
                "(11) 98765-4321",
                "FAMILIAR",
                null,
                null
        );
        AuthResponseDTO response = new AuthResponseDTO("jwt.token.registro", true);

        when(authService.registrarNovoUsuario(any(RegistroRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.registro"))
                .andExpect(jsonPath("$.perfilCompleto").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/registro deve retornar 409 quando email já cadastrado")
    void deveRetornar409QuandoEmailDuplicado() throws Exception {
        RegistroRequestDTO request = new RegistroRequestDTO(
                "duplicado@seniorinsight.com",
                "SenhaForte@123",
                "Nome",
                "123.456.789-00",
                "tel",
                "FAMILIAR",
                null,
                null
        );

        when(authService.registrarNovoUsuario(any(RegistroRequestDTO.class)))
                .thenThrow(new EmailJaCadastradoException("O e-mail informado já está cadastrado."));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("E-mail Já Cadastrado"));
    }

    // =========================================================================
    // POST /api/v1/auth/completar-perfil (HU-002) - Protegido por Spring Security
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/auth/completar-perfil deve retornar 401 quando não autenticado")
    void deveRetornar401QuandoCompletarPerfilSemToken() throws Exception {
        CompletarPerfilRequestDTO request = new CompletarPerfilRequestDTO(
                "Nome", "123.456.789-00", "tel", "FAMILIAR", null, null);

        mockMvc.perform(post("/api/v1/auth/completar-perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "usuario.sso@gmail.com")
    @DisplayName("POST /api/v1/auth/completar-perfil deve retornar 200 quando autenticado")
    void deveRetornar200QuandoCompletarPerfilAutenticado() throws Exception {
        CompletarPerfilRequestDTO request = new CompletarPerfilRequestDTO(
                "Nome Completo", "123.456.789-00", "tel", "FAMILIAR", null, null);
        AuthResponseDTO response = new AuthResponseDTO("jwt.token.atualizado", true);

        when(authService.completarPerfilSso(any(), any(CompletarPerfilRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/completar-perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.atualizado"))
                .andExpect(jsonPath("$.perfilCompleto").value(true));
    }
}
