package com.seniorinsight.backendcore.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consome a {@code care.q}. Por enquanto só registra a chegada: o tratamento de cada tipo de evento entra nos cards
 * que passarem a depender deles.
 */
@Component
class ConsumidorCare extends ConsumidorIdempotente {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorCare.class);

    ConsumidorCare(RegistroEventosProcessados registro, TransactionTemplate transacao) {
        super(registro, transacao);
    }

    @RabbitListener(queues = MensageriaConfig.FILA)
    void receber(Message mensagem) {
        processar(mensagem);
    }

    @Override
    protected String nome() {
        return "care";
    }

    @Override
    protected void tratar(String mensagemId, Message mensagem) {
        log.info("Evento {} recebido ({})", mensagemId, mensagem.getMessageProperties().getReceivedRoutingKey());
    }
}
