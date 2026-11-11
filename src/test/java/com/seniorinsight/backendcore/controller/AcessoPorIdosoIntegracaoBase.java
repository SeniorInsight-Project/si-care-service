package com.seniorinsight.backendcore.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.Medico;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/**
 * Base dos testes de ponta a ponta da autorização por idoso (filtros de segurança, controller, serviço, regra de
 * acesso e JPA reais sobre H2). Monta, para um idoso, quem pertence à rede dele e quem não pertence:
 *
 * <ul>
 *   <li>{@link #ADMIN} e {@link #APOIO}: familiares com vínculo ativo;</li>
 *   <li>{@link #MEDICO}: médico que acompanha o idoso;</li>
 *   <li>{@link #ESTRANHO}: familiar cadastrado na plataforma, mas sem vínculo com este idoso;</li>
 *   <li>o aparelho pareado do idoso, com a conta técnica {@code idoso-<id>@<domínio>}.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class AcessoPorIdosoIntegracaoBase {

    static final String DOMINIO = "idoso.seniorinsight.invalid";

    static final String ADMIN = "sub-admin";
    static final String APOIO = "sub-apoio";
    static final String ESTRANHO = "sub-estranho";
    static final String MEDICO = "sub-medico";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected IdosoRepository idosoRepository;

    @Autowired
    protected FamiliarRepository familiarRepository;

    @Autowired
    protected VinculoRedeRepository vinculoRepository;

    @Autowired
    protected MedicoRepository medicoRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    /** Idoso com ADMIN (Administrador), APOIO (vínculo comum) e MEDICO (acompanha). ESTRANHO existe, sem vínculo. */
    protected Idoso idosoComRede() {
        Idoso idoso = novoIdoso("Maria Aparecida", "111.111.111-11");
        vincular(familiar(ADMIN), idoso, PapelAcesso.ADMINISTRADOR);
        vincular(familiar(APOIO), idoso, "FAMILIAR");
        familiar(ESTRANHO);
        Medico medico = Medico.builder().keycloakSub(MEDICO).nomeCompleto("Dr. Teste")
                .crm("CRM-1").especialidade("Geriatria").build();
        medico.getIdosos().add(idoso);
        medicoRepository.saveAndFlush(medico);
        return idoso;
    }

    protected Idoso novoIdoso(String nome, String cpf) {
        return idosoRepository.saveAndFlush(Idoso.builder().nomeCompleto(nome)
                .dataNascimento(LocalDate.now().minusYears(80)).cpf(cpf).build());
    }

    private Familiar familiar(String sub) {
        return familiarRepository.findByKeycloakSub(sub).orElseGet(() -> familiarRepository.saveAndFlush(
                Familiar.builder().keycloakSub(sub).nomeCompleto("Familiar " + sub)
                        .email(sub + "@seniorinsight.com").build()));
    }

    private void vincular(Familiar familiar, Idoso idoso, String papel) {
        vinculoRepository.saveAndFlush(VinculoRede.builder().familiar(familiar).idoso(idoso)
                .grauParentesco("FILHO").papelAcesso(papel).build());
    }

    protected static JwtRequestPostProcessor token(String sub) {
        return jwt().jwt(j -> j.subject(sub).claim("email", sub + "@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of(MEDICO.equals(sub) ? "medico" : "familiar"))));
    }

    /** Token do aparelho pareado: conta técnica do idoso, com a role idoso e e-mail verificado. */
    protected static JwtRequestPostProcessor tokenDoIdoso(UUID idosoId) {
        String conta = "idoso-" + idosoId + "@" + DOMINIO;
        return jwt().jwt(j -> j.subject("sub-idoso-" + idosoId).claim("email", conta)
                .claim("preferred_username", conta).claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of("idoso"))));
    }
}
