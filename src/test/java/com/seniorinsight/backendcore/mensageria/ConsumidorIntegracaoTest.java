package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** A care.q recebe os eventos de user.created; mensagem repetida é ignorada e mensagem inválida vai para a DLQ. */
class ConsumidorIntegracaoTest extends IntegracaoBase {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void mensagemRepetidaEhTratadaUmaSoVez() {
        String id = UUID.randomUUID().toString();
        String outro = UUID.randomUUID().toString();

        enviar(id);
        enviar(id);
        enviar(outro);

        // A terceira mensagem vem depois da repetida na fila: quando ela foi tratada, a repetida já passou.
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(contar(outro)).isEqualTo(1));
        assertThat(contar(id)).isEqualTo(1);
    }

    @Test
    void mensagemSemMessageIdVaiParaADlqDepoisDasTentativas() {
        Message semId = MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "user.created", semId);

        Message morta = rabbitTemplate.receive(MensageriaConfig.FILA_DLQ, Duration.ofSeconds(20).toMillis());
        assertThat(morta).as("mensagem na care.dlq").isNotNull();
    }

    private void enviar(String messageId) {
        Message mensagem = MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId)
                .build();
        rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "user.created", mensagem);
    }

    private int contar(String mensagemId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM evento_processado WHERE mensagem_id = ? AND consumidor = 'care'",
                Integer.class, mensagemId);
    }
}
