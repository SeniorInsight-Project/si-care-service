package com.seniorinsight.backendcore.mensageria;

import java.util.UUID;

/** Confirma ao report-service que o care-service concluiu a sua parte (si-contracts/events/lgpd.erasure.completed). */
public record ExclusaoConcluida(UUID protocolo, String servico) implements EventoDominio {

    @Override
    public String tipoAgregado() {
        return "pedido_exclusao";
    }

    @Override
    public UUID agregadoId() {
        return protocolo;
    }

    @Override
    public String tipoEvento() {
        return "lgpd.erasure.completed";
    }
}
