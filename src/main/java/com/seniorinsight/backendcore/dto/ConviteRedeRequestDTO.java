package com.seniorinsight.backendcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitação de geração de convite para a Rede de Proteção (HU16).
 */
@Schema(description = "Dados para geração de convite para a Rede de Proteção do Idoso (HU16)")
public record ConviteRedeRequestDTO(

        @Schema(description = "E-mail do familiar a ser convidado", example = "maria.silva@email.com")
        @NotBlank(message = "O e-mail do destinatário é obrigatório.")
        @Email(message = "O e-mail informado deve possuir um formato válido.")
        String email,

        @Schema(description = "Papel de acesso sugerido para o membro convidado", example = "FAMILIAR")
        @NotBlank(message = "O papel de acesso sugerido é obrigatório.")
        @Size(max = 50, message = "O papel de acesso deve ter no máximo 50 caracteres.")
        String papelAcesso,

        @Schema(description = "Grau de parentesco sugerido", example = "Filha")
        @Size(max = 50, message = "O grau de parentesco deve ter no máximo 50 caracteres.")
        String grauParentesco,

        @Schema(description = "Mensagem opcional de apresentação no e-mail de convite", example = "Olá, junte-se à rede de cuidados do papai.")
        @Size(max = 500, message = "A mensagem personalizada deve ter no máximo 500 caracteres.")
        String mensagemPersonalizada
) {
}
