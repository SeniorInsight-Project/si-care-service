package com.seniorinsight.backendcore.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.seniorinsight.backendcore.domain.AlarmeEmergencia;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.Medicamento;
import com.seniorinsight.backendcore.domain.Medico;
import com.seniorinsight.backendcore.domain.RotinaMedicamento;
import com.seniorinsight.backendcore.domain.StatusAlarme;
import com.seniorinsight.backendcore.domain.VinculoRede;

/**
 * Consultas usadas pelo {@code AcessoIdosoService} contra o banco (H2 em modo PostgreSQL).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AcessoIdosoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private VinculoRedeRepository vinculoRedeRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private AlarmeEmergenciaRepository alarmeEmergenciaRepository;

    @Autowired
    private RotinaMedicamentoRepository rotinaMedicamentoRepository;

    private Idoso idoso;
    private Idoso outroIdoso;

    @BeforeEach
    void setUp() {
        idoso = em.persist(Idoso.builder().nomeCompleto("Maria").dataNascimento(LocalDate.of(1940, 1, 1)).build());
        outroIdoso = em.persist(Idoso.builder().nomeCompleto("José").dataNascimento(LocalDate.of(1938, 5, 2)).build());
    }

    @Test
    @DisplayName("Vínculo de rede é encontrado pelo sub do familiar e pelo id do idoso")
    void vinculoDoFamiliar() {
        Familiar familiar = em.persist(Familiar.builder()
                .keycloakSub("sub-familiar").nomeCompleto("Ana").cpf("111.111.111-11").build());
        em.persist(VinculoRede.builder()
                .familiar(familiar).idoso(idoso).grauParentesco("Filha").papelAcesso("ADMINISTRADOR").build());
        em.flush();

        assertThat(vinculoRedeRepository.existsByFamiliar_KeycloakSubAndIdoso_Id("sub-familiar", idoso.getId())).isTrue();
        assertThat(vinculoRedeRepository.existsByFamiliar_KeycloakSubAndIdoso_Id("sub-familiar", outroIdoso.getId())).isFalse();
        assertThat(vinculoRedeRepository.existsByFamiliar_KeycloakSubAndIdoso_Id("sub-outro", idoso.getId())).isFalse();
    }

    @Test
    @DisplayName("Médico vinculado é encontrado pelo sub e pelo id do idoso")
    void medicoVinculado() {
        Medico medico = Medico.builder()
                .keycloakSub("sub-medico").nomeCompleto("Dr. Paulo").crm("CRM-SP 1234").especialidade("Geriatria").build();
        medico.getIdosos().add(idoso);
        em.persist(medico);
        em.flush();

        assertThat(medicoRepository.existsByKeycloakSubAndIdosos_Id("sub-medico", idoso.getId())).isTrue();
        assertThat(medicoRepository.existsByKeycloakSubAndIdosos_Id("sub-medico", outroIdoso.getId())).isFalse();
    }

    @Test
    @DisplayName("Alarme e rotina devolvem o id do idoso dono")
    void idosoDonoDoRecurso() {
        AlarmeEmergencia alarme = em.persist(AlarmeEmergencia.builder().idoso(idoso).status(StatusAlarme.DISPARADO).build());
        Medicamento medicamento = em.persist(Medicamento.builder().nome("Losartana").dosagem("50mg").build());
        RotinaMedicamento rotina = em.persist(RotinaMedicamento.builder()
                .idoso(outroIdoso).medicamento(medicamento).horario(LocalTime.of(8, 0))
                .frequencia("DIARIA").dataInicio(LocalDate.now()).build());
        em.flush();

        assertThat(alarmeEmergenciaRepository.findIdosoIdById(alarme.getId())).contains(idoso.getId());
        assertThat(rotinaMedicamentoRepository.findIdosoIdById(rotina.getId())).contains(outroIdoso.getId());
        assertThat(alarmeEmergenciaRepository.findIdosoIdById(UUID.randomUUID())).isEmpty();
    }
}
