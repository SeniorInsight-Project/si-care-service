package com.seniorinsight.backendcore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
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
import com.seniorinsight.backendcore.dto.IdosoRequestDTO;
import com.seniorinsight.backendcore.dto.IdosoResponseDTO;
import com.seniorinsight.backendcore.exception.CpfJaCadastradoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.service.IdosoService;

@WebMvcTest(controllers = IdosoController.class)
@Import(SecurityConfig.class)
class IdosoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdosoService idosoService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private final UUID idosoId = UUID.randomUUID();

    // ---------------------------------------------------------------- 401

    @Test
    @DisplayName("Todas as rotas respondem 401 sem token")
    void semTokenRetorna401() throws Exception {
        String corpo = objectMapper.writeValueAsString(requisicao("Maria", LocalDate.of(1945, 3, 12)));

        mockMvc.perform(get("/api/v1/idosos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/idosos/{id}", idosoId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/idosos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/idosos/{id}", idosoId).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- POST

    @Test
    @DisplayName("POST cadastra o idoso e responde 201 com Location e corpo do contrato")
    void cadastrarRetorna201() throws Exception {
        when(idosoService.cadastrar(any(), any())).thenReturn(resposta(List.of()));

        mockMvc.perform(post("/api/v1/idosos").with(token("sub-familiar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requisicao("Maria Aparecida", LocalDate.of(1945, 3, 12)))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/idosos/" + idosoId))
                .andExpect(jsonPath("$.id").value(idosoId.toString()))
                .andExpect(jsonPath("$.nomeCompleto").value("Maria Aparecida"))
                .andExpect(jsonPath("$.comorbidades").isArray())
                .andExpect(jsonPath("$.avisos").isEmpty());
    }

    @Test
    @DisplayName("POST devolve o aviso de idade na resposta 201")
    void cadastrarTrazAvisoDeIdade() throws Exception {
        when(idosoService.cadastrar(any(), any())).thenReturn(resposta(List.of("O idoso tem menos de 49 anos.")));

        mockMvc.perform(post("/api/v1/idosos").with(token("sub-familiar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requisicao("Jovem", LocalDate.now().minusYears(30)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisos[0]").value("O idoso tem menos de 49 anos."));
    }

    @Test
    @DisplayName("POST sem nome e com data futura responde 400 no formato Erro, com a mensagem por campo")
    void cadastrarInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/v1/idosos").with(token("sub-familiar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requisicao(" ", LocalDate.now().plusDays(1)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/idosos"))
                .andExpect(jsonPath("$.errors.nomeCompleto").exists())
                .andExpect(jsonPath("$.errors.dataNascimento").exists());
    }

    @Test
    @DisplayName("POST sem data de nascimento e com JSON malformado responde 400")
    void cadastrarSemDataEJsonRuimRetorna400() throws Exception {
        mockMvc.perform(post("/api/v1/idosos").with(token("sub-familiar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"Maria\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dataNascimento").exists());

        mockMvc.perform(post("/api/v1/idosos").with(token("sub-familiar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST com CPF de outro idoso responde 409")
    void cadastrarCpfDuplicadoRetorna409() throws Exception {
        when(idosoService.cadastrar(any(), any())).thenThrow(new CpfJaCadastradoException());

        mockMvc.perform(post("/api/v1/idosos").with(token("sub-familiar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requisicao("Maria", LocalDate.of(1945, 3, 12)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST de quem não é Familiar responde 403")
    void cadastrarSemPerfilFamiliarRetorna403() throws Exception {
        when(idosoService.cadastrar(any(), any())).thenThrow(new PerfilNaoAutorizadoException("Apenas um Familiar"));

        mockMvc.perform(post("/api/v1/idosos").with(token("sub-medico"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requisicao("Maria", LocalDate.of(1945, 3, 12)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ---------------------------------------------------------------- GET

    @Test
    @DisplayName("GET lista responde 200 com a lista de idosos da rede")
    void listarRetorna200() throws Exception {
        when(idosoService.listar(any())).thenReturn(List.of(resposta(List.of())));

        mockMvc.perform(get("/api/v1/idosos").with(token("sub-familiar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(idosoId.toString()));
    }

    @Test
    @DisplayName("GET por id responde 200 com vínculo, 403 sem vínculo e 404 para idoso inexistente")
    void buscarRetorna200_403_404() throws Exception {
        UUID semVinculo = UUID.randomUUID();
        UUID inexistente = UUID.randomUUID();
        when(idosoService.buscar(any(), eq(idosoId))).thenReturn(resposta(List.of()));
        when(idosoService.buscar(any(), eq(semVinculo)))
                .thenThrow(new PerfilNaoAutorizadoException("Você não faz parte da rede"));
        when(idosoService.buscar(any(), eq(inexistente))).thenThrow(new IdosoNaoEncontradoException(inexistente));

        mockMvc.perform(get("/api/v1/idosos/{id}", idosoId).with(token("sub-familiar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idosoId.toString()));
        mockMvc.perform(get("/api/v1/idosos/{id}", semVinculo).with(token("sub-estranho")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/idosos/{id}", inexistente).with(token("sub-familiar")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET com id que não é UUID responde 400")
    void buscarComIdInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/v1/idosos/{id}", "abc").with(token("sub-familiar")))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- PUT

    @Test
    @DisplayName("PUT do Administrador responde 200 com o cadastro atualizado")
    void atualizarRetorna200() throws Exception {
        when(idosoService.atualizar(any(), eq(idosoId), any())).thenReturn(resposta(List.of()));

        mockMvc.perform(put("/api/v1/idosos/{id}", idosoId).with(token("sub-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requisicao("Maria Aparecida", LocalDate.of(1945, 3, 12)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idosoId.toString()));
    }

    @Test
    @DisplayName("PUT de Familiar de Apoio responde 403")
    void atualizarComoApoioRetorna403() throws Exception {
        when(idosoService.atualizar(any(), eq(idosoId), any()))
                .thenThrow(new PerfilNaoAutorizadoException("Apenas o Familiar Administrador"));

        mockMvc.perform(put("/api/v1/idosos/{id}", idosoId).with(token("sub-apoio"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requisicao("Maria", LocalDate.of(1945, 3, 12)))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT de idoso inexistente responde 404; com CPF de outro idoso, 409; com corpo inválido, 400")
    void atualizarRetorna404_409_400() throws Exception {
        UUID inexistente = UUID.randomUUID();
        when(idosoService.atualizar(any(), eq(inexistente), any())).thenThrow(new IdosoNaoEncontradoException(inexistente));
        when(idosoService.atualizar(any(), eq(idosoId), any())).thenThrow(new CpfJaCadastradoException());
        String valido = objectMapper.writeValueAsString(requisicao("Maria", LocalDate.of(1945, 3, 12)));

        mockMvc.perform(put("/api/v1/idosos/{id}", inexistente).with(token("sub-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(valido))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/idosos/{id}", idosoId).with(token("sub-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(valido))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/idosos/{id}", idosoId).with(token("sub-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- apoio

    private static JwtRequestPostProcessor token(String sub) {
        return jwt().jwt(j -> j.subject(sub).claim("email", sub + "@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of("familiar"))));
    }

    private static IdosoRequestDTO requisicao(String nome, LocalDate nascimento) {
        return new IdosoRequestDTO(nome, nascimento, "F",
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private IdosoResponseDTO resposta(List<String> avisos) {
        return new IdosoResponseDTO(idosoId, "Maria Aparecida", LocalDate.of(1945, 3, 12), "F",
                null, null, null, null, null, null, null, null, null, null, null, null, List.of(), Map.of(),
                Instant.parse("2026-11-10T10:00:00Z"), Instant.parse("2026-11-10T10:00:00Z"), avisos);
    }
}
