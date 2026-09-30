package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.backendcore.domain.AlarmeEmergencia;
import com.seniorinsight.backendcore.domain.StatusAlarme;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para operações com Alarmes de Emergência / Botão SOS (HU35).
 */
public record AlarmeEmergenciaResponseDTO(

        @Schema(description = "Identificador único do alarme de emergência", example = "d9b2d6a4-65f3-4d8b-967b-23a9d8f61234")
        UUID id,

        @Schema(description = "Identificador único do idoso atendido", example = "e8c3b2a1-1234-5678-9abc-def012345678")
        UUID idosoId,

        @Schema(description = "Nome completo do idoso monitorado", example = "Sebastião da Silva")
        String nomeIdoso,

        @Schema(description = "Latitude da localização do disparo", example = "-23.550520")
        Double latitude,

        @Schema(description = "Longitude da localização do disparo", example = "-46.633308")
        Double longitude,

        @Schema(description = "Nível de bateria do dispositivo no momento do disparo (0 a 100%)", example = "85")
        Integer nivelBateria,

        @Schema(description = "Status atual do alarme de emergência", example = "DISPARADO")
        StatusAlarme status,

        @Schema(description = "Observações ou justificativas de atendimento/cancelamento", example = "Alarme acionado via botão SOS móvel.")
        String observacao,

        @Schema(description = "Data e hora de registro e disparo do alarme", example = "2026-09-30T18:00:00Z")
        Instant criadoEm,

        @Schema(description = "Data e hora da última atualização de status do alarme", example = "2026-09-30T18:00:00Z")
        Instant atualizadoEm,

        @Schema(description = "Mensagem descritiva de feedback da operação", example = "Alarme SOS disparado com sucesso. Notificações sendo processadas assincronamente.")
        String mensagem
) {

    public static AlarmeEmergenciaResponseDTO fromEntity(AlarmeEmergencia alarme, String mensagem) {
        String nome = (alarme.getIdoso() != null) ? alarme.getIdoso().getNomeCompleto() : null;
        UUID idosoId = (alarme.getIdoso() != null) ? alarme.getIdoso().getId() : null;

        return new AlarmeEmergenciaResponseDTO(
                alarme.getId(),
                idosoId,
                nome,
                alarme.getLatitude(),
                alarme.getLongitude(),
                alarme.getNivelBateria(),
                alarme.getStatus(),
                alarme.getObservacao(),
                alarme.getCriadoEm(),
                alarme.getAtualizadoEm(),
                mensagem
        );
    }
}
