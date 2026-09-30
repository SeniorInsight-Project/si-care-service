package com.seniorinsight.backendcore.dto;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.backendcore.domain.StatusIngestao;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para confirmação de ingestão e histórico de medicação (HU26).
 * Inclui validação detalhada da janela de tolerância de 30 minutos e feedback amigável.
 */
public record HistoricoMedicacaoResponseDTO(

        @Schema(description = "ID único do registro de histórico", example = "4ea5572b-8b9a-4f51-b072-bc238515c0e1")
        UUID id,

        @Schema(description = "ID da rotina de medicamento associada", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID rotinaId,

        @Schema(description = "Nome do medicamento", example = "Losartana Potássica")
        String medicamentoNome,

        @Schema(description = "Dosagem do medicamento", example = "50mg")
        String dosagem,

        @Schema(description = "Status resultante da ingestão (TOMADO, ATRASADO, IGNORADO)", example = "TOMADO")
        StatusIngestao status,

        @Schema(description = "Data e hora exata em que o medicamento foi ingerido/registrado", example = "2026-10-01T08:15:00Z")
        Instant dataHoraIngestao,

        @Schema(description = "Data e hora prevista da dose para validação de tolerância", example = "2026-10-01T08:00:00Z")
        Instant dataHoraPrevista,

        @Schema(description = "Indica se a confirmação ocorreu dentro da tolerância clínica de 30 minutos", example = "true")
        Boolean dentroJanelaTolerancia,

        @Schema(description = "Diferença em minutos em relação ao horário previsto (+ para atraso, - para antecipação)", example = "15")
        Long diferencaMinutos,

        @Schema(description = "Observações registradas", example = "Ingerido com copo de água após refeição.")
        String observacao,

        @Schema(description = "E-mail ou identificador do usuário que registrou a ingestão", example = "cuidador@seniorinsight.com")
        String registradoPor,

        @Schema(description = "Mensagem explicativa sobre o status da ingestão", example = "Ingestão confirmada com sucesso dentro da janela de tolerância de 30 minutos.")
        String mensagem,

        @Schema(description = "Timestamp do registro no sistema", example = "2026-10-01T08:15:05Z")
        Instant criadoEm
) {
}
