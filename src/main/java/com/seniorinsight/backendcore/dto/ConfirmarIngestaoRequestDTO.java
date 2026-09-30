package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seniorinsight.backendcore.domain.StatusIngestao;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para confirmação de ingestão de medicamento (HU26).
 * Permite registrar a ingestão informando opcionalmente o timestamp exato,
 * data de referência ou status forçado (ex: IGNORADO quando o idoso opta por não tomar).
 */
public record ConfirmarIngestaoRequestDTO(

        @Schema(description = "Data e hora exata da ingestão (opcional, padrão é o instante atual)", example = "2026-10-01T08:15:00Z")
        Instant dataHoraIngestao,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Data de referência da dose diária (opcional, calculada a partir da ingestão)", example = "2026-10-01")
        LocalDate dataReferencia,

        @Schema(description = "Observações registradas pelo cuidador ou idoso", example = "Ingerido com copo de água após café da manhã.")
        String observacao,

        @Schema(description = "Status forçado caso o medicamento tenha sido pulado (opcional: IGNORADO). Se omitido, o sistema valida a janela de 30 minutos.", example = "TOMADO")
        StatusIngestao status
) {
}
