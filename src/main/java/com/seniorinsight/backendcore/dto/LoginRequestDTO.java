package com.seniorinsight.backendcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para autenticação tradicional com e-mail e senha (HU-002)")
public record LoginRequestDTO(
        @Schema(description = "E-mail cadastrado do usuário", example = "maria.silva@email.com")
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        String email,

        @Schema(description = "Senha do usuário", example = "SenhaForte@123")
        @NotBlank(message = "A senha é obrigatória.")
        String senha
) {
}
