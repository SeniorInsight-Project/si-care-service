package com.seniorinsight.backendcore.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.model.ProvedorSso;
import com.seniorinsight.backendcore.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByProvedorSsoAndSsoId(ProvedorSso provedorSso, String ssoId);

    boolean existsByEmail(String email);
}
