package com.seniorinsight.backendcore.domain;

import jakarta.persistence.Embeddable;

/** Membro da rede que deve ser avisado da exclusão dos dados do idoso: o mesmo trio que a lista da rede expõe. */
@Embeddable
public record DestinatarioAviso(String usuarioId, String nomeCompleto, String email) {
}
