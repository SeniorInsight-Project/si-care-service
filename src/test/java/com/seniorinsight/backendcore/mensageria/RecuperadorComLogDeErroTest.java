package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.dao.TransientDataAccessResourceException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Quando as tentativas acabam a mensagem vai para a care.dlq, e isso precisa aparecer no log: erro temporário que
 * estourou o limite é ERROR; evento fora do contrato é só WARN, porque repetir não resolveria.
 */
class RecuperadorComLogDeErroTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(RecuperadorComLogDeErro.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final RecuperadorComLogDeErro recuperador = new RecuperadorComLogDeErro();
    private final UUID mensagemId = UUID.randomUUID();
    private final Message mensagem = MessageBuilder.withBody(new byte[0]).setMessageId(mensagemId.toString())
            .setReceivedRoutingKey("lgpd.erasure.requested").build();

    @BeforeEach
    void anexarAppender() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void removerAppender() {
        logger.detachAppender(logs);
    }

    @Test
    void erroTemporarioQueEstourouOLimiteViraLogDeErroEVaiParaADlq() {
        Throwable causa = falha(new TransientDataAccessResourceException("lock timeout"));

        assertThatThrownBy(() -> recuperador.recover(mensagem, causa))
                .isInstanceOf(ListenerExecutionFailedException.class)
                .hasCauseInstanceOf(AmqpRejectAndDontRequeueException.class);

        assertThat(logs.list).filteredOn(evento -> evento.getLevel() == Level.ERROR).singleElement()
                .satisfies(evento -> {
                    assertThat(evento.getFormattedMessage()).contains(mensagemId.toString(),
                            "lgpd.erasure.requested", "care.dlq");
                    assertThat(evento.getThrowableProxy()).isNotNull();
                });
    }

    @Test
    void eventoInvalidoVaiParaADlqComLogDeAviso() {
        Throwable causa = falha(new AmqpRejectAndDontRequeueException("Evento inválido: sem protocolo"));

        assertThatThrownBy(() -> recuperador.recover(mensagem, causa))
                .isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(logs.list).noneMatch(evento -> evento.getLevel() == Level.ERROR);
        assertThat(logs.list).anySatisfy(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.WARN);
            assertThat(evento.getFormattedMessage()).contains(mensagemId.toString(), "care.dlq");
        });
    }

    private Throwable falha(Throwable causa) {
        return new ListenerExecutionFailedException("Listener method failed", causa, mensagem);
    }
}
