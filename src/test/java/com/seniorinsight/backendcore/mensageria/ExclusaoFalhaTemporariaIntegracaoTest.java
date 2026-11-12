package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Erro temporário (banco, lock) na exclusão: o RabbitMQ tenta de novo com intervalo e limite (3 tentativas); se
 * passar, a exclusão vale uma vez só, e se estourar o limite sobra um log de erro e a mensagem na care.dlq.
 *
 * <p>A falha vem de um gatilho no banco, e não de um mock, porque os contextos de teste em cache continuam
 * consumindo a mesma care.q: um mock só existiria num deles.
 */
class ExclusaoFalhaTemporariaIntegracaoTest extends IntegracaoBase {

    private static final Duration ESPERA = Duration.ofSeconds(30);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    JdbcTemplate jdbc;

    private final Logger logger = (Logger) LoggerFactory.getLogger(RecuperadorComLogDeErro.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void anexarAppender() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void removerAppenderEGatilho() {
        logger.detachAppender(logs);
        jdbc.execute("DROP TRIGGER IF EXISTS falhar_teste ON exclusao_protocolo");
        jdbc.execute("DROP FUNCTION IF EXISTS falhar_teste()");
        jdbc.execute("DROP TABLE IF EXISTS falha_teste");
    }

    @Test
    void falhaTemporariaQuePassaNaTentativaSeguinteExcluiEConfirmaUmaVez() {
        UUID idoso = idoso();
        UUID protocolo = UUID.randomUUID();
        falharAte(protocolo, "200 milliseconds");

        enviar(protocolo, idoso);

        await().atMost(ESPERA).untilAsserted(() -> assertThat(confirmacoes(protocolo)).isEqualTo(1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idosos WHERE id = ?", Integer.class, idoso)).isZero();
        assertThat(logs.list).noneMatch(evento -> evento.getLevel() == Level.ERROR);
    }

    @Test
    void falhaTemporariaQueEstouraOLimiteLogaErroEVaiParaADlqSemApagarNada() {
        UUID idoso = idoso();
        UUID protocolo = UUID.randomUUID();
        falharAte(protocolo, "1 hour");

        enviar(protocolo, idoso);

        await().atMost(ESPERA).untilAsserted(() -> assertThat(naDlq(protocolo)).isTrue());
        assertThat(logs.list).anySatisfy(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
            assertThat(evento.getFormattedMessage()).contains("lgpd.erasure.requested", "care.dlq");
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idosos WHERE id = ?", Integer.class, idoso)).isEqualTo(1);
        assertThat(confirmacoes(protocolo)).isZero();
    }

    /** A reserva do protocolo falha com erro de serialização (40001) até o prazo; as tentativas são 0, 0,1 e 0,3 s. */
    private void falharAte(UUID protocolo, String prazo) {
        jdbc.execute("CREATE TABLE falha_teste (protocolo UUID PRIMARY KEY, ate TIMESTAMPTZ NOT NULL)");
        jdbc.update("INSERT INTO falha_teste VALUES (?, now() + ?::interval)", protocolo, prazo);
        jdbc.execute("""
                CREATE FUNCTION falhar_teste() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF EXISTS (SELECT 1 FROM falha_teste WHERE protocolo = NEW.protocolo AND ate > clock_timestamp()) THEN
                        RAISE EXCEPTION 'falha temporária de teste' USING ERRCODE = '40001';
                    END IF;
                    RETURN NEW;
                END $$
                """);
        jdbc.execute("""
                CREATE TRIGGER falhar_teste BEFORE INSERT ON exclusao_protocolo
                FOR EACH ROW EXECUTE FUNCTION falhar_teste()
                """);
    }

    private UUID idoso() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO idosos (id, nome_completo, data_nascimento) VALUES (?, 'Seu Jorge', DATE '1950-01-01')", id);
        return id;
    }

    private void enviar(UUID protocolo, UUID idoso) {
        UUID mensagem = UUID.randomUUID();
        String corpo = EventosDeTeste.exclusaoSolicitada(mensagem, protocolo, "IDOSO", idoso, "sub-" + idoso);
        rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "lgpd.erasure.requested",
                MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(mensagem.toString()).build());
    }

    private boolean naDlq(UUID protocolo) {
        Message morta = rabbitTemplate.receive(MensageriaConfig.FILA_DLQ);
        return morta != null && new String(morta.getBody(), StandardCharsets.UTF_8).contains(protocolo.toString());
    }

    private int confirmacoes(UUID protocolo) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM outbox_evento WHERE tipo_evento = 'lgpd.erasure.completed' AND agregado_id = ?
                """, Integer.class, protocolo);
    }
}
