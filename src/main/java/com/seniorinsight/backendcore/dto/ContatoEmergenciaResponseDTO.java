package com.seniorinsight.backendcore.dto;

import java.util.UUID;

import com.seniorinsight.backendcore.domain.ContatoEmergencia;

import io.swagger.v3.oas.annotations.media.Schema;

/** Schema {@code ContatoEmergencia} do care-v1. O telefone sai em E.164. */
@Schema(description = "Contato de emergência do idoso")
public record ContatoEmergenciaResponseDTO(
        UUID id,
        UUID idosoId,
        String nome,
        @Schema(example = "+5511987654321") String telefone,
        String grauParentesco,
        int prioridade
) {
    public static ContatoEmergenciaResponseDTO de(ContatoEmergencia c) {
        return new ContatoEmergenciaResponseDTO(c.getId(), c.getIdosoId(), c.getNome(), c.getTelefone(),
                c.getGrauParentesco(), c.getPrioridade());
    }
}
