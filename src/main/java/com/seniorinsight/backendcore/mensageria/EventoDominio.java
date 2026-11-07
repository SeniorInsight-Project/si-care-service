package com.seniorinsight.backendcore.mensageria;

import java.util.UUID;

/**
 * Evento de domínio que sai do care-service para os outros serviços pela outbox. O próprio objeto vira o campo
 * {@code data} do envelope CloudEvents, então só deve ter campos que fazem parte do contrato em si-contracts.
 */
public interface EventoDominio {

    /** Tipo do agregado de origem, ex.: {@code alarme}. */
    String tipoAgregado();

    /** Id do agregado de origem; também vira o {@code subject} do envelope. */
    UUID agregadoId();

    /** Tipo do evento, ex.: {@code sos.updated}. É a routing key na exchange {@code si.events}. */
    String tipoEvento();
}
