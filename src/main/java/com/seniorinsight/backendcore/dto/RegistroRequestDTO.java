package com.seniorinsight.backendcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro de novo usuário e perfil inicial (HU-002)")
public record RegistroRequestDTO(
        @Schema(description = "E-mail para autenticação", example = "joao.silva@email.com")
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        String email,

        @Schema(description = "Senha de acesso (mínimo 6 caracteres)", example = "SenhaForte@123")
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, message = "A senha deve conter no mínimo 6 caracteres.")
        String senha,

        @Schema(description = "Nome completo do usuário", example = "João da Silva")
        @NotBlank(message = "O nome completo é obrigatório.")
        String nomeCompleto,

        @Schema(description = "CPF do usuário (obrigatório para Familiar)", example = "123.456.789-00")
        String cpf,

        @Schema(description = "Telefone para contato ou consultório", example = "(11) 98765-4321")
        String telefone,

        @Schema(description = "Tipo de perfil inicial: FAMILIAR ou MEDICO", example = "FAMILIAR")
        @NotBlank(message = "O tipo de perfil é obrigatório (FAMILIAR ou MEDICO).")
        String tipoPerfil,

        @Schema(description = "Número do CRM (obrigatório se tipoPerfil = MEDICO)", example = "123456/SP")
        String crm,

        @Schema(description = "Especialidade médica (obrigatório se tipoPerfil = MEDICO)", example = "Geriatria")
        String especialidade
) {
}
