package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.service.ProvisionarFamiliarService;

/** Regras de leitura do user.created, sem broker nem banco. */
class ConsumidorCareTest {

    private final ProvisionarFamiliarService servico = mock(ProvisionarFamiliarService.class);
    private final ConsumidorCare consumidor = new ConsumidorCare(mock(RegistroEventosProcessados.class), null,
            new ObjectMapper(), servico);

    @Test
    void familiarChamaOServicoComSubNomeEEmail() {
        consumidor.tratar("m1", msg(EventosDeTeste.userCreated("sub-1", "FAMILIAR", "Ana")));

        verify(servico).criarSeAusente("sub-1", "Ana", "sub-1@exemplo.com");
    }

    @Test
    void idosoEEventoDeOutroTipoNaoCriamFamiliar() {
        consumidor.tratar("m2", msg(EventosDeTeste.userCreated("sub-2", "IDOSO", "Jorge")));
        consumidor.tratar("m3", msg("{\"type\":\"sos.updated\",\"data\":{}}"));

        verify(servico, never()).criarSeAusente(any(), any(), any());
    }

    @Test
    void eventoInvalidoEhRejeitadoSemRequeue() {
        for (String corpo : new String[] {
                "nao e json",
                "[]",
                "{\"type\":\"user.created\"}",
                EventosDeTeste.userCreated("s", "MEDICO", "X"),
                EventosDeTeste.userCreated("", "FAMILIAR", "X"),
                EventosDeTeste.userCreated("x".repeat(65), "FAMILIAR", "X"),
                EventosDeTeste.userCreated("s", "FAMILIAR", "")}) {
            assertThatThrownBy(() -> consumidor.tratar("m", msg(corpo)))
                    .as(corpo).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }
        verify(servico, never()).criarSeAusente(any(), any(), any());
    }

    private static Message msg(String corpo) {
        return MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
                .setMessageId(UUID.randomUUID().toString()).build();
    }
}
