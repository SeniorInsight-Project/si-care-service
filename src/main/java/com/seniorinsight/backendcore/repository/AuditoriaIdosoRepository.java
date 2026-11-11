package com.seniorinsight.backendcore.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seniorinsight.backendcore.domain.AuditoriaIdoso;

@Repository
public interface AuditoriaIdosoRepository extends JpaRepository<AuditoriaIdoso, UUID> {

    List<AuditoriaIdoso> findByIdosoIdOrderByOcorridoEmAsc(UUID idosoId);
}
