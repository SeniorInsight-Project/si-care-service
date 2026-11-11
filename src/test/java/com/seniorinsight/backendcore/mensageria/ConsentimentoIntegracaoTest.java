package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class ConsentimentoIntegracaoTest extends IntegracaoBase {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entidades;

    @Test
    void revogaPassosEmHistoricoEOutboxSemRepetirAoReenviar() throws Exception {
        UUID idosoId = UUID.randomUUID();
        jdbc.update("INSERT INTO idosos (id, nome_completo, data_nascimento) VALUES (?, ?, DATE '1950-01-01')",
                idosoId, "Pessoa de teste");
        String rota = "/api/v1/idosos/" + idosoId + "/consentimentos";
        RequestPostProcessor titular = titular(idosoId);
        mvc.perform(get(rota).with(titular)).andExpect(status().isOk())
                .andExpect(jsonPath("$.LOCALIZACAO").value(false));
        mvc.perform(put(rota).with(titular).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        salvar(rota, titular, true);
        salvar(rota, titular, false);
        salvar(rota, titular, false);
        entidades.flush();

        assertThat(jdbc.queryForList("""
                SELECT autorizado FROM consentimentos
                WHERE idoso_id = ? AND tipo = 'PASSOS' AND versao_termo = 'v1' ORDER BY id
                """, Boolean.class, idosoId)).containsExactly(true, false);
        assertThat(jdbc.queryForList("""
                SELECT (payload->'data'->>'autorizado')::boolean FROM outbox_evento
                WHERE agregado_id = ? AND tipo_evento = 'consent.changed' AND payload->'data'->>'tipo' = 'PASSOS'
                ORDER BY criado_em
                """, Boolean.class, idosoId)).containsExactly(true, false);
    }

    private void salvar(String rota, RequestPostProcessor titular, boolean passos) throws Exception {
        mvc.perform(put(rota).with(titular).contentType(MediaType.APPLICATION_JSON).content("""
                {"BATIMENTOS":true,"PASSOS":%b,"SONO":false,"OXIGENACAO":false,"LOCALIZACAO":false}
                """.formatted(passos))).andExpect(status().isOk()).andExpect(jsonPath("$.PASSOS").value(passos));
    }

    private static RequestPostProcessor titular(UUID idosoId) {
        String conta = "idoso-" + idosoId + "@idoso.seniorinsight.invalid";
        return jwt().jwt(token -> token.subject("titular").claim("preferred_username", conta)
                .claim("email", conta).claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of("idoso"))));
    }
}
