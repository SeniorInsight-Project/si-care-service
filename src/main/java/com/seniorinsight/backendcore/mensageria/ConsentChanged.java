package com.seniorinsight.backendcore.mensageria;

import java.util.UUID;

import com.seniorinsight.backendcore.domain.TipoConsentimento;

public record ConsentChanged(UUID idosoId, TipoConsentimento tipo, String destinatario, boolean autorizado)
        implements EventoDominio {

    @Override
    public String tipoAgregado() {
        return "consentimento";
    }

    @Override
    public UUID agregadoId() {
        return idosoId;
    }

    @Override
    public String tipoEvento() {
        return "consent.changed";
    }
}
