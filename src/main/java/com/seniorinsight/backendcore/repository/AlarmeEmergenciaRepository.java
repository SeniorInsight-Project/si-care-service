package com.seniorinsight.backendcore.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.AlarmeEmergencia;
import com.seniorinsight.backendcore.domain.StatusAlarme;

/**
 * Repositório Spring Data JPA para operações com a entidade AlarmeEmergencia (HU35).
 */
@Repository
public interface AlarmeEmergenciaRepository extends JpaRepository<AlarmeEmergencia, UUID> {

    @Query("SELECT a FROM AlarmeEmergencia a WHERE a.idoso.id = :idosoId ORDER BY a.criadoEm DESC")
    List<AlarmeEmergencia> findByIdosoIdOrderByCriadoEmDesc(@Param("idosoId") UUID idosoId);

    @Query("SELECT a FROM AlarmeEmergencia a WHERE a.idoso.id = :idosoId AND a.status = :status ORDER BY a.criadoEm DESC")
    Optional<AlarmeEmergencia> findFirstByIdosoIdAndStatusOrderByCriadoEmDesc(@Param("idosoId") UUID idosoId, @Param("status") StatusAlarme status);

    List<AlarmeEmergencia> findByStatusOrderByCriadoEmDesc(StatusAlarme status);
}
