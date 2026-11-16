package com.seniorinsight.backendcore.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.stereotype.Component;

/**
 * Chamado quando as tentativas de {@code spring.rabbitmq.listener.simple.retry} acabam: a mensagem é rejeitada sem
 * requeue e o broker a manda para a {@code care.dlq}, como já acontecia, mas agora com log para quem monitora.
 * Evento fora do contrato é WARN (repetir não resolveria). Qualquer outra falha (banco, lock) é ERROR: era um erro
 * temporário que não passou no limite, e a mensagem parada na dead-letter precisa ser vista e reprocessada.
 */
@Component
class RecuperadorComLogDeErro extends RejectAndDontRequeueRecoverer {

    private static final Logger log = LoggerFactory.getLogger(RecuperadorComLogDeErro.class);

    @Override
    public void recover(Message mensagem, Throwable causa) {
        Object identificacao = identificar(mensagem);
        if (causa.getCause() instanceof AmqpRejectAndDontRequeueException) {
            log.warn("Mensagem {} rejeitada e enviada para a {}: {}", identificacao, MensageriaConfig.FILA_DLQ,
                    causa.getCause().getMessage());
        } else {
            log.error("Mensagem {} falhou em todas as tentativas e foi enviada para a {}, precisa de reprocessamento",
                    identificacao, MensageriaConfig.FILA_DLQ, causa);
        }
        super.recover(mensagem, causa);
    }

    private static String identificar(Message mensagem) {
        return mensagem.getMessageProperties().getMessageId() + " (" + tipo(mensagem) + ")";
    }

    private static String tipo(Message mensagem) {
        String tipo = mensagem.getMessageProperties().getReceivedRoutingKey();
        return tipo != null ? tipo : mensagem.getMessageProperties().getType();
    }
}
