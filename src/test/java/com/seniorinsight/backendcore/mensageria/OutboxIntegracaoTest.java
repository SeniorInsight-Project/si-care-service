package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Caminho completo: o caso de uso grava na outbox, o job agendado publica no RabbitMQ e marca a linha. */
class OutboxIntegracaoTest extends IntegracaoBase {

    private static final String ROUTING_KEY = "teste.relay";

    @Autowired
    PublicadorEventos publicador;

    @Autowired
    OutboxEventoRepository repositorio;

    @Autowired
    TransactionTemplate transacao;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publicaOEnvelopeNaExchangeSiEventsComRoutingKeyIgualAoTipoDoEventoEMarcaPublicado() throws Exception {
        Queue fila = amqpAdmin.declareQueue();
        amqpAdmin.declareBinding(BindingBuilder.bind(fila)
                .to(new TopicExchange(MensageriaConfig.EXCHANGE_EVENTOS))
                .with(ROUTING_KEY));
        UUID alarmeId = UUID.randomUUID();

        transacao.executeWithoutResult(status -> publicador.publicar(new EventoTeste(alarmeId, "Maria")));

        Message mensagem = rabbitTemplate.receive(fila.getName(), Duration.ofSeconds(15).toMillis());
        assertThat(mensagem).as("mensagem publicada pelo relay").isNotNull();
        MessageProperties propriedades = mensagem.getMessageProperties();
        assertThat(propriedades.getReceivedExchange()).isEqualTo("si.events");
        assertThat(propriedades.getReceivedRoutingKey()).isEqualTo(ROUTING_KEY);
        assertThat(propriedades.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(propriedades.getAppId()).isEqualTo("care-service");
        assertThat(propriedades.<String>getHeader("agregado_id")).isEqualTo(alarmeId.toString());

        JsonNode envelope = objectMapper.readTree(mensagem.getBody());
        assertThat(envelope.get("specversion").asText()).isEqualTo("1.0");
        assertThat(envelope.get("id").asText()).isEqualTo(propriedades.getMessageId());
        assertThat(envelope.get("source").asText()).isEqualTo("/care-service");
        assertThat(envelope.get("type").asText()).isEqualTo(ROUTING_KEY);
        assertThat(envelope.get("subject").asText()).isEqualTo(alarmeId.toString());
        assertThat(envelope.get("data").get("nome").asText()).isEqualTo("Maria");

        UUID eventoId = UUID.fromString(propriedades.getMessageId());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            OutboxEvento evento = repositorio.findById(eventoId).orElseThrow();
            assertThat(evento.getAgregadoId()).isEqualTo(alarmeId);
            assertThat(evento.getPublicadoEm()).isNotNull();
            assertThat(evento.getTentativas()).isZero();
        });
    }

    @Test
    void eventoNaoEhGravadoQuandoATransacaoDoCasoDeUsoEhDesfeita() {
        UUID alarmeId = UUID.randomUUID();

        transacao.executeWithoutResult(status -> {
            publicador.publicar(new EventoTeste(alarmeId, "João"));
            status.setRollbackOnly();
        });

        assertThat(repositorio.findAll()).noneMatch(evento -> evento.getAgregadoId().equals(alarmeId));
    }

    @Test
    void publicarExigeTransacaoAtiva() {
        assertThatThrownBy(() -> publicador.publicar(new EventoTeste(UUID.randomUUID(), "Ana")))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    /** Routing key própria, para não misturar com eventos reais publicados por outros testes. */
    record EventoTeste(UUID alarmeId, String nome) implements EventoDominio {

        @Override
        public String tipoAgregado() {
            return "alarme";
        }

        @Override
        public UUID agregadoId() {
            return alarmeId;
        }

        @Override
        public String tipoEvento() {
            return ROUTING_KEY;
        }
    }
}
