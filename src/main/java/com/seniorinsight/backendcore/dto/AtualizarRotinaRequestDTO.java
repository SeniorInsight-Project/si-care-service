package com.seniorinsight.backendcore.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para atualização de dados de uma rotina de medicamento existente (HU26).
 */
public record AtualizarRotinaRequestDTO(

        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "Horário diário programado (HH:mm)", type = "string", example = "09:00")
        LocalTime horario,

        @Schema(description = "Frequência de administração", example = "1x ao dia")
        String frequencia,

        @Schema(description = "Dias da semana aplicáveis", example = "TODOS")
        String diasSemana,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Data de início do tratamento", example = "2026-10-01")
        LocalDate dataInicio,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Data de término do tratamento", example = "2026-12-31")
        LocalDate dataFim,

        @Schema(description = "Define se a rotina está ativa ou suspensa", example = "true")
        Boolean ativo,

        @Schema(description = "Observações gerais sobre a rotina", example = "Dose ajustada pelo cardiologista.")
        String observacoes,

        @Schema(description = "Dosagem atualizada do medicamento", example = "100mg")
        String dosagem,

        @Schema(description = "Instruções atualizadas de administração", example = "Tomar junto com o café da manhã.")
        String instrucoes
) {
}
