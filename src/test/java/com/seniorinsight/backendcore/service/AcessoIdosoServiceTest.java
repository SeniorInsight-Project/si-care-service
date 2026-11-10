package com.seniorinsight.backendcore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;

class AcessoIdosoServiceTest {

    private static final String DOMINIO = "idoso.seniorinsight.invalid";

    private final IdosoRepository idosoRepository = mock(IdosoRepository.class);
    private final VinculoRedeRepository vinculoRepository = mock(VinculoRedeRepository.class);
    private final AcessoIdosoService service = new AcessoIdosoService(idosoRepository, vinculoRepository, DOMINIO);

    private final UUID idosoId = UUID.randomUUID();
    private final Idoso idoso = Idoso.builder().id(idosoId).nomeCompleto("Maria").build();

    @Test
    @DisplayName("Familiar com vínculo e médico que acompanha passam em exigirVinculo e exigirLeitura")
    void redeEMedicoPassam() {
        existeIdoso();
        when(vinculoRepository.findByFamiliar_KeycloakSubAndIdoso_Id("sub-familiar", idosoId))
                .thenReturn(Optional.of(VinculoRede.builder().papelAcesso("FAMILIAR").build()));
        when(idosoRepository.medicoAcompanha(idosoId, "sub-medico")).thenReturn(true);

        assertThat(service.exigirVinculo(usuario("sub-familiar"), idosoId)).isSameAs(idoso);
        assertThat(service.exigirLeitura(usuario("sub-familiar"), idosoId)).isSameAs(idoso);
        assertThat(service.exigirVinculo(usuario("sub-medico"), idosoId)).isSameAs(idoso);
        assertThat(service.exigirLeitura(usuario("sub-medico"), idosoId)).isSameAs(idoso);
    }

    @Test
    @DisplayName("Quem é da plataforma mas não da rede recebe 403 (PerfilNaoAutorizadoException)")
    void semVinculoEhNegado() {
        existeIdoso();
        when(vinculoRepository.findByFamiliar_KeycloakSubAndIdoso_Id("sub-estranho", idosoId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exigirVinculo(usuario("sub-estranho"), idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
        assertThatThrownBy(() -> service.exigirLeitura(usuario("sub-estranho"), idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("Sem usuário ou sem sub, nega")
    void semUsuarioEhNegado() {
        existeIdoso();

        assertThatThrownBy(() -> service.exigirVinculo(null, idosoId)).isInstanceOf(PerfilNaoAutorizadoException.class);
        assertThatThrownBy(() -> service.exigirLeitura(null, idosoId)).isInstanceOf(PerfilNaoAutorizadoException.class);
        assertThatThrownBy(() -> service.exigirLeitura(usuario(null), idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("Idoso inexistente responde 404 (IdosoNaoEncontradoException) antes de checar o vínculo")
    void idosoInexistente() {
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exigirVinculo(usuario("sub-familiar"), idosoId))
                .isInstanceOf(IdosoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.exigirLeitura(usuario("sub-familiar"), idosoId))
                .isInstanceOf(IdosoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("O aparelho pareado lê, mas não passa em exigirVinculo; o de outro idoso não lê")
    void idosoPareadoSoPassaEmExigirLeitura() {
        existeIdoso();
        UsuarioAutenticado aparelho = aparelho(idosoId, true, "idoso");

        assertThat(service.exigirLeitura(aparelho, idosoId)).isSameAs(idoso);
        assertThatThrownBy(() -> service.exigirVinculo(aparelho, idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
        assertThatThrownBy(() -> service.exigirLeitura(aparelho(UUID.randomUUID(), true, "idoso"), idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("Token com a role idoso, mas sem e-mail verificado ou sem a role, não é o aparelho pareado")
    void idosoPareadoExigeContaTecnicaVerificada() {
        existeIdoso();

        assertThatThrownBy(() -> service.exigirLeitura(aparelho(idosoId, false, "idoso"), idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
        assertThatThrownBy(() -> service.exigirLeitura(aparelho(idosoId, true, "familiar"), idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("exigirAdministrador só aceita o vínculo de Administrador")
    void administrador() {
        existeIdoso();
        when(vinculoRepository.findByFamiliar_KeycloakSubAndIdoso_Id("sub-admin", idosoId))
                .thenReturn(Optional.of(VinculoRede.builder().papelAcesso(PapelAcesso.ADMINISTRADOR).build()));
        when(vinculoRepository.findByFamiliar_KeycloakSubAndIdoso_Id("sub-apoio", idosoId))
                .thenReturn(Optional.of(VinculoRede.builder().papelAcesso("FAMILIAR").build()));

        assertThat(service.exigirAdministrador(usuario("sub-admin"), idosoId)).isSameAs(idoso);
        assertThatThrownBy(() -> service.exigirAdministrador(usuario("sub-apoio"), idosoId))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    private void existeIdoso() {
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));
    }

    private static UsuarioAutenticado usuario(String sub) {
        return new UsuarioAutenticado(sub, "u@seniorinsight.com", "Usuário", List.of("familiar"));
    }

    private static UsuarioAutenticado aparelho(UUID id, boolean emailVerificado, String papel) {
        String conta = "idoso-" + id + "@" + DOMINIO;
        return new UsuarioAutenticado("sub-aparelho-" + id, conta, null, List.of(papel), conta, emailVerificado);
    }
}
