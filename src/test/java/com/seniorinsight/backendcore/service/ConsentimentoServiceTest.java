package com.seniorinsight.backendcore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.domain.TipoConsentimento;
import com.seniorinsight.backendcore.dto.ConsentimentosDTO;
import com.seniorinsight.backendcore.mensageria.ConsentChanged;
import com.seniorinsight.backendcore.mensageria.PublicadorEventos;
import com.seniorinsight.backendcore.repository.ConsentimentoRepository;
import com.seniorinsight.backendcore.repository.ConsentimentoRepository.Decisao;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class ConsentimentoServiceTest {

    private static final UUID IDOSO = UUID.randomUUID();
    private static final String DOMINIO = "idoso.seniorinsight.invalid";
    private static final ConsentimentosDTO DESLIGADOS = new ConsentimentosDTO(false, false, false, false, false);
    private final ConsentimentoRepository repositorio = mock(ConsentimentoRepository.class);
    private final PublicadorEventos publicador = mock(PublicadorEventos.class);
    private final ConsentimentoService service = new ConsentimentoService(repositorio, publicador, DOMINIO, "v1");

    @Test
    void comecaComTodosOsTiposDesligados() {
        when(repositorio.existeIdoso(IDOSO)).thenReturn(true);
        when(repositorio.buscarAtuais(IDOSO)).thenReturn(Map.of());

        assertThat(service.consultar(IDOSO, titular(IDOSO))).isEqualTo(DESLIGADOS);
        verifyNoInteractions(publicador);
    }

    @Test
    void revogarPassosRegistraHistoricoEPublicaUmaVez() {
        when(repositorio.bloquearIdoso(IDOSO)).thenReturn(true);
        when(repositorio.buscarAtuais(IDOSO)).thenReturn(Map.of(
                TipoConsentimento.BATIMENTOS, new Decisao(false, "v1"),
                TipoConsentimento.PASSOS, new Decisao(true, "v1"),
                TipoConsentimento.SONO, new Decisao(false, "v1"),
                TipoConsentimento.OXIGENACAO, new Decisao(false, "v1"),
                TipoConsentimento.LOCALIZACAO, new Decisao(false, "v1")));

        assertThat(service.salvar(IDOSO, titular(IDOSO), DESLIGADOS)).isEqualTo(DESLIGADOS);

        verify(repositorio).registrar(IDOSO, TipoConsentimento.PASSOS, new Decisao(false, "v1"), "titular");
        verify(publicador).publicar(new ConsentChanged(IDOSO, TipoConsentimento.PASSOS, "REDE", false));
        verifyNoMoreInteractions(publicador);
    }

    @Test
    void familiarAdministradorSemCuratelaNaoPodeConsultarNemAlterar() {
        Jwt familiar = familiar();
        assertThatThrownBy(() -> service.consultar(IDOSO, familiar)).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Somente o idoso pode autorizar o uso dos próprios dados");
        assertThatThrownBy(() -> service.salvar(IDOSO, familiar, DESLIGADOS)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(publicador);
    }

    @Test
    void curadorComVinculoPodeConsultar() {
        when(repositorio.curatelaAtiva(IDOSO, "familiar")).thenReturn(true);
        when(repositorio.existeIdoso(IDOSO)).thenReturn(true);
        when(repositorio.buscarAtuais(IDOSO)).thenReturn(Map.of());

        assertThat(service.consultar(IDOSO, familiar())).isEqualTo(DESLIGADOS);
    }

    @Test
    void contaPareadaDeOutroIdosoNaoPodeAlterar() {
        assertThatThrownBy(() -> service.salvar(IDOSO, titular(UUID.randomUUID()), DESLIGADOS))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(publicador);
    }

    @Test
    void eventoSerializadoTemApenasOsCamposDoContrato() {
        var json = new ObjectMapper().valueToTree(new ConsentChanged(IDOSO, TipoConsentimento.PASSOS, "REDE", false));

        assertThat(json.size()).isEqualTo(4);
        assertThat(json.path("idosoId").asText()).isEqualTo(IDOSO.toString());
        assertThat(json.path("tipo").asText()).isEqualTo("PASSOS");
        assertThat(json.path("destinatario").asText()).isEqualTo("REDE");
        assertThat(json.path("autorizado").isBoolean()).isTrue();
        assertThat(json.path("autorizado").asBoolean()).isFalse();
    }

    private static Jwt titular(UUID idosoId) {
        String conta = "idoso-" + idosoId + "@" + DOMINIO;
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("titular")
                .claim("preferred_username", conta).claim("email", conta).claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of("idoso"))).build();
    }

    private static Jwt familiar() {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("familiar")
                .claim("realm_access", Map.of("roles", List.of("familiar"))).build();
    }
}
