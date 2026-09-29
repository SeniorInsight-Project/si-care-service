package com.seniorinsight.backendcore.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.Idoso;

@Repository
public interface IdosoRepository extends JpaRepository<Idoso, UUID> {

    Optional<Idoso> findByCpf(String cpf);

    boolean existsByCpf(String cpf);
}
