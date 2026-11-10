package com.seniorinsight.backendcore.dto;

import com.seniorinsight.backendcore.validation.TelefoneValido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST} e {@code PUT} de contatos de emergência (schema {@code ContatoEmergenciaRequest}). */
@Schema(description = "Dados do contato de emergência")
public record ContatoEmergenciaRequestDTO(

        @Schema(example = "João da Silva")
        @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres.")
        String nome,

        @Schema(example = "(11) 98765-4321")
        @NotBlank(message = "O telefone é obrigatório.")
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        @TelefoneValido
        String telefone,

        @Schema(example = "Filho", nullable = true)
        @Size(max = 50, message = "O grau de parentesco deve ter no máximo 50 caracteres.")
        String grauParentesco,

        @Schema(description = "Ordem de acionamento (1 = primeiro a ser chamado). Sem valor, entra no fim da fila.",
                example = "1")
        @Min(value = 1, message = "A prioridade deve ser no mínimo 1.")
        Integer prioridade
) {
}
