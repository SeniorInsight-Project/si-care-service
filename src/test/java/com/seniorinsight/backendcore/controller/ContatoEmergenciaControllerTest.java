package com.seniorinsight.backendcore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.dto.ContatoEmergenciaRequestDTO;
import com.seniorinsight.backendcore.dto.ContatoEmergenciaResponseDTO;
import com.seniorinsight.backendcore.exception.ContatoEmergenciaNaoEncontradoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.LimiteContatosEmergenciaException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.service.ContatoEmergenciaService;

@WebMvcTest(controllers = ContatoEmergenciaController.class)
@Import(SecurityConfig.class)
class ContatoEmergenciaControllerTest {

    private static final String BASE = "/api/v1/idosos/{idosoId}/contatos-emergencia";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContatoEmergenciaService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    private final UUID idosoId = UUID.randomUUID();
    private final UUID contatoId = UUID.randomUUID();

    @Test
    @DisplayName("Todas as rotas respondem 401 sem token")
    void semTokenRetorna401() throws Exception {
        String corpo = corpo("João da Silva", "(11) 98765-4321", 1);

        mockMvc.perform(get(BASE, idosoId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE, idosoId).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE + "/{c}", idosoId, contatoId).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/{c}", idosoId, contatoId)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET lista os contatos no formato do contrato")
    void listar() throws Exception {
        when(service.listar(any(), eq(idosoId))).thenReturn(List.of(resposta(1), resposta(2)));

        mockMvc.perform(get(BASE, idosoId).with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(contatoId.toString()))
                .andExpect(jsonPath("$[0].idosoId").value(idosoId.toString()))
                .andExpect(jsonPath("$[0].telefone").value("+5511987654321"))
                .andExpect(jsonPath("$[0].prioridade").value(1));
    }

    @Test
    @DisplayName("POST cria e responde 201 com Location")
    void adicionar() throws Exception {
        when(service.adicionar(any(), eq(idosoId), any())).thenReturn(resposta(1));

        mockMvc.perform(post(BASE, idosoId).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("João da Silva", "(11) 98765-4321", 1)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/idosos/" + idosoId + "/contatos-emergencia/" + contatoId))
                .andExpect(jsonPath("$.nome").value("João da Silva"));
    }

    @Test
    @DisplayName("POST aceita corpo sem prioridade e sem grau de parentesco")
    void adicionarSoComObrigatorios() throws Exception {
        when(service.adicionar(any(), eq(idosoId), any())).thenReturn(resposta(1));

        mockMvc.perform(post(BASE, idosoId).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"telefone\":\"11987654321\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST com nome curto, telefone inválido ou prioridade 0 responde 400 e não chama o serviço")
    void adicionarInvalido() throws Exception {
        mockMvc.perform(post(BASE, idosoId).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("J", "123", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nome").exists())
                .andExpect(jsonPath("$.errors.telefone").exists())
                .andExpect(jsonPath("$.errors.prioridade").exists());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST sem telefone responde 400")
    void adicionarSemTelefone() throws Exception {
        mockMvc.perform(post(BASE, idosoId).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.telefone").exists());
    }

    @Test
    @DisplayName("POST com telefone acima de 20 caracteres responde 400")
    void telefoneLongo() throws Exception {
        mockMvc.perform(post(BASE, idosoId).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Maria", "+55 (11) 98765-4321 ramal 12345", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.telefone").exists());
    }

    @Test
    @DisplayName("POST acima do limite responde 409")
    void limiteExcedido() throws Exception {
        when(service.adicionar(any(), eq(idosoId), any())).thenThrow(new LimiteContatosEmergenciaException(3));

        mockMvc.perform(post(BASE, idosoId).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Maria", "(11) 98765-4321", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT atualiza e responde 200")
    void atualizar() throws Exception {
        when(service.atualizar(any(), eq(idosoId), eq(contatoId), any())).thenReturn(resposta(2));

        mockMvc.perform(put(BASE + "/{c}", idosoId, contatoId).with(token())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("João da Silva", "(11) 98765-4321", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridade").value(2));
    }

    @Test
    @DisplayName("PUT com contato inexistente responde 404")
    void atualizarInexistente() throws Exception {
        when(service.atualizar(any(), eq(idosoId), eq(contatoId), any()))
                .thenThrow(new ContatoEmergenciaNaoEncontradoException(contatoId));

        mockMvc.perform(put(BASE + "/{c}", idosoId, contatoId).with(token())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("João da Silva", "(11) 98765-4321", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE responde 204")
    void remover() throws Exception {
        mockMvc.perform(delete(BASE + "/{c}", idosoId, contatoId).with(token()))
                .andExpect(status().isNoContent());
        verify(service).remover(any(), eq(idosoId), eq(contatoId));
    }

    @Test
    @DisplayName("Sem permissão responde 403 e idoso inexistente responde 404")
    void erros403e404() throws Exception {
        when(service.listar(any(), eq(idosoId))).thenThrow(new PerfilNaoAutorizadoException("negado"));
        mockMvc.perform(get(BASE, idosoId).with(token())).andExpect(status().isForbidden());

        doThrow(new IdosoNaoEncontradoException(idosoId)).when(service).remover(any(), eq(idosoId), any());
        mockMvc.perform(delete(BASE + "/{c}", idosoId, contatoId).with(token())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Identificador que não é UUID responde 400")
    void uuidInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/idosos/abc/contatos-emergencia").with(token()))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- apoio

    private String corpo(String nome, String telefone, Integer prioridade) throws Exception {
        return objectMapper.writeValueAsString(new ContatoEmergenciaRequestDTO(nome, telefone, "Filho", prioridade));
    }

    private ContatoEmergenciaResponseDTO resposta(int prioridade) {
        return new ContatoEmergenciaResponseDTO(contatoId, idosoId, "João da Silva", "+5511987654321", "Filho",
                prioridade);
    }

    private static JwtRequestPostProcessor token() {
        return jwt().jwt(j -> j.subject("sub-admin").claim("email", "admin@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of("familiar"))));
    }
}
