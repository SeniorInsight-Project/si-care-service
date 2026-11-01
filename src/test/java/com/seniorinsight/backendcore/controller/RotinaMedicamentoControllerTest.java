package com.seniorinsight.backendcore.controller;

import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.domain.StatusIngestao;
import com.seniorinsight.backendcore.dto.AtualizarRotinaRequestDTO;
import com.seniorinsight.backendcore.dto.ConfirmarIngestaoRequestDTO;
import com.seniorinsight.backendcore.dto.HistoricoMedicacaoResponseDTO;
import com.seniorinsight.backendcore.dto.RotinaMedicamentoRequestDTO;
import com.seniorinsight.backendcore.dto.RotinaMedicamentoResponseDTO;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.RotinaMedicamentoNaoEncontradaException;
import com.seniorinsight.backendcore.service.RotinaMedicamentoService;

@WebMvcTest(controllers = RotinaMedicamentoController.class)
@Import(SecurityConfig.class)
class RotinaMedicamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RotinaMedicamentoService rotinaMedicamentoService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private UUID idosoId;
    private UUID rotinaId;
    private UUID medicamentoId;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();
        rotinaId = UUID.randomUUID();
        medicamentoId = UUID.randomUUID();


    }

    // =========================================================================
    // POST /api/v1/idosos/{idosoId}/rotinas-medicamentos
    // =========================================================================

    @Test
    @DisplayName("POST rotinas-medicamentos deve retornar 401 quando não autenticado")
    void deveRetornar401AoCriarRotinaNaoAutenticado() throws Exception {
        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                null, "Losartana", "50mg", null, LocalTime.of(8, 0), "Diário", null, null, null, null
        );

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/rotinas-medicamentos", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST rotinas-medicamentos deve retornar 201 quando dados forem válidos")
    void deveCriarRotinaComSucesso() throws Exception {
        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                null, "Losartana Potássica", "50mg", "Tomar em jejum", LocalTime.of(8, 0), "1x ao dia", "TODOS", null, null, null
        );

        RotinaMedicamentoResponseDTO response = new RotinaMedicamentoResponseDTO(
                rotinaId,
                idosoId,
                "Sebastião Ferreira",
                medicamentoId,
                "Losartana Potássica",
                "50mg",
                "Tomar em jejum",
                LocalTime.of(8, 0),
                "1x ao dia",
                "TODOS",
                LocalDate.of(2026, 10, 1),
                null,
                true,
                null,
                StatusIngestao.PENDENTE,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(rotinaMedicamentoService.criarRotina(eq(idosoId), any(RotinaMedicamentoRequestDTO.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/rotinas-medicamentos", idosoId)
                        .with(tokenFamiliar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(rotinaId.toString()))
                .andExpect(jsonPath("$.idosoId").value(idosoId.toString()))
                .andExpect(jsonPath("$.medicamentoNome").value("Losartana Potássica"))
                .andExpect(jsonPath("$.dosagem").value("50mg"))
                .andExpect(jsonPath("$.statusHoje").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST rotinas-medicamentos deve retornar 400 quando horário ou frequência forem omitidos")
    void deveRetornar400AoCriarRotinaComCamposObrigatoriosAusentes() throws Exception {
        // Horário nulo e frequência em branco
        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                null, "Losartana", "50mg", null, null, "", null, null, null, null
        );

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/rotinas-medicamentos", idosoId)
                        .with(tokenFamiliar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.horario").exists())
                .andExpect(jsonPath("$.errors.frequencia").exists());
    }

    @Test
    @DisplayName("POST rotinas-medicamentos deve retornar 404 quando idoso não existir")
    void deveRetornar404AoCriarRotinaIdosoInexistente() throws Exception {
        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                null, "Losartana", "50mg", null, LocalTime.of(8, 0), "Diário", null, null, null, null
        );

        when(rotinaMedicamentoService.criarRotina(eq(idosoId), any(), any()))
                .thenThrow(new IdosoNaoEncontradoException(idosoId));

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/rotinas-medicamentos", idosoId)
                        .with(tokenFamiliar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
    }

    // =========================================================================
    // GET /api/v1/idosos/{idosoId}/rotinas-medicamentos
    // =========================================================================

    @Test
    @DisplayName("GET rotinas-medicamentos deve retornar 401 quando não autenticado")
    void deveRetornar401AoListarRotinasNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/idosos/{idosoId}/rotinas-medicamentos", idosoId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET rotinas-medicamentos deve retornar 200 com lista cronológica para React Native")
    void deveListarRotinasDiariasComSucesso() throws Exception {
        RotinaMedicamentoResponseDTO rotinaDTO = new RotinaMedicamentoResponseDTO(
                rotinaId,
                idosoId,
                "Sebastião Ferreira",
                medicamentoId,
                "Losartana Potássica",
                "50mg",
                "1 comp manhã",
                LocalTime.of(8, 0),
                "1x ao dia",
                "TODOS",
                LocalDate.of(2026, 1, 1),
                null,
                true,
                null,
                StatusIngestao.TOMADO,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        when(rotinaMedicamentoService.listarRotinasDiarias(eq(idosoId), any(), eq(true), any()))
                .thenReturn(List.of(rotinaDTO));

        mockMvc.perform(get("/api/v1/idosos/{idosoId}/rotinas-medicamentos", idosoId)
                        .with(tokenFamiliar())
                        .param("apenasAtivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rotinaId.toString()))
                .andExpect(jsonPath("$[0].medicamentoNome").value("Losartana Potássica"))
                .andExpect(jsonPath("$[0].horario").value("08:00"))
                .andExpect(jsonPath("$[0].statusHoje").value("TOMADO"));
    }

    // =========================================================================
    // POST /api/v1/rotinas-medicamentos/{rotinaId}/confirmar-ingestao
    // =========================================================================

    @Test
    @DisplayName("POST confirmar-ingestao deve retornar 401 quando não autenticado")
    void deveRetornar401AoConfirmarIngestaoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/api/v1/rotinas-medicamentos/{rotinaId}/confirmar-ingestao", rotinaId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST confirmar-ingestao deve retornar 201 com histórico e tolerância de 30 min validada")
    void deveConfirmarIngestaoComSucesso() throws Exception {
        ConfirmarIngestaoRequestDTO request = new ConfirmarIngestaoRequestDTO(
                Instant.now(), LocalDate.now(), "Tomou direitinho", null
        );

        HistoricoMedicacaoResponseDTO response = new HistoricoMedicacaoResponseDTO(
                UUID.randomUUID(),
                rotinaId,
                "Losartana Potássica",
                "50mg",
                StatusIngestao.TOMADO,
                Instant.now(),
                Instant.now().minusSeconds(600),
                true,
                10L,
                "Tomou direitinho",
                "familiar@seniorinsight.com",
                "Ingestão confirmada com sucesso dentro da janela de tolerância de 30 minutos.",
                Instant.now()
        );

        when(rotinaMedicamentoService.confirmarIngestao(eq(rotinaId), any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/rotinas-medicamentos/{rotinaId}/confirmar-ingestao", rotinaId)
                        .with(tokenFamiliar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rotinaId").value(rotinaId.toString()))
                .andExpect(jsonPath("$.status").value("TOMADO"))
                .andExpect(jsonPath("$.dentroJanelaTolerancia").value(true))
                .andExpect(jsonPath("$.diferencaMinutos").value(10));
    }

    @Test
    @DisplayName("POST confirmar-ingestao deve retornar 404 quando rotina não for encontrada")
    void deveRetornar404AoConfirmarIngestaoRotinaInexistente() throws Exception {
        when(rotinaMedicamentoService.confirmarIngestao(eq(rotinaId), any(), any()))
                .thenThrow(new RotinaMedicamentoNaoEncontradaException(rotinaId));

        mockMvc.perform(post("/api/v1/rotinas-medicamentos/{rotinaId}/confirmar-ingestao", rotinaId)
                        .with(tokenFamiliar()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Rotina de Medicamento Não Encontrada"));
    }

    // =========================================================================
    // Operações CRUD Adicionais (GET, PUT, DELETE)
    // =========================================================================

    @Test
    @DisplayName("GET rotinas-medicamentos/{rotinaId} deve retornar 200 quando encontrada")
    void deveObterRotinaPorIdComSucesso() throws Exception {
        RotinaMedicamentoResponseDTO response = new RotinaMedicamentoResponseDTO(
                rotinaId, idosoId, "Sebastião Ferreira", medicamentoId, "Losartana", "50mg",
                null, LocalTime.of(8, 0), "Diário", "TODOS", LocalDate.now(), null, true,
                null, StatusIngestao.PENDENTE, null, null, Instant.now(), Instant.now()
        );

        when(rotinaMedicamentoService.obterRotinaPorId(eq(rotinaId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/rotinas-medicamentos/{rotinaId}", rotinaId)
                        .with(tokenFamiliar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rotinaId.toString()));
    }

    @Test
    @DisplayName("PUT rotinas-medicamentos/{rotinaId} deve retornar 200 ao atualizar")
    void deveAtualizarRotinaComSucesso() throws Exception {
        AtualizarRotinaRequestDTO request = new AtualizarRotinaRequestDTO(
                LocalTime.of(9, 0), "Diário", "TODOS", null, null, true, "Atualizado", "100mg", null
        );

        RotinaMedicamentoResponseDTO response = new RotinaMedicamentoResponseDTO(
                rotinaId, idosoId, "Sebastião Ferreira", medicamentoId, "Losartana", "100mg",
                null, LocalTime.of(9, 0), "Diário", "TODOS", LocalDate.now(), null, true,
                "Atualizado", StatusIngestao.PENDENTE, null, null, Instant.now(), Instant.now()
        );

        when(rotinaMedicamentoService.atualizarRotina(eq(rotinaId), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/rotinas-medicamentos/{rotinaId}", rotinaId)
                        .with(tokenFamiliar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horario").value("09:00"))
                .andExpect(jsonPath("$.dosagem").value("100mg"));
    }

    @Test
    @DisplayName("DELETE rotinas-medicamentos/{rotinaId} deve retornar 204")
    void deveRemoverRotinaComSucesso() throws Exception {
        mockMvc.perform(delete("/api/v1/rotinas-medicamentos/{rotinaId}", rotinaId)
                        .with(tokenFamiliar()))
                .andExpect(status().isNoContent());
    }

    private static JwtRequestPostProcessor tokenFamiliar() {
        return jwt().jwt(j -> j.subject("sub-familiar")
                .claim("email", "familiar@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of("familiar"))));
    }
}
