package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO com o resultado da operação de aceite de convite da Rede de Proteção (HU16).
 */
@Schema(description = "Confirmação do aceite de convite e criação do vínculo na Rede de Proteção (HU16)")
public record AceiteConviteResponseDTO(

        @Schema(description = "Identificador único do vínculo criado na tabela vinculos_rede")
        UUID vinculoId,

        @Schema(description = "Identificador único do idoso")
        UUID idosoId,

        @Schema(description = "Nome do idoso monitorado")
        String nomeIdoso,

        @Schema(description = "Identificador do perfil do familiar vinculado")
        UUID familiarId,

        @Schema(description = "Nome do familiar vinculado")
        String nomeFamiliar,

        @Schema(description = "Grau de parentesco estabelecido")
        String grauParentesco,

        @Schema(description = "Papel de acesso atribuído")
        String papelAcesso,

        @Schema(description = "Data e hora de ativação do vínculo")
        Instant criadoEm,

        @Schema(description = "Mensagem explicativa sobre a conclusão do processo")
        String mensagem
) {
}
