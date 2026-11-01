package com.seniorinsight.backendcore.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.Medico;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, UUID> {

    Optional<Medico> findByKeycloakSub(String keycloakSub);

    boolean existsByKeycloakSub(String keycloakSub);

    boolean existsByCrm(String crm);

    Optional<Medico> findByCrm(String crm);
}
