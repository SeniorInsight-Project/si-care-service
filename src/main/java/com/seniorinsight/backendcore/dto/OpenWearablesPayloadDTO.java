package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload estruturado conforme a API do OpenWearables (openwearables.io)
 * para sincronização ou despacho em nuvem via RestTemplate ou WebClient.
 */
public record OpenWearablesPayloadDTO(

        @Schema(description = "Identificador de usuário no OpenWearables", example = "user_idoso_123")
        @JsonProperty("user_id")
        String userId,

        @Schema(description = "Identificador exclusivo do wearable", example = "redmi-watch-4-01fa")
        @JsonProperty("device_id")
        String deviceId,

        @Schema(description = "Modelo do wearable", example = "Xiaomi Redmi Watch 4")
        @JsonProperty("device_model")
        String deviceModel,

        @Schema(description = "Provedor da fonte dos dados", example = "openwearables_unified")
        @JsonProperty("provider")
        String provider,

        @Schema(description = "Tipo de telemetria", example = "unified_telemetry")
        @JsonProperty("data_type")
        String dataType,

        @Schema(description = "Instante UTC da telemetria", example = "2026-09-30T18:30:00Z")
        @JsonProperty("timestamp")
        Instant timestamp,

        @Schema(description = "Dados de frequência cardíaca")
        @JsonProperty("heart_rate")
        HeartRateDataDTO heartRate,

        @Schema(description = "Dados de passos e atividade")
        @JsonProperty("steps")
        StepsDataDTO steps,

        @Schema(description = "Dados de detecção de impacto/queda")
        @JsonProperty("fall_detection")
        FallDetectionDataDTO fallDetection,

        @Schema(description = "Nível de bateria do smartwatch (0-100)", example = "85")
        @JsonProperty("battery_level")
        Integer batteryLevel,

        @Schema(description = "Saturação de oxigênio SpO2 (%)", example = "98.0")
        @JsonProperty("spo2_percentage")
        Double spo2Percentage,

        @Schema(description = "Metadados adicionais do dispositivo ou sessão")
        @JsonProperty("metadata")
        Map<String, Object> metadata
) {
}
