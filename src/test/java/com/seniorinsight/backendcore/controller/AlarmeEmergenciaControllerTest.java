package com.seniorinsight.backendcore.controller;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.domain.StatusAlarme;
import com.seniorinsight.backendcore.dto.AlarmeEmergenciaResponseDTO;
import com.seniorinsight.backendcore.dto.AtualizarStatusAlarmeRequestDTO;
import com.seniorinsight.backendcore.dto.DispararSosRequestDTO;
import com.seniorinsight.backendcore.exception.AlarmeNaoEncontradoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.model.StatusUsuario;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.repository.UsuarioRepository;
import com.seniorinsight.backendcore.security.JwtAuthenticationFilter;
import com.seniorinsight.backendcore.security.JwtService;
import com.seniorinsight.backendcore.service.AlarmeEmergenciaService;

@WebMvcTest(controllers = AlarmeEmergenciaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AlarmeEmergenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlarmeEmergenciaService alarmeEmergenciaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private UUID idosoId;
    private UUID alarmeId;
    private Usuario mockUsuario;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();
        alarmeId = UUID.randomUUID();

        mockUsuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("idoso@seniorinsight.com")
                .status(StatusUsuario.ATIVO)
                .build();

        when(jwtService.validateAndGetSubject(any())).thenReturn("idoso@seniorinsight.com");
        when(usuarioRepository.findByEmail("idoso@seniorinsight.com")).thenReturn(Optional.of(mockUsuario));
    }

    // =========================================================================
    // POST /api/v1/idosos/{idosoId}/sos (HU35 - SLA Crítico 202 Accepted)
    // =========================================================================

    @Test
    @DisplayName("POST /idosos/{idosoId}/sos deve retornar 401 quando não autenticado")
    void deveRetornar401AoDispararSosSemAutenticacao() throws Exception {
        DispararSosRequestDTO request = new DispararSosRequestDTO(-23.550520, -46.633308, 80, "Emergência");

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/sos", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /idosos/{idosoId}/sos deve retornar 202 Accepted com dados do alarme disparado")
    @WithMockUser(username = "idoso@seniorinsight.com")
    void deveRetornar202AoDispararSosComSucesso() throws Exception {
        DispararSosRequestDTO request = new DispararSosRequestDTO(-23.550520, -46.633308, 85, "Botão acionado");

        AlarmeEmergenciaResponseDTO responseDTO = new AlarmeEmergenciaResponseDTO(
                alarmeId,
                idosoId,
                "Sebastião da Silva",
                -23.550520,
                -46.633308,
                85,
                StatusAlarme.DISPARADO,
                "Botão acionado",
                Instant.now(),
                Instant.now(),
                "Alarme SOS de emergência disparado com sucesso."
        );

        when(alarmeEmergenciaService.dispararAlarme(eq(idosoId), any(DispararSosRequestDTO.class), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/sos", idosoId)
                        .header("Authorization", "Bearer token-mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(alarmeId.toString()))
                .andExpect(jsonPath("$.idosoId").value(idosoId.toString()))
                .andExpect(jsonPath("$.status").value("DISPARADO"))
                .andExpect(jsonPath("$.nivelBateria").value(85));
    }

    @Test
    @DisplayName("POST /idosos/{idosoId}/sos deve retornar 400 Bad Request quando telemetria de bateria for inválida (>100)")
    @WithMockUser(username = "idoso@seniorinsight.com")
    void deveRetornar400AoDispararComTelemetriaInvalida() throws Exception {
        // Nível de bateria > 100 é proibido pela validação Bean Validation
        DispararSosRequestDTO requestInvalido = new DispararSosRequestDTO(-23.550520, -46.633308, 150, "Erro");

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/sos", idosoId)
                        .header("Authorization", "Bearer token-mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nivelBateria").exists());
    }

    @Test
    @DisplayName("POST /idosos/{idosoId}/sos deve retornar 404 Not Found quando o idoso não existir")
    @WithMockUser(username = "idoso@seniorinsight.com")
    void deveRetornar404AoDispararParaIdosoInexistente() throws Exception {
        DispararSosRequestDTO request = new DispararSosRequestDTO(-23.550520, -46.633308, 80, null);

        when(alarmeEmergenciaService.dispararAlarme(eq(idosoId), any(), any()))
                .thenThrow(new IdosoNaoEncontradoException(idosoId));

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/sos", idosoId)
                        .header("Authorization", "Bearer token-mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
    }

    // =========================================================================
    // PUT /api/v1/sos/{alarmeId}/status (HU35)
    // =========================================================================

    @Test
    @DisplayName("PUT /sos/{alarmeId}/status deve retornar 200 OK ao atualizar status com sucesso")
    @WithMockUser(username = "idoso@seniorinsight.com")
    void deveRetornar200AoAtualizarStatusComSucesso() throws Exception {
        AtualizarStatusAlarmeRequestDTO request = new AtualizarStatusAlarmeRequestDTO(
                StatusAlarme.EM_ATENDIMENTO, "Equipe SAMU deslocada"
        );

        AlarmeEmergenciaResponseDTO responseDTO = new AlarmeEmergenciaResponseDTO(
                alarmeId,
                idosoId,
                "Sebastião da Silva",
                -23.550520,
                -46.633308,
                85,
                StatusAlarme.EM_ATENDIMENTO,
                "Equipe SAMU deslocada",
                Instant.now(),
                Instant.now(),
                "Status atualizado com sucesso."
        );

        when(alarmeEmergenciaService.atualizarStatus(eq(alarmeId), any(), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/sos/{alarmeId}/status", alarmeId)
                        .header("Authorization", "Bearer token-mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alarmeId.toString()))
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.observacao").value("Equipe SAMU deslocada"));
    }

    @Test
    @DisplayName("PUT /sos/{alarmeId}/status deve retornar 404 Not Found quando alarme não for localizado")
    @WithMockUser(username = "idoso@seniorinsight.com")
    void deveRetornar404AoAtualizarAlarmeInexistente() throws Exception {
        AtualizarStatusAlarmeRequestDTO request = new AtualizarStatusAlarmeRequestDTO(
                StatusAlarme.RESOLVIDO, "Finalizado"
        );

        when(alarmeEmergenciaService.atualizarStatus(eq(alarmeId), any(), any()))
                .thenThrow(new AlarmeNaoEncontradoException(alarmeId));

        mockMvc.perform(put("/api/v1/sos/{alarmeId}/status", alarmeId)
                        .header("Authorization", "Bearer token-mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Alarme Não Encontrado"));
    }
}
