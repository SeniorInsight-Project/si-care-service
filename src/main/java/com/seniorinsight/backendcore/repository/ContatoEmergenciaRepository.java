package com.seniorinsight.backendcore.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.ContatoEmergencia;

@Repository
public interface ContatoEmergenciaRepository extends JpaRepository<ContatoEmergencia, UUID> {

    List<ContatoEmergencia> findByIdosoIdOrderByPrioridadeAscCriadoEmAsc(UUID idosoId);

    Optional<ContatoEmergencia> findByIdAndIdosoId(UUID id, UUID idosoId);

    long countByIdosoId(UUID idosoId);

    @Query("SELECT COALESCE(MAX(c.prioridade), 0) FROM ContatoEmergencia c WHERE c.idosoId = :idosoId")
    int maiorPrioridade(@Param("idosoId") UUID idosoId);
}
