package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO amigável para representação de um membro vinculado à Rede de Proteção do Idoso (HU16).
 * Converte os dados internos da entidade VinculoRede em um formato claro para a aplicação cliente.
 */
@Schema(description = "Dados amigáveis de um membro vinculado à Rede de Proteção do Idoso (HU16)")
public record MembroRedeResponseDTO(

        @Schema(description = "Identificador único do vínculo na rede")
        UUID vinculoId,

        @Schema(description = "Identificador único do perfil de familiar")
        UUID familiarId,

        @Schema(description = "Identificador do usuário no Keycloak (claim sub)")
        String keycloakSub,

        @Schema(description = "Nome completo do familiar")
        String nomeCompleto,

        @Schema(description = "E-mail de acesso do familiar")
        String email,

        @Schema(description = "Telefone para contato do familiar")
        String telefone,

        @Schema(description = "Grau de parentesco com o idoso", example = "Filho(a)")
        String grauParentesco,

        @Schema(description = "Papel de acesso atribuído na rede", example = "CUIDADOR_PRINCIPAL")
        String papelAcesso,

        @Schema(description = "Restrições de acesso configuradas em formato JSON")
        Map<String, Object> restricoesAcesso,

        @Schema(description = "Data e hora em que o vínculo foi estabelecido")
        Instant vinculadoEm
) {
}
