package com.seniorinsight.backendcore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.seniorinsight.backendcore.domain.AuditoriaIdoso;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.repository.AuditoriaIdosoRepository;
import com.seniorinsight.backendcore.repository.ExclusaoDadosRepository;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.TitularExcluidoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/** Exclusão do familiar: o idoso fica, e a auditoria registra quando ele ficou sem administrador. */
class ExclusaoFamiliarServiceTest {

    private static final UUID PROTOCOLO = UUID.randomUUID();
    private static final UUID IDOSO_ID = UUID.randomUUID();
    private static final String SUB = "sub-do-familiar";
    private static final Instant AGORA = Instant.parse("2026-11-12T10:00:00Z");

    private final FamiliarRepository familiares = mock(FamiliarRepository.class);
    private final VinculoRedeRepository vinculos = mock(VinculoRedeRepository.class);
    private final AuditoriaIdosoRepository auditoria = mock(AuditoriaIdosoRepository.class);
    private final TitularExcluidoRepository titulares = mock(TitularExcluidoRepository.class);
    private final ExclusaoDadosRepository dados = mock(ExclusaoDadosRepository.class);
    private final ExclusaoFamiliarService servico = new ExclusaoFamiliarService(familiares, vinculos, auditoria,
            titulares, dados, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void apagaOFamiliarPeloSubAnonimizaAAutoriaERemoveSeusConvites() {
        Familiar familiar = Familiar.builder().id(UUID.randomUUID()).keycloakSub(SUB).nomeCompleto("Ana")
                .email("ana@exemplo.com").build();
        when(familiares.findByKeycloakSub(SUB)).thenReturn(Optional.of(familiar));

        servico.excluir(PROTOCOLO, SUB);

        InOrder ordem = inOrder(titulares, dados);
        ordem.verify(titulares).travar(SUB);
        ordem.verify(dados).apagarConvitesDoEmail("ana@exemplo.com");
        ordem.verify(dados).anonimizarAutoria(SUB);
        ordem.verify(dados).apagarFamiliar(SUB);
        verify(titulares).registrar(SUB);
    }

    @Test
    void familiarSemEmailNaoApagaConvitesPorEmail() {
        Familiar familiar = Familiar.builder().id(UUID.randomUUID()).keycloakSub(SUB).nomeCompleto("Ana").build();
        when(familiares.findByKeycloakSub(SUB)).thenReturn(Optional.of(familiar));

        servico.excluir(PROTOCOLO, SUB);

        verify(dados, never()).apagarConvitesDoEmail(any());
        verify(dados).apagarFamiliar(SUB);
    }

    @Test
    void registraNaAuditoriaOIdosoQueFicouSemAdministrador() {
        Familiar familiar = Familiar.builder().id(UUID.randomUUID()).keycloakSub(SUB).nomeCompleto("Ana").build();
        Idoso idoso = Idoso.builder().id(IDOSO_ID).nomeCompleto("Maria").build();
        Idoso outro = Idoso.builder().id(UUID.randomUUID()).nomeCompleto("José").build();
        when(familiares.findByKeycloakSub(SUB)).thenReturn(Optional.of(familiar));
        when(vinculos.findByFamiliarId(familiar.getId())).thenReturn(List.of(
                VinculoRede.builder().familiar(familiar).idoso(idoso).papelAcesso(PapelAcesso.ADMINISTRADOR).build(),
                VinculoRede.builder().familiar(familiar).idoso(outro).papelAcesso("APOIO").build()));

        servico.excluir(PROTOCOLO, SUB);

        ArgumentCaptor<AuditoriaIdoso> registro = forClass(AuditoriaIdoso.class);
        verify(auditoria).save(registro.capture());
        assertThat(registro.getValue().getIdosoId()).isEqualTo(IDOSO_ID);
        assertThat(registro.getValue().getAutorSub()).isEqualTo(ExclusaoDadosRepository.AUTORIA_ANONIMIZADA);
        assertThat(registro.getValue().getOcorridoEm()).isEqualTo(AGORA);
        assertThat(registro.getValue().getValoresAnteriores()).containsEntry("administrador", true);
        assertThat(registro.getValue().getValoresNovos())
                .containsEntry("administrador", false)
                .containsEntry("motivo", "exclusao-lgpd")
                .containsEntry("protocolo", PROTOCOLO.toString());
        verify(dados).apagarFamiliar(SUB);
    }

    @Test
    void familiarSemCadastroNoCareSoBloqueiaARecriacao() {
        when(familiares.findByKeycloakSub(SUB)).thenReturn(Optional.empty());

        servico.excluir(PROTOCOLO, SUB);

        verify(titulares).registrar(SUB);
        verify(dados, never()).apagarFamiliar(any());
        verify(auditoria, never()).save(any());
    }
}
