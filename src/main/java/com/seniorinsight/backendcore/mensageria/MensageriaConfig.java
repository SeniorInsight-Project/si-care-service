package com.seniorinsight.backendcore.mensageria;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Topologia do care-service no RabbitMQ, declarada (de forma idempotente) quando a conexão abre.
 *
 * <p>{@code si.events} é a exchange topic compartilhada pelos serviços. A fila {@code care.q} recebe os eventos que o
 * care consome; a mensagem rejeitada (depois das tentativas) vai para {@code care.dlq} pela exchange
 * {@code si.events.dlx}. As routing keys da {@code care.q} vêm de {@code care.mensageria.eventos-consumidos}.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class MensageriaConfig {

    public static final String EXCHANGE_EVENTOS = "si.events";
    public static final String EXCHANGE_DLX = "si.events.dlx";
    public static final String FILA = "care.q";
    public static final String FILA_DLQ = "care.dlq";

    @Bean
    TopicExchange exchangeEventos() {
        return ExchangeBuilder.topicExchange(EXCHANGE_EVENTOS).durable(true).build();
    }

    @Bean
    DirectExchange exchangeDeadLetter() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLX).durable(true).build();
    }

    @Bean
    Queue filaCare() {
        return QueueBuilder.durable(FILA)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(FILA)
                .build();
    }

    @Bean
    Queue filaCareDlq() {
        return QueueBuilder.durable(FILA_DLQ).build();
    }

    @Bean
    Declarables bindings(TopicExchange exchangeEventos, DirectExchange exchangeDeadLetter, Queue filaCare,
                         Queue filaCareDlq,
                         @Value("${care.mensageria.eventos-consumidos}") List<String> eventosConsumidos) {
        List<Declarable> bindings = new ArrayList<>();
        for (String routingKey : eventosConsumidos) {
            bindings.add(BindingBuilder.bind(filaCare).to(exchangeEventos).with(routingKey));
        }
        bindings.add(BindingBuilder.bind(filaCareDlq).to(exchangeDeadLetter).with(FILA));
        return new Declarables(bindings);
    }
}
