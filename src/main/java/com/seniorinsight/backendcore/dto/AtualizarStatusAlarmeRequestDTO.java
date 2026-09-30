package com.seniorinsight.backendcore.dto;

import com.seniorinsight.backendcore.domain.StatusAlarme;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requisição para atualização do status de um Alarme de Emergência (HU35).
 */
public record AtualizarStatusAlarmeRequestDTO(

        @NotNull(message = "O novo status do alarme é obrigatório.")
        @Schema(description = "Novo status do alarme de emergência", example = "EM_ATENDIMENTO")
        StatusAlarme status,

        @Schema(description = "Observação, notas de atendimento ou justificativa da alteração de status", example = "Equipe do SAMU acionada e a caminho da residência.")
        String observacao
) {
}
