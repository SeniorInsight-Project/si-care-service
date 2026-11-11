package com.seniorinsight.backendcore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.repository.FamiliarRepository;

/** Cria o Familiar do care-service a partir do perfil criado no identity-service (evento user.created). */
@Service
public class ProvisionarFamiliarService {

    private static final Logger log = LoggerFactory.getLogger(ProvisionarFamiliarService.class);

    private final FamiliarRepository familiarRepository;

    public ProvisionarFamiliarService(FamiliarRepository familiarRepository) {
        this.familiarRepository = familiarRepository;
    }

    /**
     * Cria o Familiar ligado ao {@code keycloakSub}, sem CPF. Se já existe um para esse sub (cadastro anterior ao
     * evento ou outro evento do mesmo perfil), não faz nada.
     *
     * @return {@code true} se criou
     */
    @Transactional
    public boolean criarSeAusente(String keycloakSub, String nome, String email) {
        if (familiarRepository.existsByKeycloakSub(keycloakSub)) {
            log.info("Familiar do sub {} já existe, evento ignorado", keycloakSub);
            return false;
        }
        familiarRepository.save(Familiar.builder()
                .keycloakSub(keycloakSub)
                .nomeCompleto(nome)
                .email(email)
                .build());
        log.info("Familiar criado para o sub {}", keycloakSub);
        return true;
    }
}
