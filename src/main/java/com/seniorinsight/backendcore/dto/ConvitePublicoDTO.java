package com.seniorinsight.backendcore.dto;

import java.time.Instant;

import com.seniorinsight.backendcore.domain.StatusConvite;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO com os dados do convite mostrados a quem ainda não é da Rede de Proteção (HU16).
 * Não traz e-mail, telefone nem dado de saúde, e do idoso só o primeiro nome.
 */
@Schema(description = "Dados do convite mostrados antes do aceite, sem e-mail, telefone ou dado de saúde (HU16)")
public record ConvitePublicoDTO(

        @Schema(description = "Nome de quem convidou", example = "Carlos Eduardo Souza")
        String nomeConvidante,

        @Schema(description = "Primeiro nome do idoso", example = "Maria")
        String primeiroNomeIdoso,

        @Schema(description = "Papel de acesso oferecido", example = "CUIDADOR")
        String papelAcesso,

        @Schema(description = "Grau de parentesco sugerido", example = "Filho", nullable = true)
        String grauParentesco,

        @Schema(description = "Data e hora de expiração do convite")
        Instant expiraEm,

        @Schema(description = "Situação do convite", example = "PENDENTE")
        StatusConvite status
) {
}
