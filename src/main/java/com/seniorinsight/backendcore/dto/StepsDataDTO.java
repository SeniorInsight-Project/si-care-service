package com.seniorinsight.backendcore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * Payload JSON para dados de contagem de passos e atividade física padronizado no esquema OpenWearables.
 */
public record StepsDataDTO(

        @Min(value = 0, message = "A contagem de passos não pode ser negativa.")
        @Schema(description = "Total de passos contabilizados na janela/dia", example = "4520")
        @JsonProperty("count")
        Integer count,

        @Min(value = 0, message = "A distância percorrida não pode ser negativa.")
        @Schema(description = "Distância estimada percorrida em metros", example = "3150.5")
        @JsonProperty("distance_meters")
        Double distanceMeters,

        @Min(value = 0, message = "As calorias queimadas não podem ser negativas.")
        @Schema(description = "Estimativa de calorias ativas gastas (kcal)", example = "185.0")
        @JsonProperty("calories_kcal")
        Double caloriesKcal
) {
}
