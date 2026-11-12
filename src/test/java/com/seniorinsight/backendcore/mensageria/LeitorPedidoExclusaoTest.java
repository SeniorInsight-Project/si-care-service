package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.domain.PedidoExclusao;
import com.seniorinsight.backendcore.domain.TipoTitular;

/** Validação do envelope de lgpd.erasure.requested, sem broker nem banco. */
class LeitorPedidoExclusaoTest {

    private static final UUID MENSAGEM = UUID.randomUUID();
    private static final UUID PROTOCOLO = UUID.randomUUID();
    private static final UUID TITULAR = UUID.randomUUID();
    private static final UUID OUTRO = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LeitorPedidoExclusao leitor = new LeitorPedidoExclusao();

    @Test
    void envelopeValidoViraOPedido() throws Exception {
        assertThat(ler(corpo -> corpo))
                .isEqualTo(new PedidoExclusao(PROTOCOLO, TipoTitular.IDOSO, TITULAR, "sub-1"));
    }

    @Test
    void familiarViraPedidoDeFamiliar() throws Exception {
        assertThat(ler(corpo -> corpo.replace("\"IDOSO\"", "\"FAMILIAR\"")).tipo()).isEqualTo(TipoTitular.FAMILIAR);
    }

    @Test
    void envelopeInvalidoEhRejeitadoSemRequeue() {
        List<UnaryOperator<String>> corrupcoes = List.of(
                c -> c.replace("\"specversion\":\"1.0\"", "\"specversion\":\"0.3\""),
                c -> c.replace("/report-service", "/identity-service"),
                c -> c.replace("\"type\":\"lgpd.erasure.requested\"", "\"type\":\"lgpd.erasure.completed\""),
                c -> c.replace("lgpd.erasure.requested.v1.json", "outro.v1.json"),
                c -> c.replace("\"id\":\"" + MENSAGEM + "\"", "\"id\":\"" + OUTRO + "\""),
                c -> c.replace("\"subject\":\"" + PROTOCOLO + "\"", "\"subject\":\"" + OUTRO + "\""),
                c -> c.replace("\"datacontenttype\":\"application/json\"", "\"datacontenttype\":\"text/plain\""),
                c -> c.replace("\"time\":\"2026-11-08T15:30:00Z\"", "\"time\":\"ontem\""),
                c -> c.replace("\"IDOSO\"", "\"MEDICO\""),
                c -> c.replace("\"titularId\":\"" + TITULAR + "\"", "\"titularId\":\"nao-e-uuid\""),
                c -> c.replace("\"protocolo\":\"" + PROTOCOLO + "\"", "\"protocolo\":\"nao-e-uuid\""),
                c -> c.replace("\"titularSub\":\"sub-1\"", "\"titularSub\":\"\""),
                c -> c.replace("\"titularSub\":\"sub-1\"", "\"titularSub\":\"" + "x".repeat(256) + "\""),
                c -> c.replace(",\"titularSub\":\"sub-1\"", ""),
                c -> c.replace("\"data\":{", "\"dados\":{"));

        for (UnaryOperator<String> corrupcao : corrupcoes) {
            assertThatThrownBy(() -> ler(corrupcao)).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }
    }

    private PedidoExclusao ler(UnaryOperator<String> alteracao) throws Exception {
        String corpo = alteracao.apply(
                EventosDeTeste.exclusaoSolicitada(MENSAGEM, PROTOCOLO, "IDOSO", TITULAR, "sub-1"));
        return leitor.ler(objectMapper.readTree(corpo), MENSAGEM.toString());
    }
}
