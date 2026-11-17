package com.seniorinsight.backendcore.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.Familiar;

@Repository
public interface FamiliarRepository extends JpaRepository<Familiar, UUID> {

    Optional<Familiar> findByKeycloakSub(String keycloakSub);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Familiar f WHERE f.keycloakSub = :sub")
    Optional<Familiar> travarPorSub(@org.springframework.data.repository.query.Param("sub") String sub);

    boolean existsByKeycloakSub(String keycloakSub);

    Optional<Familiar> findByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    Optional<Familiar> findByCpf(String cpf);
}
