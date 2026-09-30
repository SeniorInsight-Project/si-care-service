package com.seniorinsight.backendcore.domain;

/**
 * Status de ingestão de um medicamento na rotina do idoso (HU26).
 * TOMADO: Ingerido dentro da janela de tolerância de 30 minutos.
 * ATRASADO: Ingerido após a janela de tolerância de 30 minutos.
 * IGNORADO: O idoso ou cuidador optou por não ingerir a dose.
 * PENDENTE: Dose diária agendada que ainda aguarda confirmação.
 */
public enum StatusIngestao {
    TOMADO,
    ATRASADO,
    IGNORADO,
    PENDENTE
}
