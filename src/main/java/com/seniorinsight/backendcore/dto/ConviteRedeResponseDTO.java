package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.backendcore.domain.StatusConvite;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para retorno dos dados do convite gerado para a Rede de Proteção (HU16).
 */
@Schema(description = "Detalhes do convite gerado para a Rede de Proteção (HU16)")
public record ConviteRedeResponseDTO(

        @Schema(description = "Identificador único do convite")
        UUID id,

        @Schema(description = "Identificador único do idoso")
        UUID idosoId,

        @Schema(description = "Nome completo do idoso monitorado")
        String nomeIdoso,

        @Schema(description = "E-mail do familiar convidado")
        String emailDestinatario,

        @Schema(description = "Token temporal gerado via UUID para o convite")
        String token,

        @Schema(description = "Papel de acesso sugerido")
        String papelAcessoSugerido,

        @Schema(description = "Grau de parentesco sugerido")
        String grauParentescoSugerido,

        @Schema(description = "Status atual do convite")
        StatusConvite status,

        @Schema(description = "Data e hora de expiração do convite (48 horas após criação)")
        Instant dataExpiracao,

        @Schema(description = "Data e hora de criação do convite")
        Instant criadoEm,

        @Schema(description = "Mensagem informativa sobre a simulação do envio")
        String mensagemSimulacao
) {
}
