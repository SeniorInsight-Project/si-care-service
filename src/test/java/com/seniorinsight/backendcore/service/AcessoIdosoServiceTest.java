package com.seniorinsight.backendcore.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.seniorinsight.backendcore.exception.AlarmeNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.exception.RotinaMedicamentoNaoEncontradaException;
import com.seniorinsight.backendcore.repository.AlarmeEmergenciaRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.RotinaMedicamentoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;

@ExtendWith(MockitoExtension.class)
class AcessoIdosoServiceTest {

    private static final String DOMINIO = "idoso.seniorinsight.invalid";

    @Mock
    private VinculoRedeRepository vinculoRedeRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private AlarmeEmergenciaRepository alarmeEmergenciaRepository;

    @Mock
    private RotinaMedicamentoRepository rotinaMedicamentoRepository;

    private AcessoIdosoService acesso;
    private UUID idosoId;

    @BeforeEach
    void setUp() {
        acesso = new AcessoIdosoService(vinculoRedeRepository, medicoRepository,
                alarmeEmergenciaRepository, rotinaMedicamentoRepository, DOMINIO);
        idosoId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Familiar com vínculo na rede do idoso tem acesso")
    void familiarComVinculo() {
        when(vinculoRedeRepository.existsByFamiliar_KeycloakSubAndIdoso_Id("sub-familiar", idosoId)).thenReturn(true);

        assertThatCode(() -> acesso.exigirVinculo(familiar(), idosoId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Médico vinculado ao idoso tem acesso")
    void medicoVinculado() {
        UsuarioAutenticado medico = new UsuarioAutenticado("sub-medico", "medico@x.com", "Dr", List.of("medico"));
        when(medicoRepository.existsByKeycloakSubAndIdosos_Id("sub-medico", idosoId)).thenReturn(true);

        assertThatCode(() -> acesso.exigirVinculo(medico, idosoId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Próprio idoso, pelo token do aparelho pareado, tem acesso sem consultar a rede")
    void proprioIdoso() {
        UsuarioAutenticado idoso = new UsuarioAutenticado(
                "sub-idoso", "idoso-" + idosoId + "@" + DOMINIO, "Maria", List.of("idoso", "offline_access"));

        assertThatCode(() -> acesso.exigirVinculo(idoso, idosoId)).doesNotThrowAnyException();
        verify(vinculoRedeRepository, never()).existsByFamiliar_KeycloakSubAndIdoso_Id(any(), any());
    }

    @Test
    @DisplayName("Idoso pareado não acessa outro idoso")
    void idosoNaoAcessaOutroIdoso() {
        UsuarioAutenticado outroIdoso = new UsuarioAutenticado(
                "sub-idoso", "idoso-" + UUID.randomUUID() + "@" + DOMINIO, "João", List.of("idoso"));

        assertThatThrownBy(() -> acesso.exigirVinculo(outroIdoso, idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("E-mail técnico do idoso sem a role idoso não dá acesso")
    void emailDeIdosoSemRoleIdoso() {
        UsuarioAutenticado impostor = new UsuarioAutenticado(
                "sub-x", "idoso-" + idosoId + "@" + DOMINIO, "X", List.of("familiar"));

        assertThatThrownBy(() -> acesso.exigirVinculo(impostor, idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("Familiar fora da rede do idoso recebe 403")
    void familiarSemVinculo() {
        assertThatThrownBy(() -> acesso.exigirVinculo(familiar(), idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class)
                .hasMessageContaining("vínculo");
    }

    @Test
    @DisplayName("Sem usuário autenticado recebe 403")
    void semUsuario() {
        assertThatThrownBy(() -> acesso.exigirVinculo(null, idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("Sem idosoId recebe 400")
    void semIdoso() {
        assertThatThrownBy(() -> acesso.exigirVinculo(familiar(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Alarme: checa o vínculo com o idoso dono do alarme")
    void alarmeDeIdosoDaRede() {
        UUID alarmeId = UUID.randomUUID();
        when(alarmeEmergenciaRepository.findIdosoIdById(alarmeId)).thenReturn(Optional.of(idosoId));
        when(vinculoRedeRepository.existsByFamiliar_KeycloakSubAndIdoso_Id("sub-familiar", idosoId)).thenReturn(true);

        assertThatCode(() -> acesso.exigirVinculoComAlarme(familiar(), alarmeId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Alarme de idoso fora da rede recebe 403")
    void alarmeDeIdosoForaDaRede() {
        UUID alarmeId = UUID.randomUUID();
        when(alarmeEmergenciaRepository.findIdosoIdById(alarmeId)).thenReturn(Optional.of(idosoId));

        assertThatThrownBy(() -> acesso.exigirVinculoComAlarme(familiar(), alarmeId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("Alarme inexistente recebe 404")
    void alarmeInexistente() {
        UUID alarmeId = UUID.randomUUID();
        when(alarmeEmergenciaRepository.findIdosoIdById(alarmeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acesso.exigirVinculoComAlarme(familiar(), alarmeId))
                .isInstanceOf(AlarmeNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Rotina de idoso fora da rede recebe 403")
    void rotinaDeIdosoForaDaRede() {
        UUID rotinaId = UUID.randomUUID();
        when(rotinaMedicamentoRepository.findIdosoIdById(rotinaId)).thenReturn(Optional.of(idosoId));

        assertThatThrownBy(() -> acesso.exigirVinculoComRotina(familiar(), rotinaId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("Rotina inexistente recebe 404")
    void rotinaInexistente() {
        UUID rotinaId = UUID.randomUUID();
        when(rotinaMedicamentoRepository.findIdosoIdById(rotinaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acesso.exigirVinculoComRotina(familiar(), rotinaId))
                .isInstanceOf(RotinaMedicamentoNaoEncontradaException.class);
    }

    private static UsuarioAutenticado familiar() {
        return new UsuarioAutenticado("sub-familiar", "familiar@x.com", "Ana", List.of("familiar"));
    }
}
