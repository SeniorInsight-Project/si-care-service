package com.seniorinsight.backendcore.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.Idoso;

import jakarta.persistence.LockModeType;

@Repository
public interface IdosoRepository extends JpaRepository<Idoso, UUID> {

    Optional<Idoso> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, UUID id);

    /** Trava a linha do idoso até o fim da transação, para serializar escritas que dependem de uma contagem. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i.id FROM Idoso i WHERE i.id = :id")
    Optional<UUID> travarParaEscrita(@Param("id") UUID id);

    /** Idosos em que o familiar tem vínculo na rede. */
    @Query("SELECT v.idoso FROM VinculoRede v WHERE v.familiar.keycloakSub = :sub")
    List<Idoso> findByFamiliarKeycloakSub(@Param("sub") String sub);

    /** Idosos do familiar {@code alvo} em que o familiar {@code autor} é Administrador. */
    @Query("""
            SELECT v.idoso FROM VinculoRede v
            WHERE v.familiar.keycloakSub = :alvo
              AND EXISTS (SELECT 1 FROM VinculoRede a
                          WHERE a.idoso = v.idoso AND a.familiar.keycloakSub = :autor
                            AND a.papelAcesso = 'ADMINISTRADOR')
            """)
    List<Idoso> findDoFamiliarAdministradosPor(@Param("alvo") String alvo, @Param("autor") String autor);

    /** Idosos acompanhados pelo médico. */
    @Query("SELECT i FROM Idoso i JOIN i.medicos m WHERE m.keycloakSub = :sub")
    List<Idoso> findByMedicoKeycloakSub(@Param("sub") String sub);

    @Query("SELECT COUNT(i) > 0 FROM Idoso i JOIN i.medicos m WHERE i.id = :idosoId AND m.keycloakSub = :sub")
    boolean medicoAcompanha(@Param("idosoId") UUID idosoId, @Param("sub") String sub);
}
