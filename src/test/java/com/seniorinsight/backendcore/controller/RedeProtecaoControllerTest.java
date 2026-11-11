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
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.seniorinsight.backendcore.dto.ConvitePublicoDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeRequestDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeResponseDTO;
import com.seniorinsight.backendcore.dto.MembroRedeResponseDTO;
import com.seniorinsight.backendcore.exception.ConviteExpiradoException;
import com.seniorinsight.backendcore.exception.ConviteInvalidoException;
import com.seniorinsight.backendcore.exception.ConviteJaAceitoException;
import com.seniorinsight.backendcore.exception.ConviteNaoEncontradoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
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
    // GET /api/v1/convites?token=
    // =========================================================================

    @Test
    @DisplayName("GET consultar convite deve retornar 401 sem token JWT e não chegar ao serviço")
    void deveRetornar401AoConsultarConviteNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/convites").param("token", "abc"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(redeProtecaoService);
    }

    @Test
    @DisplayName("GET consultar convite deve retornar 200 só com os campos do ConvitePublico")
    void deveConsultarConviteComSucesso() throws Exception {
        String token = UUID.randomUUID().toString();
        Instant expiraEm = Instant.parse("2026-11-20T18:30:00Z");

        when(redeProtecaoService.consultarConvite(eq(token), any()))
                .thenReturn(new ConvitePublicoDTO("Carlos Eduardo Souza", "Maria", "CUIDADOR", "Filho",
                        expiraEm, StatusConvite.PENDENTE));

        mockMvc.perform(get("/api/v1/convites").with(tokenFamiliar()).param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeConvidante").value("Carlos Eduardo Souza"))
                .andExpect(jsonPath("$.primeiroNomeIdoso").value("Maria"))
                .andExpect(jsonPath("$.papelAcesso").value("CUIDADOR"))
                .andExpect(jsonPath("$.grauParentesco").value("Filho"))
                .andExpect(jsonPath("$.expiraEm").value("2026-11-20T18:30:00Z"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("GET consultar convite deve retornar 404 quando o token não existe")
    void deveRetornar404AoConsultarConviteInexistente() throws Exception {
        when(redeProtecaoService.consultarConvite(eq("inexistente"), any()))
                .thenThrow(new ConviteNaoEncontradoException("Convite não encontrado"));

        mockMvc.perform(get("/api/v1/convites").with(tokenFamiliar()).param("token", "inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Convite não encontrado"))
                .andExpect(jsonPath("$.path").value("/api/v1/convites"));
    }

    @Test
    @DisplayName("GET consultar convite deve retornar 409 quando o convite já foi aceito")
    void deveRetornar409AoConsultarConviteJaAceito() throws Exception {
        when(redeProtecaoService.consultarConvite(eq("aceito"), any()))
                .thenThrow(new ConviteJaAceitoException("Convite já foi aceito"));

        mockMvc.perform(get("/api/v1/convites").with(tokenFamiliar()).param("token", "aceito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Convite já foi aceito"));
    }

    @Test
    @DisplayName("GET consultar convite deve retornar 410 quando o convite expirou")
    void deveRetornar410AoConsultarConviteExpirado() throws Exception {
        when(redeProtecaoService.consultarConvite(eq("expirado"), any()))
                .thenThrow(new ConviteExpiradoException("Convite expirado"));

        mockMvc.perform(get("/api/v1/convites").with(tokenFamiliar()).param("token", "expirado"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.message").value("Convite expirado"));
    }

    @Test
    @DisplayName("GET consultar convite deve retornar 400 quando o token não é enviado")
    void deveRetornar400AoConsultarConviteSemToken() throws Exception {
        mockMvc.perform(get("/api/v1/convites").with(tokenFamiliar()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
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
                .andExpect(jsonPath("$[0].usuarioId").value("sub-carlos"))
                .andExpect(jsonPath("$[0].keycloakSub").doesNotExist())
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

    // =========================================================================
    // Autorização por idoso: sem token (401) e sem vínculo (403)
    // =========================================================================

    private static final String CONVITE = "{\"email\":\"neta@seniorinsight.com\",\"papelAcesso\":\"FAMILIAR\",\"grauParentesco\":\"Neta\"}";

    @Test
    @DisplayName("As rotas de convite e de rede do idoso respondem 401 sem token e não chegam ao serviço")
    void rotasPorIdosoRetornam401SemToken() throws Exception {
        mockMvc.perform(post("/api/v1/idosos/{idosoId}/convites", idosoId)
                .contentType(MediaType.APPLICATION_JSON).content(CONVITE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/idosos/{idosoId}/rede", idosoId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/convites/aceitar").param("token", "abc")).andExpect(status().isUnauthorized());

        verifyNoInteractions(redeProtecaoService);
    }

    @Test
    @DisplayName("Sem vínculo com o idoso, gerar convite e listar a rede respondem 404 com o Erro do contrato")
    void rotasPorIdosoRetornam403SemVinculo() throws Exception {
        IdosoNaoEncontradoException foraDaRede = new IdosoNaoEncontradoException(idosoId);
        when(redeProtecaoService.gerarConvite(eq(idosoId), any(), any())).thenThrow(foraDaRede);
        when(redeProtecaoService.listarMembrosRede(eq(idosoId), any())).thenThrow(foraDaRede);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/convites", idosoId).with(tokenFamiliar())
                        .contentType(MediaType.APPLICATION_JSON).content(CONVITE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"))
                .andExpect(jsonPath("$.message").value("Idoso não encontrado com o identificador: " + idosoId))
                .andExpect(jsonPath("$.path").value("/api/v1/idosos/" + idosoId + "/convites"));
        mockMvc.perform(get("/api/v1/idosos/{idosoId}/rede", idosoId).with(tokenFamiliar()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
    }

    private static JwtRequestPostProcessor tokenFamiliar() {
        return jwt().jwt(j -> j.subject("sub-familiar")
                .claim("email", "familiar@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of("familiar"))));
    }
}
