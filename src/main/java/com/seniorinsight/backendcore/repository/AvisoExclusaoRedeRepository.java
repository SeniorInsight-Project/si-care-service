package com.seniorinsight.backendcore.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.AvisoExclusaoRede;

@Repository
public interface AvisoExclusaoRedeRepository extends JpaRepository<AvisoExclusaoRede, UUID> {

    /** Apaga os avisos gravados antes do instante, com os destinatários. Devolve quantos avisos foram apagados. */
    @Modifying
    @Query("DELETE FROM AvisoExclusaoRede a WHERE a.criadoEm < :limite")
    int apagarCriadosAntes(@Param("limite") Instant limite);
}
