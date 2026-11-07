package com.seniorinsight.backendcore.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Base dos consumidores do care-service. A entrega do RabbitMQ é "pelo menos uma vez", então a mesma mensagem pode
 * chegar de novo; aqui ela é descartada se o {@code message_id} (= id do evento na outbox de quem publicou) já foi
 * tratado por este consumidor.
 *
 * <p>O registro do id e o {@link #tratar} rodam na mesma transação: se o tratamento falhar, o registro volta atrás e a
 * mensagem pode ser reprocessada. Depois das tentativas configuradas em {@code spring.rabbitmq.listener.simple.retry}
 * ela vai para a {@code care.dlq}. Mensagem sem {@code message_id} não tem como ser deduplicada e vai direto para a
 * DLQ.
 *
 * <p>A subclasse tem o {@code @RabbitListener} e chama {@link #processar(Message)}.
 */
public abstract class ConsumidorIdempotente {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorIdempotente.class);

    private final RegistroEventosProcessados registro;
    private final TransactionTemplate transacao;

    protected ConsumidorIdempotente(RegistroEventosProcessados registro, TransactionTemplate transacao) {
        this.registro = registro;
        this.transacao = transacao;
    }

    /** Nome gravado em {@code evento_processado.consumidor}; cada consumidor deduplica por conta própria. */
    protected abstract String nome();

    /** Efeito da mensagem. Roda uma vez por {@code message_id}, dentro da transação do registro. */
    protected abstract void tratar(String mensagemId, Message mensagem);

    protected final void processar(Message mensagem) {
        String mensagemId = mensagem.getMessageProperties().getMessageId();
        if (mensagemId == null || mensagemId.isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Mensagem sem message_id, não dá para deduplicar");
        }
        Boolean tratada = transacao.execute(status -> {
            if (!registro.registrarSeNovo(mensagemId, nome())) {
                return false;
            }
            tratar(mensagemId, mensagem);
            return true;
        });
        if (Boolean.FALSE.equals(tratada)) {
            log.info("Mensagem {} já tratada por {}, ignorando", mensagemId, nome());
        }
    }
}
