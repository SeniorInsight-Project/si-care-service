package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload JSON dedicado para ingestão de passos e locomoção diária pelo aplicativo mobile.
 */
public record PassosRequestDTO(

        @Schema(description = "Identificador do idoso monitorado (opcional se fornecido na URL)", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        @JsonProperty("idoso_id")
        UUID idosoId,

        @NotNull(message = "A contagem de passos é obrigatória.")
        @Min(value = 0, message = "A contagem de passos não pode ser negativa.")
        @Schema(description = "Total de passos acumulados", example = "5230")
        @JsonProperty("count")
        Integer count,

        @Min(value = 0, message = "A distância em metros não pode ser negativa.")
        @Schema(description = "Distância percorrida em metros", example = "3600.0")
        @JsonProperty("distance_meters")
        Double distanceMeters,

        @Min(value = 0, message = "As calorias não podem ser negativas.")
        @Schema(description = "Calorias queimadas em kcal", example = "210.5")
        @JsonProperty("calories_kcal")
        Double caloriesKcal,

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
