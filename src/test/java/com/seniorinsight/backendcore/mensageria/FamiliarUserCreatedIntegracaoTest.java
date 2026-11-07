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

/** user.created vira Familiar (sem CPF), sem duplicar, e o evento inválido vai para a care.dlq. */
class FamiliarUserCreatedIntegracaoTest extends IntegracaoBase {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void userCreatedDeFamiliarCriaOFamiliarSemCpf() {
        String sub = UUID.randomUUID().toString();

        enviar(UUID.randomUUID().toString(), EventosDeTeste.userCreated(sub, "FAMILIAR", "Ana Souza"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(contarFamiliares(sub)).isEqualTo(1));
        var linha = jdbc.queryForMap("SELECT nome_completo, email, cpf FROM familiares WHERE keycloak_sub = ?", sub);
        assertThat(linha.get("nome_completo")).isEqualTo("Ana Souza");
        assertThat(linha.get("email")).isEqualTo(sub + "@exemplo.com");
        assertThat(linha.get("cpf")).isNull();
    }

    @Test
    void mesmoPerfilEntregueDuasVezesNaoDuplica() {
        String sub = UUID.randomUUID().toString();
        String mensagemId = UUID.randomUUID().toString();
        String corpo = EventosDeTeste.userCreated(sub, "FAMILIAR", "Bia");

        // Mesma mensagem repetida e outra mensagem (id diferente) para o mesmo sub.
        enviar(mensagemId, corpo);
        enviar(mensagemId, corpo);
        enviar(UUID.randomUUID().toString(), corpo);
        String sentinela = UUID.randomUUID().toString();
        enviar(UUID.randomUUID().toString(), EventosDeTeste.userCreated(sentinela, "FAMILIAR", "Fim"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(contarFamiliares(sentinela)).isEqualTo(1));
        assertThat(contarFamiliares(sub)).isEqualTo(1);
    }

    @Test
    void userCreatedDeIdosoNaoCriaFamiliar() {
        String sub = UUID.randomUUID().toString();
        String sentinela = UUID.randomUUID().toString();

        enviar(UUID.randomUUID().toString(), EventosDeTeste.userCreated(sub, "IDOSO", "Seu Jorge"));
        enviar(UUID.randomUUID().toString(), EventosDeTeste.userCreated(sentinela, "FAMILIAR", "Fim"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(contarFamiliares(sentinela)).isEqualTo(1));
        assertThat(contarFamiliares(sub)).isZero();
    }

    @Test
    void eventoInvalidoVaiParaADlqEnaoCriaFamiliar() {
        String corpo = EventosDeTeste.userCreated(UUID.randomUUID().toString(), "FAMILIAR", "Sem sub")
                .replaceFirst("\"keycloakSub\":\"[^\"]*\",", "");

        enviar(UUID.randomUUID().toString(), corpo);

        Message morta = rabbitTemplate.receive(MensageriaConfig.FILA_DLQ, Duration.ofSeconds(20).toMillis());
        assertThat(morta).as("mensagem na care.dlq").isNotNull();
        assertThat(new String(morta.getBody(), StandardCharsets.UTF_8)).contains("Sem sub");
    }

    private void enviar(String messageId, String corpo) {
        Message mensagem = MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId)
                .build();
        rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "user.created", mensagem);
    }

    private int contarFamiliares(String sub) {
        return jdbc.queryForObject("SELECT count(*) FROM familiares WHERE keycloak_sub = ?", Integer.class, sub);
    }
}
