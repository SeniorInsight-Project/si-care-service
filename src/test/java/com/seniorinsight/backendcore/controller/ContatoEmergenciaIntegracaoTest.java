package com.seniorinsight.backendcore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.seniorinsight.backendcore.domain.ContatoEmergencia;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.Medico;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.repository.ContatoEmergenciaRepository;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/**
 * HU-011 de ponta a ponta: filtros de segurança, controller, serviço, regras de acesso e JPA reais sobre H2.
 * Escrita só do Administrador; leitura também de quem está na rede, do médico e do idoso pareado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContatoEmergenciaIntegracaoTest {

    private static final String BASE = "/api/v1/idosos/{idosoId}/contatos-emergencia";
    private static final String DOMINIO = "idoso.seniorinsight.invalid";

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
    private MedicoRepository medicoRepository;

    @Autowired
    private ContatoEmergenciaRepository contatoRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    // ---------------------------------------------------------------- escrita

    @Test
    @DisplayName("Administrador adiciona: grava o telefone em E.164 e devolve o contrato")
    void administradorAdiciona() throws Exception {
        Idoso idoso = idosoComRede();

        JsonNode criado = adicionar(ADMIN, idoso, "João da Silva", "(11) 98765-4321", "Filho", 1, 201);

        assertThat(criado.get("id").asText()).isNotBlank();
        assertThat(criado.get("idosoId").asText()).isEqualTo(idoso.getId().toString());
        assertThat(criado.get("telefone").asText()).isEqualTo("+5511987654321");
        assertThat(criado.get("grauParentesco").asText()).isEqualTo("Filho");
        assertThat(criado.get("prioridade").asInt()).isEqualTo(1);
        ContatoEmergencia gravado = contatoRepository.findById(UUID.fromString(criado.get("id").asText())).orElseThrow();
        assertThat(gravado.getTelefone()).isEqualTo("+5511987654321");
    }

    @Test
    @DisplayName("Sem prioridade, o contato entra no fim da fila")
    void prioridadeAutomatica() throws Exception {
        Idoso idoso = idosoComRede();

        JsonNode primeiro = adicionar(ADMIN, idoso, "Ana", "11987654321", null, null, 201);
        JsonNode segundo = adicionar(ADMIN, idoso, "Bia", "11976543210", null, null, 201);
        JsonNode terceiro = adicionar(ADMIN, idoso, "Caio", "11965432109", null, 7, 201);

        assertThat(primeiro.get("prioridade").asInt()).isEqualTo(1);
        assertThat(segundo.get("prioridade").asInt()).isEqualTo(2);
        assertThat(terceiro.get("prioridade").asInt()).isEqualTo(7);
        assertThat(primeiro.get("grauParentesco").isNull()).isTrue();
    }

    @Test
    @DisplayName("O quarto contato responde 409 e nada é gravado")
    void limiteDeTresContatos() throws Exception {
        Idoso idoso = idosoComRede();
        for (int i = 1; i <= 3; i++) {
            adicionar(ADMIN, idoso, "Contato " + i, "1198765432" + i, null, i, 201);
        }

        mockMvc.perform(post(BASE, idoso.getId()).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Contato 4", "11987654329", null, 4)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(contatoRepository.countByIdosoId(idoso.getId())).isEqualTo(3);
    }

    @Test
    @DisplayName("Remover libera vaga para um novo contato")
    void removerLiberaVaga() throws Exception {
        Idoso idoso = idosoComRede();
        JsonNode ultimo = null;
        for (int i = 1; i <= 3; i++) {
            ultimo = adicionar(ADMIN, idoso, "Contato " + i, "1198765432" + i, null, i, 201);
        }

        mockMvc.perform(delete(BASE + "/{c}", idoso.getId(), ultimo.get("id").asText()).with(token(ADMIN)))
                .andExpect(status().isNoContent());
        adicionar(ADMIN, idoso, "Novo", "11987654329", null, null, 201);

        assertThat(contatoRepository.countByIdosoId(idoso.getId())).isEqualTo(3);
    }

    @Test
    @DisplayName("Telefone inválido ou nome curto responde 400 e não grava")
    void validacao() throws Exception {
        Idoso idoso = idosoComRede();

        mockMvc.perform(post(BASE, idoso.getId()).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana", "12345", null, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.telefone").exists());
        mockMvc.perform(post(BASE, idoso.getId()).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("A", "11987654321", null, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nome").exists());

        assertThat(contatoRepository.countByIdosoId(idoso.getId())).isZero();
    }

    @Test
    @DisplayName("PUT atualiza o contato, normaliza o telefone e mantém a prioridade quando ela não vem")
    void atualizar() throws Exception {
        Idoso idoso = idosoComRede();
        JsonNode criado = adicionar(ADMIN, idoso, "Ana", "11987654321", "Filha", 2, 201);

        mockMvc.perform(put(BASE + "/{c}", idoso.getId(), criado.get("id").asText()).with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana Paula", "+55 (21) 99876-5432", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Paula"))
                .andExpect(jsonPath("$.telefone").value("+5521998765432"))
                .andExpect(jsonPath("$.prioridade").value(2))
                .andExpect(jsonPath("$.grauParentesco").doesNotExist());
    }

    @Test
    @DisplayName("PUT e DELETE de contato de outro idoso respondem 404")
    void contatoDeOutroIdoso() throws Exception {
        Idoso meu = idosoComRede();
        Idoso outro = idosoDoAdmin("Outro", "222.222.222-22");
        JsonNode doOutro = adicionar(ADMIN, outro, "Ana", "11987654321", null, 1, 201);
        String contatoId = doOutro.get("id").asText();

        mockMvc.perform(put(BASE + "/{c}", meu.getId(), contatoId).with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Ana", "11987654321", null, 1)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/{c}", meu.getId(), contatoId).with(token(ADMIN)))
                .andExpect(status().isNotFound());

        assertThat(contatoRepository.findById(UUID.fromString(contatoId))).isPresent();
    }

    @Test
    @DisplayName("Contato inexistente responde 404 e idoso inexistente responde 404")
    void naoEncontrado() throws Exception {
        Idoso idoso = idosoComRede();

        mockMvc.perform(delete(BASE + "/{c}", idoso.getId(), UUID.randomUUID()).with(token(ADMIN)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE, UUID.randomUUID()).with(token(ADMIN))).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- controle de acesso

    @Test
    @DisplayName("Quem não é Administrador não escreve: apoio e médico recebem 403; estranho e idoso pareado, 404")
    void soAdministradorEscreve() throws Exception {
        Idoso idoso = idosoComRede();
        JsonNode contato = adicionar(ADMIN, idoso, "Ana", "11987654321", null, 1, 201);
        String contatoId = contato.get("id").asText();

        for (JwtRequestPostProcessor quem : List.of(token(APOIO), token(MEDICO))) {
            mockMvc.perform(post(BASE, idoso.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("Novo", "11987654322", null, 2)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(BASE + "/{c}", idoso.getId(), contatoId).with(quem)
                            .contentType(MediaType.APPLICATION_JSON).content(corpo("Ana", "11987654321", null, 1)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(BASE + "/{c}", idoso.getId(), contatoId).with(quem))
                    .andExpect(status().isForbidden());
        }

        for (JwtRequestPostProcessor quem : List.of(token(ESTRANHO), tokenDoIdoso(idoso.getId()))) {
            mockMvc.perform(post(BASE, idoso.getId()).with(quem).contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("Novo", "11987654322", null, 2)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(put(BASE + "/{c}", idoso.getId(), contatoId).with(quem)
                            .contentType(MediaType.APPLICATION_JSON).content(corpo("Ana", "11987654321", null, 1)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(delete(BASE + "/{c}", idoso.getId(), contatoId).with(quem))
                    .andExpect(status().isNotFound());
        }

        assertThat(contatoRepository.countByIdosoId(idoso.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Leem em ordem de prioridade: Administrador, apoio, médico e idoso pareado")
    void leitura() throws Exception {
        Idoso idoso = idosoComRede();
        adicionar(ADMIN, idoso, "Segundo", "11987654322", null, 2, 201);
        adicionar(ADMIN, idoso, "Primeiro", "11987654321", null, 1, 201);

        for (JwtRequestPostProcessor quem : List.of(token(ADMIN), token(APOIO), token(MEDICO),
                tokenDoIdoso(idoso.getId()))) {
            mockMvc.perform(get(BASE, idoso.getId()).with(quem))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].nome").value("Primeiro"))
                    .andExpect(jsonPath("$[1].nome").value("Segundo"));
        }
    }

    @Test
    @DisplayName("Sem vínculo, 404 (igual a idoso inexistente); conta de idoso de outro idoso, 404; sem token, 401")
    void leituraNegada() throws Exception {
        Idoso idoso = idosoComRede();

        mockMvc.perform(get(BASE, idoso.getId()).with(token(ESTRANHO))).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE, idoso.getId()).with(tokenDoIdoso(UUID.randomUUID())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE, idoso.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token com a role idoso, mas sem e-mail verificado ou de conta diferente, não lê")
    void idosoPareadoExigeContaTecnicaVerificada() throws Exception {
        Idoso idoso = idosoComRede();
        String conta = "idoso-" + idoso.getId() + "@" + DOMINIO;

        JwtRequestPostProcessor naoVerificado = jwt().jwt(j -> j.subject("sub-idoso").claim("email", conta)
                .claim("preferred_username", conta).claim("email_verified", false)
                .claim("realm_access", Map.of("roles", List.of("idoso"))));
        JwtRequestPostProcessor semRole = jwt().jwt(j -> j.subject("sub-idoso").claim("email", conta)
                .claim("preferred_username", conta).claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of("familiar"))));

        mockMvc.perform(get(BASE, idoso.getId()).with(naoVerificado)).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE, idoso.getId()).with(semRole)).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- apoio

    private JsonNode adicionar(String sub, Idoso idoso, String nome, String telefone, String parentesco,
                               Integer prioridade, int statusEsperado) throws Exception {
        MvcResult r = mockMvc.perform(post(BASE, idoso.getId()).with(token(sub))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(nome, telefone, parentesco, prioridade)))
                .andExpect(status().is(statusEsperado)).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private String corpo(String nome, String telefone, String parentesco, Integer prioridade) throws Exception {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("nome", nome);
        m.put("telefone", telefone);
        if (parentesco != null) {
            m.put("grauParentesco", parentesco);
        }
        if (prioridade != null) {
            m.put("prioridade", prioridade);
        }
        return json.writeValueAsString(m);
    }

    /** Idoso com ADMIN (Administrador), APOIO (vínculo comum) e MEDICO (acompanha). ESTRANHO só existe como familiar. */
    private Idoso idosoComRede() {
        Idoso idoso = idosoDoAdmin("Maria Aparecida", "111.111.111-11");
        vincular(familiar(APOIO), idoso, "FAMILIAR");
        familiar(ESTRANHO);
        Medico medico = Medico.builder().keycloakSub(MEDICO).nomeCompleto("Dr. Teste")
                .crm("CRM-1").especialidade("Geriatria").build();
        medico.getIdosos().add(idoso);
        medicoRepository.saveAndFlush(medico);
        return idoso;
    }

    private Idoso idosoDoAdmin(String nome, String cpf) {
        Familiar admin = familiarRepository.findByKeycloakSub(ADMIN).orElseGet(() -> familiar(ADMIN));
        Idoso idoso = idosoRepository.saveAndFlush(Idoso.builder().nomeCompleto(nome)
                .dataNascimento(LocalDate.now().minusYears(80)).cpf(cpf).build());
        vincular(admin, idoso, PapelAcesso.ADMINISTRADOR);
        return idoso;
    }

    private Familiar familiar(String sub) {
        return familiarRepository.saveAndFlush(Familiar.builder().keycloakSub(sub).nomeCompleto("Familiar " + sub)
                .email(sub + "@seniorinsight.com").build());
    }

    private void vincular(Familiar familiar, Idoso idoso, String papel) {
        vinculoRepository.saveAndFlush(VinculoRede.builder().familiar(familiar).idoso(idoso)
                .grauParentesco("FILHO").papelAcesso(papel).build());
    }

    private static JwtRequestPostProcessor token(String sub) {
        return jwt().jwt(j -> j.subject(sub).claim("email", sub + "@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of(MEDICO.equals(sub) ? "medico" : "familiar"))));
    }

    /** Token do aparelho pareado: conta técnica do idoso, com a role idoso e e-mail verificado. */
    private static JwtRequestPostProcessor tokenDoIdoso(UUID idosoId) {
        String conta = "idoso-" + idosoId + "@" + DOMINIO;
        return jwt().jwt(j -> j.subject("sub-idoso-" + idosoId).claim("email", conta)
                .claim("preferred_username", conta).claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of("idoso"))));
    }
}
