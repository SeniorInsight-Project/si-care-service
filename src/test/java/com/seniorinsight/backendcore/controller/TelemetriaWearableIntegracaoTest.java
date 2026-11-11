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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.repository.AlarmeEmergenciaRepository;
import com.seniorinsight.backendcore.repository.TelemetriaWearableRepository;

/**
 * Telemetria do wearable de ponta a ponta: o aparelho pareado, a rede e o médico enviam e consultam; quem não tem
 * vínculo recebe 403 e nada é gravado. Cobre as rotas com o idoso na rota e as que o levam no corpo.
 */
class TelemetriaWearableIntegracaoTest extends AcessoPorIdosoIntegracaoBase {

    private static final String BASE = "/api/v1/idosos/{idosoId}/telemetria-wearable";
    private static final String TS = "\"timestamp\":\"2026-11-10T10:00:00Z\"";

    @Autowired
    private TelemetriaWearableRepository telemetriaRepository;

    @Autowired
    private AlarmeEmergenciaRepository alarmeRepository;

    @Test
    @DisplayName("Com vínculo (aparelho pareado, familiar ou médico), todas as rotas funcionam")
    void comVinculo() throws Exception {
        Idoso idoso = idosoComRede();

        for (JwtRequestPostProcessor quem : List.of(tokenDoIdoso(idoso.getId()), token(ADMIN), token(APOIO),
                token(MEDICO))) {
            mockMvc.perform(envio("", "{" + TS + "}", idoso.getId(), quem)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idoso_id").exists());
            mockMvc.perform(envio("/lote", "{\"readings\":[{" + TS + "},{" + TS + "}]}", idoso.getId(), quem))
                    .andExpect(status().isCreated());
            mockMvc.perform(envio("/frequencia-cardiaca", "{\"bpm\":72}", idoso.getId(), quem))
                    .andExpect(status().isCreated());
            mockMvc.perform(envio("/passos", "{\"count\":100}", idoso.getId(), quem))
                    .andExpect(status().isCreated());
            mockMvc.perform(envio("/deteccao-queda", "{\"fall_detected\":false}", idoso.getId(), quem))
                    .andExpect(status().isAccepted());
            mockMvc.perform(get(BASE + "/ultimas", idoso.getId()).with(quem)).andExpect(status().isOk());
            mockMvc.perform(get(BASE + "/recente", idoso.getId()).with(quem)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Com o idoso no corpo (rotas sem {idosoId}), o vínculo também é checado")
    void idosoNoCorpo() throws Exception {
        Idoso idoso = idosoComRede();
        String corpo = "{\"idoso_id\":\"" + idoso.getId() + "\"," + TS + "}";
        long antes = telemetriaRepository.count();

        mockMvc.perform(post("/api/v1/telemetria-wearable/ingestao").with(tokenDoIdoso(idoso.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/telemetria-wearable/ingestao").with(token(ESTRANHO))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
        mockMvc.perform(post("/api/v1/telemetria-wearable/ingestao").with(tokenDoIdoso(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound());

        assertThat(telemetriaRepository.count()).isEqualTo(antes + 1);
    }

    @Test
    @DisplayName("Um aparelho não grava telemetria de outro idoso, nem num lote que mistura idosos")
    void loteMisturandoIdosos() throws Exception {
        Idoso idoso = idosoComRede();
        Idoso outro = novoIdoso("Outro Idoso", "222.222.222-22");
        String lote = "{\"readings\":[{\"idoso_id\":\"" + idoso.getId() + "\"," + TS + "},{\"idoso_id\":\""
                + outro.getId() + "\"," + TS + "}]}";
        long antes = telemetriaRepository.count();

        mockMvc.perform(post("/api/v1/telemetria-wearable/lote").with(tokenDoIdoso(idoso.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(lote))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));

        assertThat(telemetriaRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("Sem vínculo, todas as rotas respondem 404 com o Erro do contrato; nada é gravado e nenhuma queda vira SOS")
    void semVinculo() throws Exception {
        Idoso idoso = idosoComRede();
        long leituras = telemetriaRepository.count();
        long alarmes = alarmeRepository.count();

        for (JwtRequestPostProcessor quem : List.of(token(ESTRANHO), tokenDoIdoso(UUID.randomUUID()))) {
            for (String[] rota : new String[][] {
                    {"", "{" + TS + "}"},
                    {"/lote", "{\"readings\":[{" + TS + "}]}"},
                    {"/frequencia-cardiaca", "{\"bpm\":72}"},
                    {"/passos", "{\"count\":100}"},
                    {"/deteccao-queda", "{\"fall_detected\":true}"}}) {
                mockMvc.perform(envio(rota[0], rota[1], idoso.getId(), quem))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404))
                        .andExpect(jsonPath("$.error").value("Idoso Não Encontrado"))
                        .andExpect(jsonPath("$.path").value("/api/v1/idosos/" + idoso.getId()
                                + "/telemetria-wearable" + rota[0]));
            }
            mockMvc.perform(get(BASE + "/ultimas", idoso.getId()).with(quem))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
            mockMvc.perform(get(BASE + "/recente", idoso.getId()).with(quem))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Idoso Não Encontrado"));
        }

        assertThat(telemetriaRepository.count()).isEqualTo(leituras);
        assertThat(alarmeRepository.count()).isEqualTo(alarmes);
    }

    @Test
    @DisplayName("Sem token, todas as rotas respondem 401")
    void semToken() throws Exception {
        Idoso idoso = idosoComRede();

        mockMvc.perform(post(BASE, idoso.getId()).contentType(MediaType.APPLICATION_JSON).content("{" + TS + "}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/lote", idoso.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"readings\":[{" + TS + "}]}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/frequencia-cardiaca", idoso.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bpm\":72}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/passos", idoso.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"count\":100}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/deteccao-queda", idoso.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fall_detected\":true}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/ultimas", idoso.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/recente", idoso.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Idoso inexistente responde 404")
    void idosoInexistente() throws Exception {
        idosoComRede();
        UUID inexistente = UUID.randomUUID();

        mockMvc.perform(envio("", "{" + TS + "}", inexistente, token(ADMIN))).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/ultimas", inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/recente", inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
    }

    private static MockHttpServletRequestBuilder envio(String sufixo, String corpo, UUID idosoId,
            JwtRequestPostProcessor quem) {
        return post(BASE + sufixo, idosoId).with(quem).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }
}
