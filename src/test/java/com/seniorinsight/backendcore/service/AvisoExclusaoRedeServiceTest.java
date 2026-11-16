package com.seniorinsight.backendcore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seniorinsight.backendcore.domain.AvisoExclusaoRede;
import com.seniorinsight.backendcore.domain.DestinatarioAviso;
import com.seniorinsight.backendcore.dto.AvisoExclusaoRedeResponseDTO;
import com.seniorinsight.backendcore.exception.AvisoExclusaoNaoEncontradoException;
import com.seniorinsight.backendcore.repository.AvisoExclusaoRedeRepository;

/** Consulta do aviso à rede pelo protocolo: 404 enquanto o pedido não foi processado. */
class AvisoExclusaoRedeServiceTest {

    private static final UUID PROTOCOLO = UUID.randomUUID();

    private final AvisoExclusaoRedeRepository avisos = mock(AvisoExclusaoRedeRepository.class);
    private final AvisoExclusaoRedeService servico = new AvisoExclusaoRedeService(avisos);

    @Test
    void devolveOPrimeiroNomeEOsMembrosDoAviso() {
        when(avisos.findById(PROTOCOLO)).thenReturn(Optional.of(new AvisoExclusaoRede(PROTOCOLO, "Dona",
                List.of(new DestinatarioAviso("sub-ana", "Ana Lima", "ana@exemplo.com"),
                        new DestinatarioAviso("sub-caio", "Caio Lima", null)),
                Instant.parse("2026-11-12T10:00:00Z"))));

        AvisoExclusaoRedeResponseDTO resposta = servico.consultar(PROTOCOLO);

        assertThat(resposta.protocolo()).isEqualTo(PROTOCOLO);
        assertThat(resposta.primeiroNomeIdoso()).isEqualTo("Dona");
        assertThat(resposta.membros()).containsExactly(
                new AvisoExclusaoRedeResponseDTO.Membro("sub-ana", "Ana Lima", "ana@exemplo.com"),
                new AvisoExclusaoRedeResponseDTO.Membro("sub-caio", "Caio Lima", null));
    }

    @Test
    void protocoloAindaNaoProcessadoNaoTemAviso() {
        when(avisos.findById(PROTOCOLO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.consultar(PROTOCOLO)).isInstanceOf(AvisoExclusaoNaoEncontradoException.class);
    }
}
