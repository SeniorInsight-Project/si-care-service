package com.seniorinsight.backendcore.service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.AuditoriaIdoso;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.repository.AuditoriaIdosoRepository;
import com.seniorinsight.backendcore.repository.ExclusaoDadosRepository;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.TitularExcluidoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/**
 * Exclusão dos dados de um familiar, localizado pelo {@code sub} do Keycloak. Os idosos da rede continuam. Se o
 * familiar era o administrador de um idoso, a exclusão segue (o direito do titular prevalece) e a auditoria do idoso
 * registra que ele ficou sem administrador, sem identificar o titular.
 */
@Service
public class ExclusaoFamiliarService {

    static final String MOTIVO = "exclusao-lgpd";

    private final FamiliarRepository familiares;
    private final VinculoRedeRepository vinculos;
    private final AuditoriaIdosoRepository auditoria;
    private final TitularExcluidoRepository titulares;
    private final ExclusaoDadosRepository dados;
    private final Clock relogio;

    public ExclusaoFamiliarService(FamiliarRepository familiares, VinculoRedeRepository vinculos,
                                   AuditoriaIdosoRepository auditoria, TitularExcluidoRepository titulares,
                                   ExclusaoDadosRepository dados, Clock relogio) {
        this.familiares = familiares;
        this.vinculos = vinculos;
        this.auditoria = auditoria;
        this.titulares = titulares;
        this.dados = dados;
        this.relogio = relogio;
    }

    @Transactional
    public void excluir(UUID protocolo, String titularSub) {
        titulares.travar(titularSub);
        familiares.findByKeycloakSub(titularSub).ifPresent(familiar -> apagar(familiar, protocolo));
        titulares.registrar(titularSub);
    }

    private void apagar(Familiar familiar, UUID protocolo) {
        registrarIdososSemAdministrador(familiar, protocolo);
        if (familiar.getEmail() != null) {
            dados.apagarConvitesDoEmail(familiar.getEmail());
        }
        dados.anonimizarAutoria(familiar.getKeycloakSub());
        dados.apagarFamiliar(familiar.getKeycloakSub());
    }

    private void registrarIdososSemAdministrador(Familiar familiar, UUID protocolo) {
        vinculos.findByFamiliarId(familiar.getId()).stream()
                .filter(vinculo -> PapelAcesso.ADMINISTRADOR.equals(vinculo.getPapelAcesso()))
                .forEach(vinculo -> auditoria.save(new AuditoriaIdoso(
                        vinculo.getIdoso().getId(),
                        ExclusaoDadosRepository.AUTORIA_ANONIMIZADA,
                        relogio.instant(),
                        Map.of("administrador", true),
                        Map.of("administrador", false, "motivo", MOTIVO, "protocolo", protocolo.toString()))));
    }
}
