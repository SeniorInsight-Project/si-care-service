package com.seniorinsight.backendcore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import com.seniorinsight.backendcore.domain.Idoso;

/**
 * Comorbidades de ponta a ponta. Leitura: rede, médico e aparelho pareado. Escrita: só rede e médico.
 * Sem vínculo, 403; sem token, 401; idoso inexistente, 404.
 */
class ComorbidadeIntegracaoTest extends AcessoPorIdosoIntegracaoBase {

    private static final String BASE = "/api/v1/idosos/{idosoId}/comorbidades";
    private static final String CORPO = "{\"comorbidades\":[\"Diabetes\"]}";

    @Test
    @DisplayName("Com vínculo (familiar ou médico), adiciona, lista e remove")
    void comVinculo() throws Exception {
        Idoso idoso = idosoComRede();

        mockMvc.perform(post(BASE, idoso.getId()).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comorbidades[0]").value("Diabetes"));
        mockMvc.perform(post(BASE, idoso.getId()).with(token(MEDICO)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comorbidades\":[\"Asma\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete(BASE + "/{n}", idoso.getId(), "Asma").with(token(APOIO)))
                .andExpect(status().isNoContent());

        for (JwtRequestPostProcessor quem : List.of(token(ADMIN), token(APOIO), token(MEDICO),
                tokenDoIdoso(idoso.getId()))) {
            mockMvc.perform(get(BASE, idoso.getId()).with(quem))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comorbidades.length()").value(1))
                    .andExpect(jsonPath("$.comorbidades[0]").value("Diabetes"));
        }
    }

    @Test
    @DisplayName("O aparelho pareado lê as comorbidades, mas não as altera")
    void aparelhoPareadoNaoEscreve() throws Exception {
        Idoso idoso = idosoComRede();

        mockMvc.perform(post(BASE, idoso.getId()).with(tokenDoIdoso(idoso.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/{n}", idoso.getId(), "Diabetes").with(tokenDoIdoso(idoso.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sem vínculo, as três rotas respondem 403 com o Erro do contrato e nada é gravado")
    void semVinculo() throws Exception {
        Idoso idoso = idosoComRede();

        for (JwtRequestPostProcessor quem : List.of(token(ESTRANHO), tokenDoIdoso(UUID.randomUUID()))) {
            mockMvc.perform(post(BASE, idoso.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content(CORPO))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Acesso Proibido"))
                    .andExpect(jsonPath("$.path").value("/api/v1/idosos/" + idoso.getId() + "/comorbidades"));
            mockMvc.perform(get(BASE, idoso.getId()).with(quem))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("Acesso Proibido"));
            mockMvc.perform(delete(BASE + "/{n}", idoso.getId(), "Diabetes").with(quem))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("Acesso Proibido"));
        }

        assertThat(idosoRepository.findById(idoso.getId()).orElseThrow().getComorbidades()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Sem token, as três rotas respondem 401")
    void semToken() throws Exception {
        Idoso idoso = idosoComRede();

        mockMvc.perform(post(BASE, idoso.getId()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE, idoso.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/{n}", idoso.getId(), "Diabetes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Idoso inexistente responde 404")
    void idosoInexistente() throws Exception {
        idosoComRede();
        UUID inexistente = UUID.randomUUID();

        mockMvc.perform(post(BASE, inexistente).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content(CORPO)).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE, inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/{n}", inexistente, "Diabetes").with(token(ADMIN)))
                .andExpect(status().isNotFound());
    }
}
