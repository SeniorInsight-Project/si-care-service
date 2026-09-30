package com.seniorinsight.backendcore.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para criação da agenda de um medicamento na rotina do idoso (HU26).
 * Otimizado para formulários React Native com suporte a medicamentos novos ou existentes.
 */
public record RotinaMedicamentoRequestDTO(

        @Schema(description = "ID de medicamento já cadastrado (opcional caso informe nome e dosagem)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID medicamentoId,

        @Schema(description = "Nome comercial ou princípio ativo do medicamento", example = "Losartana Potássica")
        String nomeMedicamento,

        @Schema(description = "Dosagem prescrita do medicamento", example = "50mg")
        String dosagem,

        @Schema(description = "Instruções clínicas de administração", example = "Ingerir 1 comprimido pela manhã em jejum com água.")
        String instrucoes,

        @NotNull(message = "O horário programado para a medicação é obrigatório.")
        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "Horário diário programado (HH:mm)", type = "string", example = "08:00")
        LocalTime horario,

        @NotBlank(message = "A frequência de administração é obrigatória.")
        @Schema(description = "Frequência de administração", example = "1x ao dia (Diário)")
        String frequencia,

        @Schema(description = "Dias da semana aplicáveis", example = "TODOS")
        String diasSemana,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Data de início do tratamento (padrão hoje se omitido)", example = "2026-10-01")
        LocalDate dataInicio,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Data de término do tratamento (nulo para uso contínuo)", example = "2026-12-31")
        LocalDate dataFim,

        @Schema(description = "Observações gerais para o idoso ou cuidador", example = "Verificar pressão arterial antes de administrar.")
        String observacoes
) {
}
