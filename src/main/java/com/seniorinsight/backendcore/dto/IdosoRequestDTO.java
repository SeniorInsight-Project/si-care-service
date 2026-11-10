package com.seniorinsight.backendcore.dto;

import java.time.LocalDate;

import org.hibernate.validator.constraints.URL;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/v1/idosos} e {@code PUT /api/v1/idosos/{idosoId}} (schema {@code IdosoRequest} do care-v1). */
@Schema(description = "Dados do cadastro do idoso")
public record IdosoRequestDTO(

        @Schema(example = "Maria Aparecida da Silva")
        @NotBlank(message = "O nome completo é obrigatório.")
        @Size(max = 255, message = "O nome completo deve ter no máximo 255 caracteres.")
        String nomeCompleto,

        @Schema(example = "1945-03-12")
        @NotNull(message = "A data de nascimento é obrigatória.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate dataNascimento,

        @Size(max = 20, message = "O gênero deve ter no máximo 20 caracteres.")
        String genero,

        @Schema(example = "123.456.789-00")
        @Size(max = 14, message = "O CPF deve ter no máximo 14 caracteres.")
        String cpf,

        @URL(message = "A URL da foto de perfil é inválida.")
        @Size(max = 500, message = "A URL da foto de perfil deve ter no máximo 500 caracteres.")
        String fotoPerfilUrl,

        @Schema(example = "O+")
        @Size(max = 5, message = "O tipo sanguíneo deve ter no máximo 5 caracteres.")
        String tipoSanguineo,

        String alergias,

        String logradouro,

        @Size(max = 20, message = "O número deve ter no máximo 20 caracteres.")
        String numero,

        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres.")
        String bairro,

        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
        String cidade,

        @Size(max = 50, message = "O estado deve ter no máximo 50 caracteres.")
        String estado,

        @Size(max = 20, message = "O CEP deve ter no máximo 20 caracteres.")
        String cep,

        @Size(max = 100, message = "O plano de saúde deve ter no máximo 100 caracteres.")
        String planoSaude,

        @Schema(description = "Orientações para quem prestar socorro")
        String observacoesSocorros
) {
}
