package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Payload JSON para evento crítico de detecção de queda gerado pelo smartwatch.
 */
public record DeteccaoQuedaRequestDTO(

        @Schema(description = "Identificador do idoso monitorado (opcional se fornecido na URL)", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        @JsonProperty("idoso_id")
        UUID idosoId,

        @NotNull(message = "O indicador de queda é obrigatório.")
        @Schema(description = "Confirmação de queda pelos sensores inerciais", example = "true")
        @JsonProperty("fall_detected")
        Boolean fallDetected,

        @Schema(description = "Aceleração máxima do impacto em força G", example = "4.2")
        @JsonProperty("impact_force_g")
        Double impactForceG,

        @Schema(description = "Nível de confiança da detecção (0.0 a 1.0)", example = "0.95")
        @JsonProperty("confidence")
        Double confidence,

        @Schema(description = "Instante da queda detectada", example = "2026-09-30T18:30:00Z")
        @JsonProperty("timestamp")
        Instant timestamp,

        @DecimalMin(value = "-90.0", message = "A latitude deve ser maior ou igual a -90.0.")
        @DecimalMax(value = "90.0", message = "A latitude deve ser menor ou igual a 90.0.")
        @Schema(description = "Latitude GPS do evento", example = "-23.550520")
        @JsonProperty("latitude")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "A longitude deve ser maior ou igual a -180.0.")
        @DecimalMax(value = "180.0", message = "A longitude deve ser menor ou igual a 180.0.")
        @Schema(description = "Longitude GPS do evento", example = "-46.633308")
        @JsonProperty("longitude")
        Double longitude,

        @Schema(description = "Nível de bateria do smartwatch", example = "78")
        @JsonProperty("battery_level")
        Integer batteryLevel,

        @Schema(description = "Identificador do dispositivo", example = "redmi-watch-4-01fa")
        @JsonProperty("device_id")
        String deviceId,

        @Schema(description = "Modelo do relógio", example = "Xiaomi Redmi Watch 4")
        @JsonProperty("device_model")
        String deviceModel,

        @Schema(description = "Observações adicionais ou notas de emergência", example = "Queda detectada sem movimento subsequente por 30s")
        @JsonProperty("observacao")
        String observacao
) {
}
