package com.seniorinsight.backendcore.domain;

/**
 * Estados do ciclo de vida do Alarme de Emergência / SOS (HU35).
 */
public enum StatusAlarme {
    DISPARADO,
    EM_ATENDIMENTO,
    RESOLVIDO,
    CANCELADO;

    /**
     * Verifica se o alarme já se encontra em estado terminal (resolvido ou cancelado).
     */
    public boolean isFinalizado() {
        return this == RESOLVIDO || this == CANCELADO;
    }
}
