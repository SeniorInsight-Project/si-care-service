package com.seniorinsight.backendcore.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.RotinaMedicamento;

/**
 * Repositório de dados para Rotina de Medicamentos (HU26).
 */
@Repository
public interface RotinaMedicamentoRepository extends JpaRepository<RotinaMedicamento, UUID> {

    List<RotinaMedicamento> findByIdosoIdOrderByHorarioAsc(UUID idosoId);

    List<RotinaMedicamento> findByIdosoIdAndAtivoTrueOrderByHorarioAsc(UUID idosoId);

    @Query("SELECT r FROM RotinaMedicamento r JOIN FETCH r.medicamento JOIN FETCH r.idoso WHERE r.idoso.id = :idosoId AND (:ativo IS NULL OR r.ativo = :ativo) ORDER BY r.horario ASC")
    List<RotinaMedicamento> findByIdosoIdAndAtivoWithDetails(@Param("idosoId") UUID idosoId, @Param("ativo") Boolean ativo);

    @Query("SELECT r FROM RotinaMedicamento r JOIN FETCH r.medicamento JOIN FETCH r.idoso WHERE r.id = :id")
    Optional<RotinaMedicamento> findByIdWithDetails(@Param("id") UUID id);
}
