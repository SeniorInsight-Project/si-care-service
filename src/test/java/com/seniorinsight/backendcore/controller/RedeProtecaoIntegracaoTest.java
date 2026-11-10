package com.seniorinsight.backendcore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.repository.ConviteRedeRepository;

/**
 * Rede de proteção e convites de ponta a ponta. Convidar: só rede e médico. Ver a rede: também o aparelho pareado.
 * Sem vínculo, 403; sem token, 401; idoso inexistente, 404.
 */
class RedeProtecaoIntegracaoTest extends AcessoPorIdosoIntegracaoBase {

    private static final String CONVITES = "/api/v1/idosos/{idosoId}/convites";
    private static final String REDE = "/api/v1/idosos/{idosoId}/rede";
    private static final String CONVITE =
            "{\"email\":\"neta@seniorinsight.com\",\"papelAcesso\":\"FAMILIAR\",\"grauParentesco\":\"Neta\"}";

    @Autowired
    private ConviteRedeRepository conviteRepository;

    @Test
    @DisplayName("Com vínculo (familiar ou médico), gera convite e lista a rede; o aparelho pareado só lista")
    void comVinculo() throws Exception {
        Idoso idoso = idosoComRede();

        for (JwtRequestPostProcessor quem : List.of(token(ADMIN), token(APOIO), token(MEDICO))) {
            mockMvc.perform(post(CONVITES, idoso.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content(CONVITE))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idosoId").value(idoso.getId().toString()))
                    .andExpect(jsonPath("$.status").value("PENDENTE"));
        }

        for (JwtRequestPostProcessor quem : List.of(token(ADMIN), token(APOIO), token(MEDICO),
                tokenDoIdoso(idoso.getId()))) {
            mockMvc.perform(get(REDE, idoso.getId()).with(quem))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        mockMvc.perform(post(CONVITES, idoso.getId()).with(tokenDoIdoso(idoso.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(CONVITE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sem vínculo (404), gerar convite e listar a rede respondem 404 com o Erro do contrato e nenhum convite é gravado")
    void semVinculo() throws Exception {
        Idoso idoso = idosoComRede();
        long convites = conviteRepository.count();

        for (JwtRequestPostProcessor quem : List.of(token(ESTRANHO), tokenDoIdoso(UUID.randomUUID()))) {
            mockMvc.perform(post(CONVITES, idoso.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content(CONVITE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"))
                    .andExpect(jsonPath("$.path").value("/api/v1/idosos/" + idoso.getId() + "/convites"));
            mockMvc.perform(get(REDE, idoso.getId()).with(quem))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
        }

        assertThat(conviteRepository.count()).isEqualTo(convites);
    }

    @Test
    @DisplayName("Sem token, as rotas respondem 401")
    void semToken() throws Exception {
        Idoso idoso = idosoComRede();

        mockMvc.perform(post(CONVITES, idoso.getId()).contentType(MediaType.APPLICATION_JSON).content(CONVITE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(REDE, idoso.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/convites/aceitar").param("token", "qualquer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Idoso inexistente responde 404")
    void idosoInexistente() throws Exception {
        idosoComRede();
        UUID inexistente = UUID.randomUUID();

        mockMvc.perform(post(CONVITES, inexistente).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content(CONVITE)).andExpect(status().isNotFound());
        mockMvc.perform(get(REDE, inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Convite com papel Administrador responde 400 e nenhum convite é gravado (RN-012)")
    void conviteNaoConcedeAdministrador() throws Exception {
        Idoso idoso = idosoComRede();
        long convites = conviteRepository.count();

        mockMvc.perform(post(CONVITES, idoso.getId()).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"neta@seniorinsight.com\",\"papelAcesso\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isBadRequest());

        assertThat(conviteRepository.count()).isEqualTo(convites);
    }
}
