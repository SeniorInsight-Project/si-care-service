package com.seniorinsight.backendcore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.domain.AuditoriaIdoso;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.Medico;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.repository.AuditoriaIdosoRepository;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/**
 * Cenários da HU-008 (cadastrar o idoso) e da HU-009 (editar o idoso) de ponta a ponta: filtros de segurança,
 * controller, serviços e JPA reais sobre H2, sem mocks de negócio. Cada teste roda numa transação desfeita no fim.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdosoIntegracaoTest {

    private static final String ADMIN = "sub-admin";
    private static final String APOIO = "sub-apoio";
    private static final String ESTRANHO = "sub-estranho";
    private static final String MEDICO = "sub-medico";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private IdosoRepository idosoRepository;

    @Autowired
    private FamiliarRepository familiarRepository;

    @Autowired
    private VinculoRedeRepository vinculoRepository;

    @Autowired
    private AuditoriaIdosoRepository auditoriaRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    // =========================================================================
    // HU-008: cadastrar o idoso
    // =========================================================================

    @Nested
    @DisplayName("HU-008 · cadastrar o idoso")
    class Cadastro {

        @Test
        @DisplayName("cria o idoso e exatamente 1 vínculo ADMINISTRADOR para o autor (RN-012)")
        void criaIdosoEVinculoAdministrador() throws Exception {
            Familiar autor = familiar(ADMIN);

            JsonNode corpo = cadastrar(ADMIN, requisicao("Maria Aparecida", LocalDate.now().minusYears(80), "111.111.111-11"),
                    201);

            UUID id = UUID.fromString(corpo.get("id").asText());
            assertThat(idosoRepository.findById(id)).isPresent();
            List<VinculoRede> vinculos = vinculoRepository.findByIdosoId(id);
            assertThat(vinculos).hasSize(1);
            assertThat(vinculos.get(0).getFamiliar().getId()).isEqualTo(autor.getId());
            assertThat(vinculos.get(0).getPapelAcesso()).isEqualTo(PapelAcesso.ADMINISTRADOR);
            assertThat(vinculoRepository.countByIdoso_IdAndPapelAcesso(id, PapelAcesso.ADMINISTRADOR)).isEqualTo(1);
            assertThat(corpo.get("comorbidades").isArray()).isTrue();
            assertThat(corpo.get("avisos")).isEmpty();
        }

        @Test
        @DisplayName("idade abaixo de 49 anos cadastra mesmo assim e traz o aviso na resposta")
        void avisoDeIdadeNaoBloqueia() throws Exception {
            familiar(ADMIN);

            JsonNode corpo = cadastrar(ADMIN, requisicao("Cláudia Nova", LocalDate.now().minusYears(48), null), 201);

            assertThat(corpo.get("avisos")).hasSize(1);
            assertThat(corpo.get("avisos").get(0).asText()).contains("49");
            assertThat(idosoRepository.findById(UUID.fromString(corpo.get("id").asText()))).isPresent();
        }

        @Test
        @DisplayName("idade a partir de 49 anos não traz aviso")
        void semAvisoAPartirDe49() throws Exception {
            familiar(ADMIN);

            JsonNode corpo = cadastrar(ADMIN,
                    requisicao("Aníbal Veterano", LocalDate.now().minusYears(49).minusDays(1), null), 201);

            assertThat(corpo.get("avisos")).isEmpty();
        }

        @Test
        @DisplayName("sem nome ou sem data de nascimento responde 400 e não grava nada")
        void camposObrigatorios() throws Exception {
            familiar(ADMIN);
            long antes = idosoRepository.count();

            mockMvc.perform(post("/api/v1/idosos").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"genero\":\"F\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.nomeCompleto").exists())
                    .andExpect(jsonPath("$.errors.dataNascimento").exists());

            assertThat(idosoRepository.count()).isEqualTo(antes);
        }

        @Test
        @DisplayName("data de nascimento no futuro responde 400")
        void dataFutura() throws Exception {
            familiar(ADMIN);

            cadastrar(ADMIN, requisicao("Maria", LocalDate.now().plusDays(1), null), 400);
        }

        @Test
        @DisplayName("CPF já cadastrado responde 409 e não cria outro idoso nem outro vínculo")
        void cpfDuplicado() throws Exception {
            familiar(ADMIN);
            cadastrar(ADMIN, requisicao("Maria", LocalDate.now().minusYears(80), "222.222.222-22"), 201);
            long idosos = idosoRepository.count();
            long vinculos = vinculoRepository.count();

            cadastrar(ADMIN, requisicao("Outra Maria", LocalDate.now().minusYears(75), "222.222.222-22"), 409);

            assertThat(idosoRepository.count()).isEqualTo(idosos);
            assertThat(vinculoRepository.count()).isEqualTo(vinculos);
        }

        @Test
        @DisplayName("quem não tem perfil de Familiar recebe 403 e nada é gravado")
        void semPerfilFamiliar() throws Exception {
            long antes = idosoRepository.count();

            cadastrar(ESTRANHO, requisicao("Maria", LocalDate.now().minusYears(80), null), 403);

            assertThat(idosoRepository.count()).isEqualTo(antes);
        }

        @Test
        @DisplayName("sem token responde 401")
        void semToken() throws Exception {
            mockMvc.perform(post("/api/v1/idosos").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(requisicao("Maria", LocalDate.now().minusYears(80), null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // HU-009: editar o idoso e consultar com vínculo
    // =========================================================================

    @Nested
    @DisplayName("HU-009 · editar o idoso")
    class Edicao {

        @Test
        @DisplayName("Administrador edita; a resposta traz o novo valor e a auditoria guarda autor, horário, antes e depois")
        void administradorEditaEAuditoriaGravaAntesEDepois() throws Exception {
            Idoso idoso = idosoDoAdmin("Maria Antiga", "333.333.333-33");

            mockMvc.perform(put("/api/v1/idosos/{id}", idoso.getId()).with(token(ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(
                                    requisicao("Maria Nova", idoso.getDataNascimento(), "333.333.333-33"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nomeCompleto").value("Maria Nova"));

            assertThat(idosoRepository.findById(idoso.getId()).orElseThrow().getNomeCompleto()).isEqualTo("Maria Nova");
            List<AuditoriaIdoso> auditorias = auditoriaRepository.findByIdosoIdOrderByOcorridoEmAsc(idoso.getId());
            assertThat(auditorias).hasSize(1);
            AuditoriaIdoso a = auditorias.get(0);
            assertThat(a.getAutorSub()).isEqualTo(ADMIN);
            assertThat(a.getOcorridoEm()).isNotNull();
            assertThat(a.getValoresAnteriores()).containsExactly(Map.entry("nomeCompleto", "Maria Antiga"));
            assertThat(a.getValoresNovos()).containsExactly(Map.entry("nomeCompleto", "Maria Nova"));
        }

        @Test
        @DisplayName("a auditoria só leva os campos que mudaram, inclusive os que passaram a vazio")
        void auditoriaSoComCamposAlterados() throws Exception {
            Idoso idoso = idosoDoAdmin("Maria", "444.444.444-44");
            idoso.setCidade("Brasília");
            idosoRepository.saveAndFlush(idoso);

            IdosoPut sem = requisicao("Maria", idoso.getDataNascimento(), "444.444.444-44");
            sem.cidade = null;
            sem.planoSaude = "Unimed";
            mockMvc.perform(put("/api/v1/idosos/{id}", idoso.getId()).with(token(ADMIN))
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(sem)))
                    .andExpect(status().isOk());

            AuditoriaIdoso a = auditoriaRepository.findByIdosoIdOrderByOcorridoEmAsc(idoso.getId()).get(0);
            assertThat(a.getValoresAnteriores()).containsOnlyKeys("cidade", "planoSaude");
            assertThat(a.getValoresAnteriores().get("cidade")).isEqualTo("Brasília");
            assertThat(a.getValoresNovos().get("planoSaude")).isEqualTo("Unimed");
            assertThat(a.getValoresNovos().get("cidade")).isNull();
        }

        @Test
        @DisplayName("PUT sem mudança nenhuma não gera auditoria")
        void semMudancaSemAuditoria() throws Exception {
            Idoso idoso = idosoDoAdmin("Maria", "555.555.555-55");

            mockMvc.perform(put("/api/v1/idosos/{id}", idoso.getId()).with(token(ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(
                                    requisicao("Maria", idoso.getDataNascimento(), "555.555.555-55"))))
                    .andExpect(status().isOk());

            assertThat(auditoriaRepository.findByIdosoIdOrderByOcorridoEmAsc(idoso.getId())).isEmpty();
        }

        @Test
        @DisplayName("Familiar de Apoio não edita: 403, cadastro intacto e sem auditoria")
        void apoioNaoEdita() throws Exception {
            Idoso idoso = idosoDoAdmin("Maria", "666.666.666-66");
            vincular(familiar(APOIO), idoso, "APOIO");

            mockMvc.perform(put("/api/v1/idosos/{id}", idoso.getId()).with(token(APOIO))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(
                                    requisicao("Nome Trocado", idoso.getDataNascimento(), "666.666.666-66"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            assertThat(idosoRepository.findById(idoso.getId()).orElseThrow().getNomeCompleto()).isEqualTo("Maria");
            assertThat(auditoriaRepository.findByIdosoIdOrderByOcorridoEmAsc(idoso.getId())).isEmpty();
        }

        @Test
        @DisplayName("quem está fora da rede não edita: 403; idoso inexistente: 404")
        void foraDaRedeEInexistente() throws Exception {
            Idoso idoso = idosoDoAdmin("Maria", null);
            familiar(ESTRANHO);
            String corpo = json.writeValueAsString(requisicao("X", idoso.getDataNascimento(), null));

            mockMvc.perform(put("/api/v1/idosos/{id}", idoso.getId()).with(token(ESTRANHO))
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/v1/idosos/{id}", UUID.randomUUID()).with(token(ADMIN))
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("editar para o CPF de outro idoso responde 409 e não altera nada")
        void cpfDeOutroIdoso() throws Exception {
            Idoso a = idosoDoAdmin("Maria", "777.777.777-77");
            idosoDoAdmin("João", "888.888.888-88");

            mockMvc.perform(put("/api/v1/idosos/{id}", a.getId()).with(token(ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(requisicao("Maria", a.getDataNascimento(), "888.888.888-88"))))
                    .andExpect(status().isConflict());

            assertThat(idosoRepository.findById(a.getId()).orElseThrow().getCpf()).isEqualTo("777.777.777-77");
            assertThat(auditoriaRepository.findByIdosoIdOrderByOcorridoEmAsc(a.getId())).isEmpty();
        }

        @Test
        @DisplayName("PUT com a data de nascimento mudando para menos de 49 anos devolve o aviso")
        void avisoNaEdicao() throws Exception {
            Idoso idoso = idosoDoAdmin("Maria", null);

            mockMvc.perform(put("/api/v1/idosos/{id}", idoso.getId()).with(token(ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(
                                    requisicao("Maria", LocalDate.now().minusYears(30), null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.avisos.length()").value(1));
        }
    }

    @Nested
    @DisplayName("HU-008/009 · consultar só com vínculo")
    class Consulta {

        @Test
        @DisplayName("GET por id: Administrador e Apoio veem (200); estranho recebe 403; inexistente 404; sem token 401")
        void buscarPorId() throws Exception {
            Idoso idoso = idosoDoAdmin("Maria", null);
            vincular(familiar(APOIO), idoso, "APOIO");
            familiar(ESTRANHO);

            mockMvc.perform(get("/api/v1/idosos/{id}", idoso.getId()).with(token(ADMIN)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nomeCompleto").value("Maria"));
            mockMvc.perform(get("/api/v1/idosos/{id}", idoso.getId()).with(token(APOIO)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/idosos/{id}", idoso.getId()).with(token(ESTRANHO)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/idosos/{id}", UUID.randomUUID()).with(token(ADMIN)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/v1/idosos/{id}", idoso.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET lista traz só os idosos da rede do usuário")
        void listarSoDaRede() throws Exception {
            Idoso meu = idosoDoAdmin("Maria", null);
            Familiar outro = familiar("sub-outro-admin");
            Idoso alheio = idosoCom("João", null);
            vincular(outro, alheio, PapelAcesso.ADMINISTRADOR);

            MvcResult r = mockMvc.perform(get("/api/v1/idosos").with(token(ADMIN)))
                    .andExpect(status().isOk()).andReturn();

            JsonNode lista = json.readTree(r.getResponse().getContentAsString());
            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).get("id").asText()).isEqualTo(meu.getId().toString());
            mockMvc.perform(get("/api/v1/idosos").with(token(ESTRANHO)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("médico que acompanha o idoso consulta (200), mas não edita (403)")
        void medicoConsultaENaoEdita() throws Exception {
            Idoso idoso = idosoDoAdmin("Maria", null);
            Medico medico = Medico.builder().keycloakSub(MEDICO).nomeCompleto("Dra. Ana").crm("CRM-DF-1")
                    .especialidade("Geriatria").build();
            medico.getIdosos().add(idoso);
            medicoRepository.saveAndFlush(medico);

            mockMvc.perform(get("/api/v1/idosos/{id}", idoso.getId()).with(token(MEDICO)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/idosos").with(token(MEDICO)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
            mockMvc.perform(put("/api/v1/idosos/{id}", idoso.getId()).with(token(MEDICO))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(requisicao("X", idoso.getDataNascimento(), null))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // apoio
    // =========================================================================

    /** Corpo mutável só para os testes que precisam ajustar campos soltos. */
    static class IdosoPut {
        public String nomeCompleto;
        public LocalDate dataNascimento;
        public String cpf;
        public String cidade;
        public String planoSaude;
    }

    private static IdosoPut requisicao(String nome, LocalDate nascimento, String cpf) {
        IdosoPut r = new IdosoPut();
        r.nomeCompleto = nome;
        r.dataNascimento = nascimento;
        r.cpf = cpf;
        return r;
    }

    private JsonNode cadastrar(String sub, IdosoPut corpo, int statusEsperado) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/idosos").with(token(sub))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
                .andExpect(status().is(statusEsperado)).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private Familiar familiar(String sub) {
        return familiarRepository.saveAndFlush(Familiar.builder().keycloakSub(sub).nomeCompleto("Familiar " + sub)
                .email(sub + "@seniorinsight.com").build());
    }

    private Idoso idosoCom(String nome, String cpf) {
        return idosoRepository.saveAndFlush(Idoso.builder().nomeCompleto(nome)
                .dataNascimento(LocalDate.now().minusYears(80)).cpf(cpf).build());
    }

    private void vincular(Familiar familiar, Idoso idoso, String papel) {
        vinculoRepository.saveAndFlush(VinculoRede.builder().familiar(familiar).idoso(idoso)
                .grauParentesco("FILHO").papelAcesso(papel).build());
    }

    /** Idoso já cadastrado, com o ADMIN como Familiar Administrador. */
    private Idoso idosoDoAdmin(String nome, String cpf) {
        Familiar admin = familiarRepository.findByKeycloakSub(ADMIN).orElseGet(() -> familiar(ADMIN));
        Idoso idoso = idosoCom(nome, cpf);
        vincular(admin, idoso, PapelAcesso.ADMINISTRADOR);
        return idoso;
    }

    private static JwtRequestPostProcessor token(String sub) {
        return jwt().jwt(j -> j.subject(sub).claim("email", sub + "@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of("familiar"))));
    }
}
