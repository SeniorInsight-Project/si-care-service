package com.seniorinsight.backendcore.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.Medicamento;

/**
 * Repositório de dados para Medicamento (HU26).
 */
@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, UUID> {

    Optional<Medicamento> findByNomeIgnoreCaseAndDosagemIgnoreCase(String nome, String dosagem);

    boolean existsByNomeIgnoreCaseAndDosagemIgnoreCase(String nome, String dosagem);
}
