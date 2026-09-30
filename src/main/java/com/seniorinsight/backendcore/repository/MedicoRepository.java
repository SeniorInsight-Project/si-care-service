package com.seniorinsight.backendcore.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.Medico;
import com.seniorinsight.backendcore.model.Usuario;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, UUID> {

    Optional<Medico> findByUsuario(Usuario usuario);

    Optional<Medico> findByUsuarioId(UUID usuarioId);

    boolean existsByUsuario(Usuario usuario);

    boolean existsByUsuarioId(UUID usuarioId);

    boolean existsByCrm(String crm);

    Optional<Medico> findByCrm(String crm);
}
