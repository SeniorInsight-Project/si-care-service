package com.seniorinsight.backendcore.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.seniorinsight.backendcore.domain.PedidoExclusao;
import com.seniorinsight.backendcore.domain.TipoTitular;
import com.seniorinsight.backendcore.mensageria.ExclusaoConcluida;
import com.seniorinsight.backendcore.mensageria.PublicadorEventos;
import com.seniorinsight.backendcore.repository.ProtocoloExclusaoRepository;

/** Orquestração do pedido de exclusão: reserva do protocolo, exclusão por tipo de titular e confirmação. */
class ExclusaoTitularServiceTest {

    private static final UUID PROTOCOLO = UUID.randomUUID();
    private static final UUID TITULAR = UUID.randomUUID();
    private static final String SUB = "sub-1";

    private final ProtocoloExclusaoRepository protocolos = mock(ProtocoloExclusaoRepository.class);
    private final ExclusaoIdosoService exclusaoIdoso = mock(ExclusaoIdosoService.class);
    private final ExclusaoFamiliarService exclusaoFamiliar = mock(ExclusaoFamiliarService.class);
    private final PublicadorEventos publicador = mock(PublicadorEventos.class);
    private final ExclusaoTitularService servico =
            new ExclusaoTitularService(protocolos, exclusaoIdoso, exclusaoFamiliar, publicador);

    @Test
    void idosoExcluiOIdosoEConfirmaDepois() {
        when(protocolos.reservar(PROTOCOLO)).thenReturn(true);

        servico.tratar(new PedidoExclusao(PROTOCOLO, TipoTitular.IDOSO, TITULAR, SUB));

        InOrder ordem = inOrder(exclusaoIdoso, publicador);
        ordem.verify(exclusaoIdoso).excluir(PROTOCOLO, TITULAR, SUB);
        ordem.verify(publicador).publicar(new ExclusaoConcluida(PROTOCOLO, "care-service"));
        verifyNoInteractions(exclusaoFamiliar);
    }

    @Test
    void familiarExcluiOFamiliarEConfirmaDepois() {
        when(protocolos.reservar(PROTOCOLO)).thenReturn(true);

        servico.tratar(new PedidoExclusao(PROTOCOLO, TipoTitular.FAMILIAR, TITULAR, SUB));

        InOrder ordem = inOrder(exclusaoFamiliar, publicador);
        ordem.verify(exclusaoFamiliar).excluir(PROTOCOLO, SUB);
        ordem.verify(publicador).publicar(new ExclusaoConcluida(PROTOCOLO, "care-service"));
        verifyNoInteractions(exclusaoIdoso);
    }

    @Test
    void protocoloJaReservadoNaoFazNadaNemPublicaDeNovo() {
        when(protocolos.reservar(PROTOCOLO)).thenReturn(false);

        servico.tratar(new PedidoExclusao(PROTOCOLO, TipoTitular.IDOSO, TITULAR, SUB));

        verifyNoInteractions(exclusaoIdoso, exclusaoFamiliar, publicador);
        verify(protocolos).reservar(PROTOCOLO);
    }

    @Test
    void falhaNaExclusaoNaoPublicaAConfirmacao() {
        when(protocolos.reservar(PROTOCOLO)).thenReturn(true);
        doThrow(new IllegalStateException("banco fora"))
                .when(exclusaoIdoso).excluir(any(), any(), any());

        assertThatThrownBy(
                () -> servico.tratar(new PedidoExclusao(PROTOCOLO, TipoTitular.IDOSO, TITULAR, SUB)))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(publicador);
    }
}
