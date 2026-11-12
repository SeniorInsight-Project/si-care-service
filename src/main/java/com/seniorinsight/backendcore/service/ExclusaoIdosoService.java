package com.seniorinsight.backendcore.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.AvisoExclusaoRede;
import com.seniorinsight.backendcore.domain.DestinatarioAviso;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.repository.AvisoExclusaoRedeRepository;
import com.seniorinsight.backendcore.repository.ExclusaoDadosRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.TitularExcluidoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/**
 * Exclusão dos dados de um idoso. Antes de apagar, grava a cópia de quem da rede deve ser avisado (cenário 1 da
 * HU-022); depois apaga o idoso com tudo que depende dele e bloqueia a recriação pelo {@code sub} e pelo id.
 * Idoso sem cadastro no care não tem o que apagar: só fica o aviso, vazio, para o pedido constar como tratado.
 */
@Service
public class ExclusaoIdosoService {

    private final IdosoRepository idosos;
    private final VinculoRedeRepository vinculos;
    private final AvisoExclusaoRedeRepository avisos;
    private final TitularExcluidoRepository titulares;
    private final ExclusaoDadosRepository dados;
    private final Clock relogio;

    public ExclusaoIdosoService(IdosoRepository idosos, VinculoRedeRepository vinculos,
                                AvisoExclusaoRedeRepository avisos, TitularExcluidoRepository titulares,
                                ExclusaoDadosRepository dados, Clock relogio) {
        this.idosos = idosos;
        this.vinculos = vinculos;
        this.avisos = avisos;
        this.titulares = titulares;
        this.dados = dados;
        this.relogio = relogio;
    }

    @Transactional
    public void excluir(UUID protocolo, UUID idosoId, String titularSub) {
        Optional<Idoso> idoso = idosos.travarParaEscrita(idosoId).flatMap(idosos::findById);
        avisos.save(avisoDaRede(protocolo, idoso));
        idoso.ifPresent(encontrado -> apagar(encontrado, titularSub));
    }

    private AvisoExclusaoRede avisoDaRede(UUID protocolo, Optional<Idoso> idoso) {
        List<DestinatarioAviso> destinatarios = idoso
                .map(encontrado -> vinculos.findByIdosoIdWithFamiliar(encontrado.getId()).stream()
                        .map(vinculo -> paraDestinatario(vinculo.getFamiliar()))
                        .toList())
                .orElse(List.of());
        return new AvisoExclusaoRede(protocolo, idoso.map(Idoso::primeiroNome).orElse(null), destinatarios,
                relogio.instant());
    }

    private static DestinatarioAviso paraDestinatario(Familiar familiar) {
        return new DestinatarioAviso(familiar.getKeycloakSub(), familiar.getNomeCompleto(), familiar.getEmail());
    }

    private void apagar(Idoso idoso, String titularSub) {
        titulares.registrar(titularSub);
        titulares.registrar(idoso.getId().toString());
        dados.apagarEventosPendentesDoIdoso(idoso.getId());
        dados.apagarIdoso(idoso.getId());
    }
}
