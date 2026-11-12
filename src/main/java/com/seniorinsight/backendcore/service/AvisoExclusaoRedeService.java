package com.seniorinsight.backendcore.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.AvisoExclusaoRede;
import com.seniorinsight.backendcore.dto.AvisoExclusaoRedeResponseDTO;
import com.seniorinsight.backendcore.exception.AvisoExclusaoNaoEncontradoException;
import com.seniorinsight.backendcore.repository.AvisoExclusaoRedeRepository;

/** Consulta, pelo protocolo, da cópia de quem da rede deve ser avisado da exclusão dos dados do idoso. */
@Service
public class AvisoExclusaoRedeService {

    private final AvisoExclusaoRedeRepository avisos;

    public AvisoExclusaoRedeService(AvisoExclusaoRedeRepository avisos) {
        this.avisos = avisos;
    }

    @Transactional(readOnly = true)
    public AvisoExclusaoRedeResponseDTO consultar(UUID protocolo) {
        AvisoExclusaoRede aviso = avisos.findById(protocolo).orElseThrow(() ->
                new AvisoExclusaoNaoEncontradoException("Não há aviso à rede para o protocolo informado."));
        return new AvisoExclusaoRedeResponseDTO(aviso.getProtocolo(), aviso.getPrimeiroNomeIdoso(),
                aviso.getDestinatarios().stream()
                        .map(d -> new AvisoExclusaoRedeResponseDTO.Membro(d.usuarioId(), d.nomeCompleto(), d.email()))
                        .toList());
    }
}
