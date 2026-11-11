package com.seniorinsight.backendcore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import com.seniorinsight.backendcore.domain.AlarmeEmergencia;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.StatusAlarme;
import com.seniorinsight.backendcore.repository.AlarmeEmergenciaRepository;

/**
 * Rotas de SOS (RF-007) de ponta a ponta: com vínculo (rede, médico e aparelho pareado), sem vínculo (403), sem token
 * (401) e recurso inexistente (404), com a regra do {@code AcessoIdosoService} e o banco reais.
 */
class AlarmeEmergenciaIntegracaoTest extends AcessoPorIdosoIntegracaoBase {

    private static final String SOS_DO_IDOSO = "/api/v1/idosos/{idosoId}/sos";
    private static final String SOS = "/api/v1/sos/{alarmeId}";
    private static final String STATUS = "/api/v1/sos/{alarmeId}/status";
    private static final String CORPO_DISPARO = "{\"latitude\":-23.55,\"longitude\":-46.63,\"nivelBateria\":80}";
    private static final String CORPO_STATUS = "{\"status\":\"EM_ATENDIMENTO\",\"observacao\":\"A caminho\"}";

    @Autowired
    private AlarmeEmergenciaRepository alarmeRepository;

    @Test
    @DisplayName("Com vínculo (familiar, médico ou aparelho pareado), as quatro rotas funcionam")
    void comVinculo() throws Exception {
        Idoso idoso = idosoComRede();

        for (JwtRequestPostProcessor quem : List.of(token(ADMIN), token(APOIO), token(MEDICO),
                tokenDoIdoso(idoso.getId()))) {
            AlarmeEmergencia alarme = alarme(idoso);

            mockMvc.perform(post(SOS_DO_IDOSO, idoso.getId()).with(quem)
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_DISPARO))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.idosoId").value(idoso.getId().toString()))
                    .andExpect(jsonPath("$.status").value("DISPARADO"));
            mockMvc.perform(get(SOS, alarme.getId()).with(quem))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(alarme.getId().toString()));
            mockMvc.perform(put(STATUS, alarme.getId()).with(quem)
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_STATUS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
            mockMvc.perform(get(SOS_DO_IDOSO, idoso.getId()).with(quem))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }
    }

    @Test
    @DisplayName("Sem vínculo (404, igual a inexistente), as quatro rotas respondem 404 com o Erro do contrato e nada é gravado ou alterado")
    void semVinculo() throws Exception {
        Idoso idoso = idosoComRede();
        AlarmeEmergencia alarme = alarme(idoso);
        long antes = alarmeRepository.count();

        for (JwtRequestPostProcessor quem : List.of(token(ESTRANHO), tokenDoIdoso(UUID.randomUUID()))) {
            mockMvc.perform(post(SOS_DO_IDOSO, idoso.getId()).with(quem)
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_DISPARO))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"))
                    .andExpect(jsonPath("$.path").value("/api/v1/idosos/" + idoso.getId() + "/sos"));
            mockMvc.perform(get(SOS_DO_IDOSO, idoso.getId()).with(quem))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
            mockMvc.perform(get(SOS, alarme.getId()).with(quem))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Alarme Não Encontrado"));
            mockMvc.perform(put(STATUS, alarme.getId()).with(quem)
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_STATUS))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Alarme Não Encontrado"));
        }

        assertThat(alarmeRepository.count()).isEqualTo(antes);
        assertThat(alarmeRepository.findById(alarme.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusAlarme.DISPARADO);
    }

    @Test
    @DisplayName("Sem token, as quatro rotas respondem 401")
    void semToken() throws Exception {
        Idoso idoso = idosoComRede();
        AlarmeEmergencia alarme = alarme(idoso);

        mockMvc.perform(post(SOS_DO_IDOSO, idoso.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPO_DISPARO)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(SOS_DO_IDOSO, idoso.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(SOS, alarme.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS, alarme.getId()).contentType(MediaType.APPLICATION_JSON).content(CORPO_STATUS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Idoso ou alarme inexistente responde 404")
    void inexistente() throws Exception {
        idosoComRede();
        UUID inexistente = UUID.randomUUID();

        mockMvc.perform(post(SOS_DO_IDOSO, inexistente).with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_DISPARO))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(SOS_DO_IDOSO, inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
        mockMvc.perform(get(SOS, inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
        mockMvc.perform(put(STATUS, inexistente).with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_STATUS))
                .andExpect(status().isNotFound());
    }

    private AlarmeEmergencia alarme(Idoso idoso) {
        return alarmeRepository.saveAndFlush(AlarmeEmergencia.builder().idoso(idoso)
                .status(StatusAlarme.DISPARADO).observacao("Botão acionado").build());
    }
}
