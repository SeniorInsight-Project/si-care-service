package com.seniorinsight.backendcore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Payload JSON para dados de frequência cardíaca padronizado no esquema OpenWearables.
 */
public record HeartRateDataDTO(

        @Min(value = 30, message = "A frequência cardíaca deve ser de no mínimo 30 BPM.")
        @Max(value = 260, message = "A frequência cardíaca deve ser de no máximo 260 BPM.")
        @Schema(description = "Batimentos cardíacos por minuto (BPM)", example = "78")
        @JsonProperty("bpm")
        Integer bpm,

        @Min(value = 30, message = "A frequência cardíaca de repouso deve ser de no mínimo 30 BPM.")
        @Max(value = 200, message = "A frequência cardíaca de repouso deve ser de no máximo 200 BPM.")
        @Schema(description = "Frequência cardíaca em repouso (BPM)", example = "64")
        @JsonProperty("resting_bpm")
        Integer restingBpm,

        @Schema(description = "Variabilidade da frequência cardíaca (HRV) em milissegundos", example = "42.5")
        @JsonProperty("hrv_ms")
        Double hrvMs
) {
}
