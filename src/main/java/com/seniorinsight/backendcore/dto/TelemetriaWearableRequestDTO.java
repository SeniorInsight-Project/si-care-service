package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Payload JSON unificado de telemetria emitido pelo App React Native Expo
 * normalizado conforme as especificações do openwearables.io.
 */
public record TelemetriaWearableRequestDTO(

        @Schema(description = "Identificador do idoso monitorado (opcional se fornecido na URL)", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        @JsonProperty("idoso_id")
        UUID idosoId,

        @Schema(description = "Identificador exclusivo ou MAC do smartwatch", example = "redmi-watch-4-01fa")
        @JsonProperty("device_id")
        String deviceId,

        @Schema(description = "Modelo do wearable conectado", example = "Xiaomi Redmi Watch 4")
        @JsonProperty("device_model")
        String deviceModel,

        @Schema(description = "Provedor/origem dos dados da telemetria", example = "XIAOMI_HEALTH")
        @JsonProperty("provider")
        String provider,

        @Schema(description = "Data e hora da captura dos dados no dispositivo (ISO-8601 UTC)", example = "2026-09-30T21:30:00Z")
        @JsonProperty("timestamp")
        Instant timestamp,

        @Valid
        @Schema(description = "Métricas cardiovasculares capturadas pelo sensor óptico PPG")
        @JsonProperty("heart_rate")
        HeartRateDataDTO heartRate,

        @Valid
        @Schema(description = "Métricas de passos e locomoção capturadas pelo pedômetro")
        @JsonProperty("steps")
        StepsDataDTO steps,

        @Valid
        @Schema(description = "Dados do algoritmo de detecção de impacto e queda")
        @JsonProperty("fall_detection")
        FallDetectionDataDTO fallDetection,

        @Min(value = 0, message = "O nível de bateria não pode ser inferior a 0%.")
        @Max(value = 100, message = "O nível de bateria não pode ser superior a 100%.")
        @Schema(description = "Nível de bateria do smartwatch (0 a 100%)", example = "85")
        @JsonProperty("battery_level")
        Integer batteryLevel,

        @Min(value = 50, message = "A saturação de O2 deve ser de no mínimo 50%.")
        @Max(value = 100, message = "A saturação de O2 deve ser de no máximo 100%.")
        @Schema(description = "Percentual de saturação de oxigênio no sangue (SpO2)", example = "98.5")
        @JsonProperty("spo2_percentage")
        Double spo2Percentage,

        @Schema(description = "Metadados adicionais e dados brutos de sensores em formato chave/valor")
        @JsonProperty("metadata")
        Map<String, Object> metadata
) {
}
