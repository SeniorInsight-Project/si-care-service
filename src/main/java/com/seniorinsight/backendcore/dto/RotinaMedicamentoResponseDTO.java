package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seniorinsight.backendcore.domain.StatusIngestao;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para a Rotina de Medicamentos (HU26).
 * Estruturado com atributos planos e status diário em tempo real para consumo mobile direto no React Native.
 */
public record RotinaMedicamentoResponseDTO(

        @Schema(description = "ID único da rotina de medicamento", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "ID do idoso vinculado", example = "58066f27-6f8e-4a6c-9419-f90b1c09b83b")
        UUID idosoId,

        @Schema(description = "Nome completo do idoso", example = "Sebastião Ferreira")
        String idosoNome,

        @Schema(description = "ID do medicamento", example = "7c12f458-9584-482c-b5eb-a169b1df0a50")
        UUID medicamentoId,

        @Schema(description = "Nome do medicamento", example = "Losartana Potássica")
        String medicamentoNome,

        @Schema(description = "Dosagem prescrita", example = "50mg")
        String dosagem,

        @Schema(description = "Instruções de administração", example = "Ingerir 1 comprimido pela manhã em jejum com água.")
        String instrucoes,

        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "Horário diário programado (HH:mm)", type = "string", example = "08:00")
        LocalTime horario,

        @Schema(description = "Frequência programada", example = "1x ao dia (Diário)")
        String frequencia,

        @Schema(description = "Dias da semana da medicação", example = "TODOS")
        String diasSemana,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Data de início do tratamento", example = "2026-10-01")
        LocalDate dataInicio,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Data de término do tratamento (nulo se contínuo)", example = "2026-12-31")
        LocalDate dataFim,

        @Schema(description = "Status ativo da rotina", example = "true")
        Boolean ativo,

        @Schema(description = "Observações gerais", example = "Verificar pressão arterial antes de administrar.")
        String observacoes,

        @Schema(description = "Status de ingestão do dia consultado (PENDENTE, TOMADO, ATRASADO, IGNORADO)", example = "PENDENTE")
        StatusIngestao statusHoje,

        @Schema(description = "Data e hora exata da ingestão registrada no dia consultado", example = "2026-10-01T08:12:00Z")
        Instant horarioIngestaoHoje,

        @Schema(description = "Data e hora do último registro de ingestão", example = "2026-10-01T08:12:00Z")
        Instant ultimaIngestao,

        @Schema(description = "Data e hora de criação da rotina", example = "2026-10-01T07:00:00Z")
        Instant criadoEm,

        @Schema(description = "Data e hora da última atualização da rotina", example = "2026-10-01T07:00:00Z")
        Instant atualizadoEm
) {
}
