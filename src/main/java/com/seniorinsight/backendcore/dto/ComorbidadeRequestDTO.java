package com.seniorinsight.backendcore.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitação de cadastro/adição de comorbidades no perfil do idoso (HU15).
 * Permite o envio de uma ou mais comorbidades em uma única requisição.
 */
@Schema(description = "Objeto de requisição para adicionar comorbidades ao perfil do idoso")
public record ComorbidadeRequestDTO(

        @Schema(
                description = "Lista com uma ou mais comorbidades a serem adicionadas",
                example = "[\"Hipertensão Arterial Sistêmica\", \"Diabetes Mellitus Tipo 2\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotEmpty(message = "A lista de comorbidades não pode estar vazia.")
        List<@NotBlank(message = "O nome da comorbidade não pode ser vazio ou em branco.")
             @Size(min = 2, max = 150, message = "O nome da comorbidade deve ter entre 2 e 150 caracteres.")
             String> comorbidades
) {
}
