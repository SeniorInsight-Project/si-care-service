package com.seniorinsight.backendcore.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/** {@code GET /api/v1/familiares/{familiarId}/idosos} de ponta a ponta sobre H2. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FamiliarIdososIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdosoRepository idosoRepository;

    @Autowired
    private FamiliarRepository familiarRepository;

    @Autowired
    private VinculoRedeRepository vinculoRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("o próprio familiar vê todos os seus idosos, por id do familiar ou pelo sub")
    void proprioFamiliar() throws Exception {
        Familiar ana = familiar("sub-ana");
        vincular(ana, idoso("Maria"), PapelAcesso.ADMINISTRADOR);
        vincular(ana, idoso("Bento"), "APOIO");
        vincular(familiar("sub-outro"), idoso("Alheio"), PapelAcesso.ADMINISTRADOR);

        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", ana.getId()).with(token("sub-ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nomeCompleto").value("Bento"))
                .andExpect(jsonPath("$[1].nomeCompleto").value("Maria"));
        UUID sub = UUID.randomUUID();
        Familiar porSub = familiar(sub.toString());
        vincular(porSub, idoso("Zélia"), PapelAcesso.ADMINISTRADOR);
        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", sub).with(token(sub.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Administrador consulta outro familiar e vê só os idosos que ele administra")
    void administradorVeSoOsQueAdministra() throws Exception {
        Familiar admin = familiar("sub-admin");
        Familiar apoio = familiar("sub-apoio");
        Idoso compartilhado = idoso("Maria");
        vincular(admin, compartilhado, PapelAcesso.ADMINISTRADOR);
        vincular(apoio, compartilhado, "APOIO");
        vincular(apoio, idoso("Do Outro Admin"), "APOIO");

        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", apoio.getId()).with(token("sub-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nomeCompleto").value("Maria"));
    }

    @Test
    @DisplayName("Familiar de Apoio e estranho não consultam a lista de outro familiar: 403")
    void apoioEEstranhoRecebem403() throws Exception {
        Familiar admin = familiar("sub-admin");
        Familiar apoio = familiar("sub-apoio");
        familiar("sub-estranho");
        Idoso idoso = idoso("Maria");
        vincular(admin, idoso, PapelAcesso.ADMINISTRADOR);
        vincular(apoio, idoso, "APOIO");

        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", admin.getId()).with(token("sub-apoio")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", admin.getId()).with(token("sub-estranho")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("familiar inexistente responde 404; sem token, 401")
    void inexistenteESemToken() throws Exception {
        familiar("sub-ana");

        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", UUID.randomUUID()).with(token("sub-ana")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private Familiar familiar(String sub) {
        return familiarRepository.saveAndFlush(Familiar.builder().keycloakSub(sub).nomeCompleto("Familiar " + sub)
                .email(sub + "@seniorinsight.com").build());
    }

    private Idoso idoso(String nome) {
        return idosoRepository.saveAndFlush(Idoso.builder().nomeCompleto(nome)
                .dataNascimento(LocalDate.now().minusYears(80)).build());
    }

    private void vincular(Familiar familiar, Idoso idoso, String papel) {
        vinculoRepository.saveAndFlush(VinculoRede.builder().familiar(familiar).idoso(idoso)
                .grauParentesco("FILHO").papelAcesso(papel).build());
    }

    private static JwtRequestPostProcessor token(String sub) {
        return jwt().jwt(j -> j.subject(sub).claim("realm_access", Map.of("roles", List.of("familiar"))));
    }
}
