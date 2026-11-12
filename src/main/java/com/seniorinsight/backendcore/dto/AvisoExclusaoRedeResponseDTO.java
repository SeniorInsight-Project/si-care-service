package com.seniorinsight.backendcore.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** Quem da rede do idoso deve ser avisado da exclusão dos dados dele, para o notification-service enviar o aviso. */
@Schema(description = "Destinatários do aviso de exclusão dos dados de um idoso (HU-022)")
public record AvisoExclusaoRedeResponseDTO(

        @Schema(description = "Protocolo do pedido de exclusão")
        UUID protocolo,

        @Schema(description = "Primeiro nome do idoso, nulo se ele não tinha cadastro no care-service", example = "Sebastiana")
        String primeiroNomeIdoso,

        @Schema(description = "Membros da rede no momento da exclusão; vazia se não havia ninguém a avisar")
        List<Membro> membros
) {

    @Schema(description = "Membro da rede a avisar")
    public record Membro(

            @Schema(description = "Identificador do usuário do familiar (claim sub do Keycloak)")
            String usuarioId,

            @Schema(description = "Nome completo do familiar")
            String nomeCompleto,

            @Schema(description = "E-mail do familiar, se houver")
            String email
    ) {
    }
}
