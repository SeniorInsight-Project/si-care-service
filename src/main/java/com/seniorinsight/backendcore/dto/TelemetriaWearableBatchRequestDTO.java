package com.seniorinsight.backendcore.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Payload JSON para ingestão em lote de telemetrias (Batch Ingestion).
 * Permite que o aplicativo React Native sincronize dados acumulados durante momentos offline.
 */
public record TelemetriaWearableBatchRequestDTO(

        @NotEmpty(message = "O lote de telemetrias não pode estar vazio.")
        @Valid
        @Schema(description = "Lista de leituras coletadas pelo wearable")
        @JsonProperty("readings")
        List<TelemetriaWearableRequestDTO> readings
) {
}
