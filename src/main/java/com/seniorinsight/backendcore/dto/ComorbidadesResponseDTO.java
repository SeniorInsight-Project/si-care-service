package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para retorno dos dados atualizados de comorbidades do idoso (HU15).
 */
@Schema(description = "Resposta contendo o identificador do idoso e a lista atualizada de comorbidades")
public record ComorbidadesResponseDTO(

        @Schema(description = "Identificador único do idoso (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID idosoId,

        @Schema(description = "Lista atualizada de comorbidades cadastradas no perfil do idoso", example = "[\"Hipertensão Arterial Sistêmica\", \"Diabetes Mellitus Tipo 2\"]")
        List<String> comorbidades,

        @Schema(description = "Data e hora da última atualização do registro do idoso", example = "2026-09-30T17:45:00Z")
        Instant atualizadoEm
) {
}
