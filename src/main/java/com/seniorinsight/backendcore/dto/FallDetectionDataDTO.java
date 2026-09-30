package com.seniorinsight.backendcore.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload JSON para dados de detecção de queda capturados por sensores inerciais (IMU) do smartwatch.
 */
public record FallDetectionDataDTO(

        @Schema(description = "Indica se o algoritmo embarcado no wearable confirmou uma queda brusca", example = "true")
        @JsonProperty("fall_detected")
        Boolean fallDetected,

        @Schema(description = "Aceleração máxima do impacto em múltiplos da gravidade (g-force)", example = "3.8")
        @JsonProperty("impact_force_g")
        Double impactForceG,

        @Schema(description = "Índice de confiança ou probabilidade da detecção de queda (0.0 a 1.0)", example = "0.92")
        @JsonProperty("confidence")
        Double confidence,

        @Schema(description = "Instante exato registrado do impacto de queda", example = "2026-09-30T18:30:00Z")
        @JsonProperty("fall_timestamp")
        Instant fallTimestamp
) {
}
