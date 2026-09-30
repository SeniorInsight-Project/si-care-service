package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seniorinsight.backendcore.domain.TelemetriaWearable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta com os dados consolidados da telemetria persistida no backend.
 */
public record TelemetriaWearableResponseDTO(

        @Schema(description = "Identificador único do registro de telemetria", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identificador do idoso monitorado", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        @JsonProperty("idoso_id")
        UUID idosoId,

        @Schema(description = "Identificador do wearable", example = "redmi-watch-4-01fa")
        @JsonProperty("device_id")
        String deviceId,

        @Schema(description = "Modelo do smartwatch", example = "Xiaomi Redmi Watch 4")
        @JsonProperty("device_model")
        String deviceModel,

        @Schema(description = "Provedor dos dados", example = "XIAOMI_HEALTH")
        String provider,

        @Schema(description = "Batimentos por minuto (BPM)", example = "78")
        @JsonProperty("frequencia_cardiaca_bpm")
        Integer frequenciaCardiacaBpm,

        @Schema(description = "Frequência cardíaca em repouso (BPM)", example = "64")
        @JsonProperty("frequencia_cardiaca_repouso_bpm")
        Integer frequenciaCardiacaRepousoBpm,

        @Schema(description = "Variabilidade da frequência cardíaca em ms", example = "42.5")
        @JsonProperty("variabilidade_cardiaca_hrv")
        Double variabilidadeCardiacaHrv,

        @Schema(description = "Passos acumulados", example = "4520")
        @JsonProperty("passos_contagem")
        Integer passosContagem,

        @Schema(description = "Distância percorrida em metros", example = "3150.5")
        @JsonProperty("distancia_metros")
        Double distanciaMetros,

        @Schema(description = "Calorias queimadas em kcal", example = "185.0")
        @JsonProperty("calorias_kcal")
        Double caloriasKcal,

        @Schema(description = "Indica se houve queda detectada", example = "false")
        @JsonProperty("queda_detectada")
        Boolean quedaDetectada,

        @Schema(description = "Força G do impacto", example = "3.8")
        @JsonProperty("forca_impacto_g")
        Double forcaImpactoG,

        @Schema(description = "Grau de confiança da detecção de queda", example = "0.92")
        @JsonProperty("confianca_queda")
        Double confiancaQueda,

        @Schema(description = "Nível de bateria do smartwatch (0 a 100)", example = "85")
        @JsonProperty("nivel_bateria")
        Integer nivelBateria,

        @Schema(description = "Saturação de oxigênio (SpO2)", example = "98.5")
        @JsonProperty("saturacao_oxigenio")
        Double saturacaoOxigenio,

        @Schema(description = "Instante da coleta dos dados no relógio", example = "2026-09-30T18:30:00Z")
        @JsonProperty("coletado_em")
        Instant coletadoEm,

        @Schema(description = "Instante da persistência no backend", example = "2026-09-30T18:30:05Z")
        @JsonProperty("criado_em")
        Instant criadoEm,

        @Schema(description = "Status do processamento", example = "PROCESSADO_COM_SUCESSO")
        @JsonProperty("status_processamento")
        String statusProcessamento
) {
    public static TelemetriaWearableResponseDTO fromEntity(TelemetriaWearable entity, String statusProcessamento) {
        if (entity == null) {
            return null;
        }
        return new TelemetriaWearableResponseDTO(
                entity.getId(),
                entity.getIdoso() != null ? entity.getIdoso().getId() : null,
                entity.getDispositivoId(),
                entity.getModeloDispositivo(),
                entity.getProvedor(),
                entity.getFrequenciaCardiaca(),
                entity.getFrequenciaCardiacaRepouso(),
                entity.getVariabilidadeCardiaca(),
                entity.getPassos(),
                entity.getDistanciaMetros(),
                entity.getCaloriasQueimadas(),
                entity.getQuedaDetectada(),
                entity.getForcaImpactoG(),
                entity.getConfiancaQueda(),
                entity.getNivelBateria(),
                entity.getSaturacaoOxigenio(),
                entity.getColetadoEm(),
                entity.getCriadoEm(),
                statusProcessamento
        );
    }
}
