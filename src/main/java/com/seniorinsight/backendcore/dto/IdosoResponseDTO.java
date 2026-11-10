package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.seniorinsight.backendcore.domain.Idoso;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta das rotas de idosos (schema {@code Idoso} do care-v1). {@code avisos} traz alertas que não bloqueiam
 * a operação, como o de idade abaixo de 49 anos.
 */
@Schema(description = "Cadastro do idoso")
public record IdosoResponseDTO(
        UUID id,
        String nomeCompleto,
        LocalDate dataNascimento,
        String genero,
        String cpf,
        String fotoPerfilUrl,
        String tipoSanguineo,
        String alergias,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        String planoSaude,
        String observacoesSocorros,
        List<String> comorbidades,
        Map<String, Object> limitesBasais,
        Instant criadoEm,
        Instant atualizadoEm,
        @Schema(description = "Avisos que não bloqueiam a operação (ex.: idade abaixo de 49 anos)")
        List<String> avisos
) {

    public static IdosoResponseDTO de(Idoso i, List<String> avisos) {
        return new IdosoResponseDTO(
                i.getId(), i.getNomeCompleto(), i.getDataNascimento(), i.getGenero(), i.getCpf(),
                i.getFotoPerfilUrl(), i.getTipoSanguineo(), i.getAlergias(), i.getLogradouro(), i.getNumero(),
                i.getBairro(), i.getCidade(), i.getEstado(), i.getCep(), i.getPlanoSaude(),
                i.getObservacoesSocorros(),
                i.getComorbidades() != null ? List.copyOf(i.getComorbidades()) : List.of(),
                i.getLimitesBasais() != null ? Map.copyOf(i.getLimitesBasais()) : Map.of(),
                i.getCriadoEm(), i.getAtualizadoEm(), avisos);
    }
}
