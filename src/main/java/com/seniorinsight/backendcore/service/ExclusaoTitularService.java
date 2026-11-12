package com.seniorinsight.backendcore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.PedidoExclusao;
import com.seniorinsight.backendcore.mensageria.ExclusaoConcluida;
import com.seniorinsight.backendcore.mensageria.PublicadorEventos;
import com.seniorinsight.backendcore.repository.ProtocoloExclusaoRepository;

/**
 * Trata um pedido de exclusão dos dados do titular (HU-022, lgpd.erasure.requested): reserva o protocolo, exclui o
 * idoso ou o familiar e confirma ao report-service pela outbox. Tudo na mesma transação: se qualquer passo falhar, a
 * reserva e a exclusão voltam atrás e a confirmação não sai.
 */
@Service
public class ExclusaoTitularService {

    static final String SERVICO = "care-service";

    private static final Logger log = LoggerFactory.getLogger(ExclusaoTitularService.class);

    private final ProtocoloExclusaoRepository protocolos;
    private final ExclusaoIdosoService exclusaoIdoso;
    private final ExclusaoFamiliarService exclusaoFamiliar;
    private final PublicadorEventos publicador;

    public ExclusaoTitularService(ProtocoloExclusaoRepository protocolos, ExclusaoIdosoService exclusaoIdoso,
                                  ExclusaoFamiliarService exclusaoFamiliar, PublicadorEventos publicador) {
        this.protocolos = protocolos;
        this.exclusaoIdoso = exclusaoIdoso;
        this.exclusaoFamiliar = exclusaoFamiliar;
        this.publicador = publicador;
    }

    @Transactional
    public void tratar(PedidoExclusao pedido) {
        if (!protocolos.reservar(pedido.protocolo())) {
            log.info("Protocolo {} de exclusão já tratado, ignorado", pedido.protocolo());
            return;
        }
        switch (pedido.tipo()) {
            case IDOSO -> exclusaoIdoso.excluir(pedido.protocolo(), pedido.titularId(), pedido.titularSub());
            case FAMILIAR -> exclusaoFamiliar.excluir(pedido.protocolo(), pedido.titularSub());
        }
        publicador.publicar(new ExclusaoConcluida(pedido.protocolo(), SERVICO));
        log.info("Exclusão {} ({}) concluída no care-service", pedido.protocolo(), pedido.tipo());
    }
}
