package com.seniorinsight.backendcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * DTO de requisição para acionamento do Botão de Alarme / SOS de Emergência (HU35).
 * Fornece dados geográficos e de telemetria do dispositivo no momento do disparo.
 */
public record DispararSosRequestDTO(

        @DecimalMin(value = "-90.0", message = "A latitude deve ser maior ou igual a -90.0.")
        @DecimalMax(value = "90.0", message = "A latitude deve ser menor ou igual a 90.0.")
        @Schema(description = "Latitude da coordenada GPS no momento do disparo", example = "-23.550520")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "A longitude deve ser maior ou igual a -180.0.")
        @DecimalMax(value = "180.0", message = "A longitude deve ser menor ou igual a 180.0.")
        @Schema(description = "Longitude da coordenada GPS no momento do disparo", example = "-46.633308")
        Double longitude,

        @Min(value = 0, message = "O nível de bateria não pode ser inferior a 0%.")
        @Max(value = 100, message = "O nível de bateria não pode ser superior a 100%.")
        @Schema(description = "Nível percentual de bateria do dispositivo emissor (0 a 100)", example = "85")
        Integer nivelBateria,

        @Schema(description = "Observações adicionais ou contexto informado no disparo", example = "Idoso relatou tontura severa antes de acionar.")
        String observacao
) {
}
