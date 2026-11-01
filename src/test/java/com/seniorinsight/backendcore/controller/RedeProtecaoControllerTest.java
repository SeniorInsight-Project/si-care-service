package com.seniorinsight.backendcore.controller;

import java.util.Map;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.domain.StatusConvite;
import com.seniorinsight.backendcore.dto.AceiteConviteResponseDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeRequestDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeResponseDTO;
import com.seniorinsight.backendcore.dto.MembroRedeResponseDTO;
import com.seniorinsight.backendcore.exception.ConviteExpiradoException;
import com.seniorinsight.backendcore.exception.ConviteInvalidoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.service.RedeProtecaoService;

@WebMvcTest(controllers = RedeProtecaoController.class)
@Import(SecurityConfig.class)
class RedeProtecaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedeProtecaoService redeProtecaoService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private UUID idosoId;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();
    }

    // =========================================================================
    // POST /api/v1/idosos/{idosoId}/convites
    // =========================================================================

    @Test
    @DisplayName("POST convite deve retornar 401 quando não autenticado")
    void deveRetornar401AoGerarConviteNaoAutenticado() throws Exception {
        ConviteRedeRequestDTO request = new ConviteRedeRequestDTO("convidado@email.com", "FAMILIAR", "Filho", null);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/convites", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST convite deve retornar 201 quando autenticado e válido")
    void deveGerarConviteComSucesso() throws Exception {
        ConviteRedeRequestDTO request = new ConviteRedeRequestDTO("convidado@email.com", "FAMILIAR", "Filho", "Olá!");
        ConviteRedeResponseDTO response = new ConviteRedeResponseDTO(
                UUID.randomUUID(),
                idosoId,
                "Dona Nair",
                "convidado@email.com",
                UUID.randomUUID().toString(),
                "FAMILIAR",
                "Filho",
                StatusConvite.PENDENTE,
                Instant.now().plusSeconds(172800),
                Instant.now(),
                "Convite gerado com sucesso."
        );

        when(redeProtecaoService.gerarConvite(eq(idosoId), any(ConviteRedeRequestDTO.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/convites", idosoId)
                        .with(tokenFamiliar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idosoId").value(idosoId.toString()))
                .andExpect(jsonPath("$.emailDestinatario").value("convidado@email.com"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST convite deve retornar 400 quando e-mail for inválido")
    void deveRetornar400QuandoEmailInvalido() throws Exception {
        ConviteRedeRequestDTO request = new ConviteRedeRequestDTO("email-invalido", "FAMILIAR", "Filho", null);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/convites", idosoId)
                        .with(tokenFamiliar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    // =========================================================================
    // POST /api/v1/convites/aceitar?token={token}
    // =========================================================================

    @Test
    @DisplayName("POST aceitar convite deve retornar 401 quando não autenticado")
    void deveRetornar401AoAceitarConviteNaoAutenticado() throws Exception {
        mockMvc.perform(post("/api/v1/convites/aceitar")
                        .param("token", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST aceitar convite deve retornar 200 quando token for válido")
    void deveAceitarConviteComSucesso() throws Exception {
        String token = UUID.randomUUID().toString();
        AceiteConviteResponseDTO response = new AceiteConviteResponseDTO(
                UUID.randomUUID(),
                idosoId,
                "Dona Nair",
                UUID.randomUUID(),
                "Carlos Silva",
                "Filho",
                "CUIDADOR_PRINCIPAL",
                Instant.now(),
                "Convite aceito com sucesso!"
        );

        when(redeProtecaoService.aceitarConvite(eq(token), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/convites/aceitar")
                        .with(tokenFamiliar())
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculoId").isNotEmpty())
                .andExpect(jsonPath("$.idosoId").value(idosoId.toString()))
                .andExpect(jsonPath("$.nomeFamiliar").value("Carlos Silva"));
    }

    @Test
    @DisplayName("POST aceitar convite deve retornar 410 quando convite estiver expirado")
    void deveRetornar410QuandoConviteExpirado() throws Exception {
        String token = UUID.randomUUID().toString();

        when(redeProtecaoService.aceitarConvite(eq(token), any()))
                .thenThrow(new ConviteExpiradoException("O convite expirou. O prazo de 48 horas foi ultrapassado."));

        mockMvc.perform(post("/api/v1/convites/aceitar")
                        .with(tokenFamiliar())
                        .param("token", token))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("Convite Expirado"));
    }

    @Test
    @DisplayName("POST aceitar convite deve retornar 400 quando token for inválido")
    void deveRetornar400QuandoTokenInvalido() throws Exception {
        String token = "token-falso";

        when(redeProtecaoService.aceitarConvite(eq(token), any()))
                .thenThrow(new ConviteInvalidoException("Convite não encontrado para o token informado."));

        mockMvc.perform(post("/api/v1/convites/aceitar")
                        .with(tokenFamiliar())
                        .param("token", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Convite Inválido"));
    }

    // =========================================================================
    // GET /api/v1/idosos/{idosoId}/rede
    // =========================================================================

    @Test
    @DisplayName("GET rede deve retornar 401 quando não autenticado")
    void deveRetornar401AoListarRedeNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/idosos/{idosoId}/rede", idosoId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET rede deve retornar 200 com lista de membros convertida para DTO")
    void deveListarMembrosRedeComSucesso() throws Exception {
        MembroRedeResponseDTO membro = new MembroRedeResponseDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "sub-carlos",
                "Carlos Silva",
                "carlos@email.com",
                "(11) 98888-7777",
                "Filho",
                "CUIDADOR_PRINCIPAL",
                Collections.emptyMap(),
                Instant.now()
        );

        when(redeProtecaoService.listarMembrosRede(eq(idosoId), any()))
                .thenReturn(List.of(membro));

        mockMvc.perform(get("/api/v1/idosos/{idosoId}/rede", idosoId)
                        .with(tokenFamiliar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeCompleto").value("Carlos Silva"))
                .andExpect(jsonPath("$[0].email").value("carlos@email.com"))
                .andExpect(jsonPath("$[0].grauParentesco").value("Filho"))
                .andExpect(jsonPath("$[0].papelAcesso").value("CUIDADOR_PRINCIPAL"));
    }

    @Test
    @DisplayName("GET rede deve retornar 404 quando idoso não existir")
    void deveRetornar404AoListarRedeIdosoInexistente() throws Exception {
        when(redeProtecaoService.listarMembrosRede(eq(idosoId), any()))
                .thenThrow(new IdosoNaoEncontradoException(idosoId));

        mockMvc.perform(get("/api/v1/idosos/{idosoId}/rede", idosoId)
                        .with(tokenFamiliar()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
    }

    private static JwtRequestPostProcessor tokenFamiliar() {
        return jwt().jwt(j -> j.subject("sub-familiar")
                .claim("email", "familiar@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of("familiar"))));
    }
}
