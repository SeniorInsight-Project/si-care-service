package com.seniorinsight.backendcore.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.TelemetriaWearable;

/**
 * Repositório Spring Data JPA para a entidade TelemetriaWearable.
 * Contém consultas otimizadas utilizando os índices time-series da tabela telemetria_wearable.
 */
@Repository
public interface TelemetriaWearableRepository extends JpaRepository<TelemetriaWearable, UUID> {

    /**
     * Busca o histórico de telemetrias de um idoso ordenado pela data de coleta decrescente.
     */
    List<TelemetriaWearable> findByIdosoIdOrderByColetadoEmDesc(UUID idosoId, Pageable pageable);

    /**
     * Obtém a última leitura de telemetria registrada para o idoso.
     */
    Optional<TelemetriaWearable> findTopByIdosoIdOrderByColetadoEmDesc(UUID idosoId);

    /**
     * Recupera todos os eventos de queda detectados para um idoso específico.
     */
    List<TelemetriaWearable> findByIdosoIdAndQuedaDetectadaTrueOrderByColetadoEmDesc(UUID idosoId);

    /**
     * Busca leituras em um intervalo de tempo específico (para geração de gráficos ou relatórios).
     */
    @Query("SELECT t FROM TelemetriaWearable t WHERE t.idoso.id = :idosoId AND t.coletadoEm BETWEEN :inicio AND :fim ORDER BY t.coletadoEm ASC")
    List<TelemetriaWearable> findLeiturasNoPeriodo(
            @Param("idosoId") UUID idosoId,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim
    );
}
