package com.seniorinsight.backendcore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.Medicamento;
import com.seniorinsight.backendcore.domain.RotinaMedicamento;
import com.seniorinsight.backendcore.repository.HistoricoMedicacaoRepository;
import com.seniorinsight.backendcore.repository.MedicamentoRepository;
import com.seniorinsight.backendcore.repository.RotinaMedicamentoRepository;

/**
 * Rotinas de medicamento de ponta a ponta. Criar, editar e remover: rede e médico. Listar, consultar, ver o histórico
 * e confirmar a ingestão: também o aparelho pareado do idoso. Nas rotas por rotina, o vínculo é com o idoso dono dela.
 */
class RotinaMedicamentoIntegracaoTest extends AcessoPorIdosoIntegracaoBase {

    private static final String DO_IDOSO = "/api/v1/idosos/{idosoId}/rotinas-medicamentos";
    private static final String ROTINA = "/api/v1/rotinas-medicamentos/{rotinaId}";
    private static final String CRIAR =
            "{\"nomeMedicamento\":\"Aspirina\",\"dosagem\":\"100mg\",\"horario\":\"08:00\",\"frequencia\":\"1x ao dia\"}";

    @Autowired
    private RotinaMedicamentoRepository rotinaRepository;

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private HistoricoMedicacaoRepository historicoRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("Com vínculo (familiar ou médico), as sete rotas funcionam")
    void comVinculo() throws Exception {
        Idoso idoso = idosoComRede();

        // Cada usuário remove a própria rotina; criar todas antes evita reaproveitar a coleção do idoso após um delete
        List<JwtRequestPostProcessor> quemEntra = List.of(token(ADMIN), token(APOIO), token(MEDICO));
        List<RotinaMedicamento> rotinas = List.of(rotina(idoso), rotina(idoso), rotina(idoso));
        em.flush();
        em.clear();

        for (int i = 0; i < quemEntra.size(); i++) {
            JwtRequestPostProcessor quem = quemEntra.get(i);
            RotinaMedicamento rotina = rotinas.get(i);

            mockMvc.perform(post(DO_IDOSO, idoso.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content(CRIAR))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idosoId").value(idoso.getId().toString()));
            mockMvc.perform(get(DO_IDOSO, idoso.getId()).with(quem)).andExpect(status().isOk());
            mockMvc.perform(get(ROTINA, rotina.getId()).with(quem))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(rotina.getId().toString()));
            mockMvc.perform(get(ROTINA + "/historico", rotina.getId()).with(quem)).andExpect(status().isOk());
            mockMvc.perform(post(ROTINA + "/confirmar-ingestao", rotina.getId()).with(quem)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isCreated());
            mockMvc.perform(put(ROTINA, rotina.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"observacoes\":\"Após o café\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.observacoes").value("Após o café"));
            mockMvc.perform(delete(ROTINA, rotina.getId()).with(quem)).andExpect(status().isNoContent());
            // tudo roda na mesma transação do teste: sincroniza o delete para o idoso não referenciar a rotina removida
            em.flush();
            em.clear();
        }
    }

    @Test
    @DisplayName("O aparelho pareado lê as rotinas e confirma a ingestão, mas não cria, edita nem remove")
    void aparelhoPareado() throws Exception {
        Idoso idoso = idosoComRede();
        RotinaMedicamento rotina = rotina(idoso);
        JwtRequestPostProcessor aparelho = tokenDoIdoso(idoso.getId());

        mockMvc.perform(get(DO_IDOSO, idoso.getId()).with(aparelho)).andExpect(status().isOk());
        mockMvc.perform(get(ROTINA, rotina.getId()).with(aparelho)).andExpect(status().isOk());
        mockMvc.perform(get(ROTINA + "/historico", rotina.getId()).with(aparelho)).andExpect(status().isOk());
        mockMvc.perform(post(ROTINA + "/confirmar-ingestao", rotina.getId()).with(aparelho)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post(DO_IDOSO, idoso.getId()).with(aparelho).contentType(MediaType.APPLICATION_JSON)
                .content(CRIAR)).andExpect(status().isForbidden());
        mockMvc.perform(put(ROTINA, rotina.getId()).with(aparelho).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(delete(ROTINA, rotina.getId()).with(aparelho)).andExpect(status().isForbidden());
        assertThat(rotinaRepository.findById(rotina.getId())).isPresent();
    }

    @Test
    @DisplayName("Sem vínculo, as sete rotas respondem 403 com o Erro do contrato e nada é gravado, alterado ou removido")
    void semVinculo() throws Exception {
        Idoso idoso = idosoComRede();
        RotinaMedicamento rotina = rotina(idoso);
        long rotinas = rotinaRepository.count();
        long historicos = historicoRepository.count();

        for (JwtRequestPostProcessor quem : List.of(token(ESTRANHO), tokenDoIdoso(UUID.randomUUID()))) {
            mockMvc.perform(post(DO_IDOSO, idoso.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content(CRIAR))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Acesso Proibido"))
                    .andExpect(jsonPath("$.path").value("/api/v1/idosos/" + idoso.getId() + "/rotinas-medicamentos"));
            mockMvc.perform(get(DO_IDOSO, idoso.getId()).with(quem)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Acesso Proibido"));
            mockMvc.perform(get(ROTINA, rotina.getId()).with(quem)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Acesso Proibido"));
            mockMvc.perform(get(ROTINA + "/historico", rotina.getId()).with(quem)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Acesso Proibido"));
            mockMvc.perform(post(ROTINA + "/confirmar-ingestao", rotina.getId()).with(quem)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("Acesso Proibido"));
            mockMvc.perform(put(ROTINA, rotina.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"observacoes\":\"invadida\"}"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("Acesso Proibido"));
            mockMvc.perform(delete(ROTINA, rotina.getId()).with(quem)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Acesso Proibido"));
        }

        assertThat(rotinaRepository.count()).isEqualTo(rotinas);
        assertThat(historicoRepository.count()).isEqualTo(historicos);
        assertThat(rotinaRepository.findById(rotina.getId()).orElseThrow().getObservacoes()).isEqualTo("Controle");
    }

    @Test
    @DisplayName("Rotina de outro idoso: quem é da rede de um idoso não acessa a rotina de outro")
    void rotinaDeOutroIdoso() throws Exception {
        Idoso idoso = idosoComRede();
        Idoso outro = novoIdoso("Outro Idoso", "222.222.222-22");
        RotinaMedicamento rotinaDoOutro = rotina(outro);

        mockMvc.perform(get(ROTINA, rotinaDoOutro.getId()).with(token(ADMIN))).andExpect(status().isForbidden());
        mockMvc.perform(delete(ROTINA, rotinaDoOutro.getId()).with(token(ADMIN))).andExpect(status().isForbidden());
        mockMvc.perform(get(DO_IDOSO, outro.getId()).with(token(ADMIN))).andExpect(status().isForbidden());
        assertThat(rotinaRepository.findById(rotinaDoOutro.getId())).isPresent();
        assertThat(idoso.getId()).isNotEqualTo(outro.getId());
    }

    @Test
    @DisplayName("Sem token, as sete rotas respondem 401")
    void semToken() throws Exception {
        Idoso idoso = idosoComRede();
        RotinaMedicamento rotina = rotina(idoso);

        mockMvc.perform(post(DO_IDOSO, idoso.getId()).contentType(MediaType.APPLICATION_JSON).content(CRIAR))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(DO_IDOSO, idoso.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ROTINA, rotina.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ROTINA + "/historico", rotina.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ROTINA + "/confirmar-ingestao", rotina.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(put(ROTINA, rotina.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ROTINA, rotina.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Idoso ou rotina inexistente responde 404")
    void inexistente() throws Exception {
        idosoComRede();
        UUID inexistente = UUID.randomUUID();

        mockMvc.perform(post(DO_IDOSO, inexistente).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content(CRIAR)).andExpect(status().isNotFound());
        mockMvc.perform(get(DO_IDOSO, inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
        mockMvc.perform(get(ROTINA, inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
        mockMvc.perform(get(ROTINA + "/historico", inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
        mockMvc.perform(post(ROTINA + "/confirmar-ingestao", inexistente).with(token(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
        mockMvc.perform(put(ROTINA, inexistente).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isNotFound());
        mockMvc.perform(delete(ROTINA, inexistente).with(token(ADMIN))).andExpect(status().isNotFound());
    }

    private RotinaMedicamento rotina(Idoso idoso) {
        Medicamento medicamento = medicamentoRepository.saveAndFlush(Medicamento.builder()
                .nome("Losartana " + UUID.randomUUID()).dosagem("50mg").build());
        return rotinaRepository.saveAndFlush(RotinaMedicamento.builder().idoso(idoso).medicamento(medicamento)
                .horario(LocalTime.of(8, 0)).frequencia("1x ao dia").diasSemana("TODOS")
                .dataInicio(LocalDate.now().minusDays(1)).ativo(true).observacoes("Controle").build());
    }
}
