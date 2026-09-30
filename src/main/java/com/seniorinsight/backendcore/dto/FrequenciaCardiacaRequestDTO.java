package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload JSON dedicado para ingestão rápida de frequência cardíaca pelo aplicativo mobile.
 */
public record FrequenciaCardiacaRequestDTO(

        @Schema(description = "Identificador do idoso monitorado (opcional se fornecido na URL)", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        @JsonProperty("idoso_id")
        UUID idosoId,

        @NotNull(message = "A frequência cardíaca (BPM) é obrigatória.")
        @Min(value = 30, message = "A frequência cardíaca deve ser de no mínimo 30 BPM.")
        @Max(value = 260, message = "A frequência cardíaca deve ser de no máximo 260 BPM.")
        @Schema(description = "Batimentos por minuto (BPM)", example = "75")
        @JsonProperty("bpm")
        Integer bpm,

        @Schema(description = "Frequência cardíaca de repouso (BPM)", example = "62")
        @JsonProperty("resting_bpm")
        Integer restingBpm,

        @Schema(description = "Variabilidade da frequência cardíaca (HRV) em milissegundos", example = "45.0")
        @JsonProperty("hrv_ms")
        Double hrvMs,

        @Schema(description = "Instante da medição (ISO-8601 UTC)", example = "2026-09-30T18:30:00Z")
        @JsonProperty("timestamp")
        Instant timestamp,

        @Schema(description = "Identificador do dispositivo emissor", example = "redmi-watch-4-01fa")
        @JsonProperty("device_id")
        String deviceId,

        @Schema(description = "Modelo do smartwatch", example = "Xiaomi Redmi Watch 4")
        @JsonProperty("device_model")
        String deviceModel
) {
}
