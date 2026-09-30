package com.seniorinsight.backendcore.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.HistoricoMedicacao;

/**
 * Repositório de dados para Histórico de Medicação (HU26).
 */
@Repository
public interface HistoricoMedicacaoRepository extends JpaRepository<HistoricoMedicacao, UUID> {

    List<HistoricoMedicacao> findByRotinaIdOrderByDataHoraIngestaoDesc(UUID rotinaId);

    List<HistoricoMedicacao> findByRotinaIdAndDataHoraIngestaoBetweenOrderByDataHoraIngestaoDesc(
            UUID rotinaId, Instant inicio, Instant fim);

    Optional<HistoricoMedicacao> findTopByRotinaIdOrderByDataHoraIngestaoDesc(UUID rotinaId);

    @Query("SELECT h FROM HistoricoMedicacao h WHERE h.rotina.id IN :rotinaIds AND h.dataHoraIngestao >= :inicio AND h.dataHoraIngestao < :fim ORDER BY h.dataHoraIngestao DESC")
    List<HistoricoMedicacao> findByRotinaIdsAndDataRange(
            @Param("rotinaIds") List<UUID> rotinaIds,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim);

    @Query("SELECT h FROM HistoricoMedicacao h JOIN FETCH h.rotina r JOIN FETCH r.medicamento WHERE r.idoso.id = :idosoId ORDER BY h.dataHoraIngestao DESC")
    List<HistoricoMedicacao> findByIdosoIdWithDetails(@Param("idosoId") UUID idosoId);
}
