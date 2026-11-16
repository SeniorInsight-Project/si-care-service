package com.seniorinsight.backendcore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.seniorinsight.backendcore.domain.AvisoExclusaoRede;
import com.seniorinsight.backendcore.domain.DestinatarioAviso;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.repository.AvisoExclusaoRedeRepository;
import com.seniorinsight.backendcore.repository.ExclusaoDadosRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.TitularExcluidoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/** Exclusão do idoso: o aviso à rede é gravado antes de apagar, e o titular fica bloqueado para recriação. */
class ExclusaoIdosoServiceTest {

    private static final UUID PROTOCOLO = UUID.randomUUID();
    private static final UUID IDOSO_ID = UUID.randomUUID();
    private static final String SUB = "sub-do-idoso";
    private static final Instant AGORA = Instant.parse("2026-11-12T10:00:00Z");

    private final IdosoRepository idosos = mock(IdosoRepository.class);
    private final VinculoRedeRepository vinculos = mock(VinculoRedeRepository.class);
    private final AvisoExclusaoRedeRepository avisos = mock(AvisoExclusaoRedeRepository.class);
    private final TitularExcluidoRepository titulares = mock(TitularExcluidoRepository.class);
    private final ExclusaoDadosRepository dados = mock(ExclusaoDadosRepository.class);
    private final ExclusaoIdosoService servico = new ExclusaoIdosoService(idosos, vinculos, avisos, titulares, dados,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void gravaOAvisoDaRedeAntesDeApagarOIdoso() {
        Idoso idoso = idoso("Dona Sebastiana Lima");
        Familiar ana = Familiar.builder().keycloakSub("sub-ana").nomeCompleto("Ana Lima").email("ana@exemplo.com").build();
        Familiar caio = Familiar.builder().keycloakSub("sub-caio").nomeCompleto("Caio Lima").build();
        when(idosos.travarParaEscrita(IDOSO_ID)).thenReturn(Optional.of(IDOSO_ID));
        when(idosos.findById(IDOSO_ID)).thenReturn(Optional.of(idoso));
        when(vinculos.findByIdosoIdWithFamiliar(IDOSO_ID)).thenReturn(List.of(
                VinculoRede.builder().familiar(ana).idoso(idoso).build(),
                VinculoRede.builder().familiar(caio).idoso(idoso).build()));

        servico.excluir(PROTOCOLO, IDOSO_ID, SUB);

        ArgumentCaptor<AvisoExclusaoRede> aviso = forClass(AvisoExclusaoRede.class);
        InOrder ordem = inOrder(avisos, dados);
        ordem.verify(avisos).save(aviso.capture());
        ordem.verify(dados).apagarIdoso(IDOSO_ID);
        assertThat(aviso.getValue().getProtocolo()).isEqualTo(PROTOCOLO);
        assertThat(aviso.getValue().getPrimeiroNomeIdoso()).isEqualTo("Dona");
        assertThat(aviso.getValue().getCriadoEm()).isEqualTo(AGORA);
        assertThat(aviso.getValue().getDestinatarios()).containsExactly(
                new DestinatarioAviso("sub-ana", "Ana Lima", "ana@exemplo.com"),
                new DestinatarioAviso("sub-caio", "Caio Lima", null));
    }

    @Test
    void bloqueiaARecriacaoPeloSubEPeloIdEApagaOsEventosPendentes() {
        when(idosos.travarParaEscrita(IDOSO_ID)).thenReturn(Optional.of(IDOSO_ID));
        when(idosos.findById(IDOSO_ID)).thenReturn(Optional.of(idoso("Maria")));

        servico.excluir(PROTOCOLO, IDOSO_ID, SUB);

        verify(titulares).registrar(SUB);
        verify(titulares).registrar(IDOSO_ID.toString());
        verify(dados).apagarEventosPendentesDoIdoso(IDOSO_ID);
    }

    @Test
    void idosoSemCadastroNoCareGravaAvisoVazioENaoApagaNada() {
        when(idosos.travarParaEscrita(IDOSO_ID)).thenReturn(Optional.empty());

        servico.excluir(PROTOCOLO, IDOSO_ID, SUB);

        ArgumentCaptor<AvisoExclusaoRede> aviso = forClass(AvisoExclusaoRede.class);
        verify(avisos).save(aviso.capture());
        assertThat(aviso.getValue().getDestinatarios()).isEmpty();
        assertThat(aviso.getValue().getPrimeiroNomeIdoso()).isNull();
        verifyNoInteractions(dados, titulares, vinculos);
    }

    private static Idoso idoso(String nome) {
        return Idoso.builder().id(IDOSO_ID).nomeCompleto(nome).dataNascimento(LocalDate.of(1940, 10, 12)).build();
    }
}
