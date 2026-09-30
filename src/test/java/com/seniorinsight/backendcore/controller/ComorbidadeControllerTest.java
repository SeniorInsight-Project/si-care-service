package com.seniorinsight.backendcore.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.dto.ComorbidadeRequestDTO;
import com.seniorinsight.backendcore.dto.ComorbidadesResponseDTO;
import com.seniorinsight.backendcore.exception.ComorbidadeJaCadastradaException;
import com.seniorinsight.backendcore.exception.ComorbidadeNaoEncontradaException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.model.StatusUsuario;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.repository.UsuarioRepository;
import com.seniorinsight.backendcore.security.JwtAuthenticationFilter;
import com.seniorinsight.backendcore.security.JwtService;
import com.seniorinsight.backendcore.service.ComorbidadeService;

@WebMvcTest(controllers = ComorbidadeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ComorbidadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComorbidadeService comorbidadeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private UUID idosoId;
    private Usuario mockUsuario;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();
        mockUsuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@seniorinsight.com")
                .status(StatusUsuario.ATIVO)
                .build();

        when(jwtService.validateAndGetSubject(any())).thenReturn("usuario@seniorinsight.com");
        when(usuarioRepository.findByEmail("usuario@seniorinsight.com")).thenReturn(Optional.of(mockUsuario));
    }

    // =========================================================================
    // POST /api/v1/idosos/{idosoId}/comorbidades (HU15)
    // =========================================================================

    @Test
    @DisplayName("POST comorbidades deve retornar 401 quando não autenticado")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Hipertensão"));

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/comorbidades", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "usuario@seniorinsight.com")
    @DisplayName("POST comorbidades deve retornar 200 e lista atualizada quando autenticado e autorizado")
    void deveAdicionarComorbidadesComSucesso() throws Exception {
        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Diabetes Mellitus Tipo 2", "Hipertensão"));
        ComorbidadesResponseDTO response = new ComorbidadesResponseDTO(
                idosoId,
                List.of("Diabetes Mellitus Tipo 2", "Hipertensão"),
                Instant.now()
        );

        when(comorbidadeService.adicionarComorbidades(eq(idosoId), any(ComorbidadeRequestDTO.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/comorbidades", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idosoId").value(idosoId.toString()))
                .andExpect(jsonPath("$.comorbidades[0]").value("Diabetes Mellitus Tipo 2"))
                .andExpect(jsonPath("$.comorbidades[1]").value("Hipertensão"));
    }

    @Test
    @WithMockUser(username = "usuario@seniorinsight.com")
    @DisplayName("POST comorbidades deve retornar 400 quando lista de comorbidades for vazia (Jakarta Validation)")
    void deveRetornar400QuandoListaComorbidadesVazia() throws Exception {
        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(Collections.emptyList());

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/comorbidades", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Validação"));
    }

    @Test
    @WithMockUser(username = "usuario@seniorinsight.com")
    @DisplayName("POST comorbidades deve retornar 403 quando usuário não possuir perfil autorizado")
    void deveRetornar403QuandoPerfilNaoAutorizado() throws Exception {
        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Hipertensão"));

        when(comorbidadeService.adicionarComorbidades(eq(idosoId), any(ComorbidadeRequestDTO.class), any()))
                .thenThrow(new PerfilNaoAutorizadoException("Apenas perfis autorizados (Familiar ou Médico) podem alterar as comorbidades do idoso."));

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/comorbidades", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acesso Proibido"))
                .andExpect(jsonPath("$.message").value("Apenas perfis autorizados (Familiar ou Médico) podem alterar as comorbidades do idoso."));
    }

    @Test
    @WithMockUser(username = "usuario@seniorinsight.com")
    @DisplayName("POST comorbidades deve retornar 404 quando o idoso não for encontrado")
    void deveRetornar404QuandoIdosoNaoEncontrado() throws Exception {
        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Hipertensão"));

        when(comorbidadeService.adicionarComorbidades(eq(idosoId), any(ComorbidadeRequestDTO.class), any()))
                .thenThrow(new IdosoNaoEncontradoException(idosoId));

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/comorbidades", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
    }

    @Test
    @WithMockUser(username = "usuario@seniorinsight.com")
    @DisplayName("POST comorbidades deve retornar 409 quando todas as comorbidades informadas já estiverem cadastradas")
    void deveRetornar409QuandoComorbidadeJaCadastrada() throws Exception {
        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Hipertensão"));

        when(comorbidadeService.adicionarComorbidades(eq(idosoId), any(ComorbidadeRequestDTO.class), any()))
                .thenThrow(new ComorbidadeJaCadastradaException("Todas as comorbidades informadas já estão cadastradas para este idoso."));

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/comorbidades", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Comorbidade Já Cadastrada"));
    }

    // =========================================================================
    // DELETE /api/v1/idosos/{idosoId}/comorbidades/{nomeComorbidade} (HU15)
    // =========================================================================

    @Test
    @WithMockUser(username = "usuario@seniorinsight.com")
    @DisplayName("DELETE comorbidade deve retornar 204 No Content ao remover comorbidade com sucesso")
    void deveRemoverComorbidadeComSucesso() throws Exception {
        ComorbidadesResponseDTO response = new ComorbidadesResponseDTO(idosoId, Collections.emptyList(), Instant.now());
        when(comorbidadeService.removerComorbidade(eq(idosoId), eq("Hipertensao"), any()))
                .thenReturn(response);

        mockMvc.perform(delete("/api/v1/idosos/{idosoId}/comorbidades/{nomeComorbidade}", idosoId, "Hipertensao"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "usuario@seniorinsight.com")
    @DisplayName("DELETE comorbidade deve retornar 404 quando comorbidade não existir no perfil do idoso")
    void deveRetornar404AoRemoverComorbidadeInexistente() throws Exception {
        doThrow(new ComorbidadeNaoEncontradaException("Asma"))
                .when(comorbidadeService).removerComorbidade(eq(idosoId), eq("Asma"), any());

        mockMvc.perform(delete("/api/v1/idosos/{idosoId}/comorbidades/{nomeComorbidade}", idosoId, "Asma"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Comorbidade Não Encontrada"));
    }

    // =========================================================================
    // GET /api/v1/idosos/{idosoId}/comorbidades
    // =========================================================================

    @Test
    @WithMockUser(username = "usuario@seniorinsight.com")
    @DisplayName("GET comorbidades deve retornar 200 e lista de comorbidades")
    void deveListarComorbidadesComSucesso() throws Exception {
        ComorbidadesResponseDTO response = new ComorbidadesResponseDTO(
                idosoId,
                List.of("Hipertensão Arterial"),
                Instant.now()
        );

        when(comorbidadeService.listarComorbidades(eq(idosoId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/idosos/{idosoId}/comorbidades", idosoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idosoId").value(idosoId.toString()))
                .andExpect(jsonPath("$.comorbidades[0]").value("Hipertensão Arterial"));
    }
}
