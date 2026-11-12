package com.seniorinsight.backendcore.service;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.repository.AvisoExclusaoRedeRepository;

/**
 * Apaga os avisos à rede depois do prazo {@code care.exclusao.aviso-rede.retencao}: o aviso guarda nome e e-mail de
 * quem estava na rede e só existe para o notification-service enviar a mensagem, então não deve ficar para sempre.
 */
@Component
public class LimpezaAvisosExclusaoRede {

    private static final Logger log = LoggerFactory.getLogger(LimpezaAvisosExclusaoRede.class);

    private final AvisoExclusaoRedeRepository avisos;
    private final Clock relogio;
    private final Duration retencao;

    public LimpezaAvisosExclusaoRede(AvisoExclusaoRedeRepository avisos, Clock relogio,
                                     @Value("${care.exclusao.aviso-rede.retencao}") Duration retencao) {
        this.avisos = avisos;
        this.relogio = relogio;
        this.retencao = retencao;
    }

    @Scheduled(fixedDelayString = "${care.exclusao.aviso-rede.intervalo-limpeza}")
    @Transactional
    public void removerVencidos() {
        int removidos = avisos.apagarCriadosAntes(relogio.instant().minus(retencao));
        if (removidos > 0) {
            log.info("{} aviso(s) de exclusão à rede apagado(s) por vencimento", removidos);
        }
    }
}
