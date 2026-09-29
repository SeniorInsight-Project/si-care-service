package com.seniorinsight.backendcore.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.model.Usuario;

@Repository
public interface FamiliarRepository extends JpaRepository<Familiar, UUID> {

    Optional<Familiar> findByUsuario(Usuario usuario);

    Optional<Familiar> findByUsuarioId(UUID usuarioId);

    boolean existsByUsuario(Usuario usuario);

    boolean existsByUsuarioId(UUID usuarioId);

    boolean existsByCpf(String cpf);

    Optional<Familiar> findByCpf(String cpf);
}
