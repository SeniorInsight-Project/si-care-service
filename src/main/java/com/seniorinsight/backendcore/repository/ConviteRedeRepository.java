package com.seniorinsight.backendcore.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.ConviteRede;
import com.seniorinsight.backendcore.domain.StatusConvite;

/**
 * Repositório Spring Data JPA para a entidade ConviteRede (HU16).
 */
@Repository
public interface ConviteRedeRepository extends JpaRepository<ConviteRede, UUID> {

    /**
     * Localiza um convite pelo token temporal gerado via UUID com eager fetch do Idoso.
     */
    @Query("SELECT c FROM ConviteRede c JOIN FETCH c.idoso WHERE c.token = :token")
    Optional<ConviteRede> findByTokenWithIdoso(@Param("token") String token);

    /**
     * Localiza um convite pelo seu token único.
     */
    Optional<ConviteRede> findByToken(String token);

    /**
     * Lista todos os convites emitidos para um determinado idoso.
     */
    List<ConviteRede> findByIdosoId(UUID idosoId);

    /**
     * Lista todos os convites de um idoso filtrados pelo status do convite.
     */
    List<ConviteRede> findByIdosoIdAndStatus(UUID idosoId, StatusConvite status);

    /**
     * Busca um convite específico por idoso, e-mail do destinatário e status.
     */
    Optional<ConviteRede> findByIdosoIdAndEmailAndStatus(UUID idosoId, String email, StatusConvite status);

    /**
     * Verifica a existência de convite ativo (pendente) para o mesmo idoso e e-mail.
     */
    boolean existsByIdosoIdAndEmailAndStatus(UUID idosoId, String email, StatusConvite status);
}
