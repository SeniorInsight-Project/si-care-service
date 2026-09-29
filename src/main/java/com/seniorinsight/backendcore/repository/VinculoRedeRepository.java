package com.seniorinsight.backendcore.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.VinculoRede;

@Repository
public interface VinculoRedeRepository extends JpaRepository<VinculoRede, UUID> {

    List<VinculoRede> findByFamiliarId(UUID familiarId);

    List<VinculoRede> findByIdosoId(UUID idosoId);

    Optional<VinculoRede> findByFamiliarAndIdoso(Familiar familiar, Idoso idoso);

    boolean existsByFamiliarAndIdoso(Familiar familiar, Idoso idoso);
}
