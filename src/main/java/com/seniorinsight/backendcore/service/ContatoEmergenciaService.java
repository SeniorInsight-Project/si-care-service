package com.seniorinsight.backendcore.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.ContatoEmergencia;
import com.seniorinsight.backendcore.dto.ContatoEmergenciaRequestDTO;
import com.seniorinsight.backendcore.dto.ContatoEmergenciaResponseDTO;
import com.seniorinsight.backendcore.exception.ContatoEmergenciaNaoEncontradoException;
import com.seniorinsight.backendcore.exception.LimiteContatosEmergenciaException;
import com.seniorinsight.backendcore.repository.ContatoEmergenciaRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.validation.Telefones;

/**
 * Contatos de emergência do idoso (HU-011). Só o Familiar Administrador escreve; a rede, o médico e o próprio idoso
 * pareado leem. O idoso tem no máximo {@value #LIMITE_CONTATOS} contatos e o telefone é gravado em E.164.
 */
@Service
public class ContatoEmergenciaService {

    private static final Logger log = LoggerFactory.getLogger(ContatoEmergenciaService.class);

    /** Limite do #78 (HU-011), ainda fora do contrato care-v1. */
    static final int LIMITE_CONTATOS = 3;

    private final ContatoEmergenciaRepository repository;
    private final IdosoRepository idosoRepository;
    private final AcessoIdosoService acessoIdosoService;

    public ContatoEmergenciaService(ContatoEmergenciaRepository repository,
                                    IdosoRepository idosoRepository,
                                    AcessoIdosoService acessoIdosoService) {
        this.repository = repository;
        this.idosoRepository = idosoRepository;
        this.acessoIdosoService = acessoIdosoService;
    }

    @Transactional(readOnly = true)
    public List<ContatoEmergenciaResponseDTO> listar(UsuarioAutenticado usuario, UUID idosoId) {
        acessoIdosoService.exigirLeitura(usuario, idosoId);
        return repository.findByIdosoIdOrderByPrioridadeAscCriadoEmAsc(idosoId).stream()
                .map(ContatoEmergenciaResponseDTO::de)
                .toList();
    }

    @Transactional
    public ContatoEmergenciaResponseDTO adicionar(UsuarioAutenticado usuario, UUID idosoId,
                                                  ContatoEmergenciaRequestDTO request) {
        acessoIdosoService.exigirAdministrador(usuario, idosoId);
        // Trava a linha do idoso para que dois POST simultâneos não passem do limite.
        idosoRepository.travarParaEscrita(idosoId);

        if (repository.countByIdosoId(idosoId) >= LIMITE_CONTATOS) {
            throw new LimiteContatosEmergenciaException(LIMITE_CONTATOS);
        }
        int prioridade = request.prioridade() != null ? request.prioridade()
                : repository.maiorPrioridade(idosoId) + 1;

        ContatoEmergencia contato = repository.save(new ContatoEmergencia(
                idosoId, request.nome().trim(), telefone(request), texto(request.grauParentesco()), prioridade));
        log.info("Contato de emergência {} adicionado ao idoso {} por {}", contato.getId(), idosoId,
                usuario.keycloakSub());
        return ContatoEmergenciaResponseDTO.de(contato);
    }

    @Transactional
    public ContatoEmergenciaResponseDTO atualizar(UsuarioAutenticado usuario, UUID idosoId, UUID contatoId,
                                                  ContatoEmergenciaRequestDTO request) {
        acessoIdosoService.exigirAdministrador(usuario, idosoId);
        ContatoEmergencia contato = buscar(idosoId, contatoId);

        int prioridade = request.prioridade() != null ? request.prioridade() : contato.getPrioridade();
        contato.preencher(request.nome().trim(), telefone(request), texto(request.grauParentesco()), prioridade);
        contato = repository.saveAndFlush(contato);
        log.info("Contato de emergência {} do idoso {} atualizado por {}", contatoId, idosoId,
                usuario.keycloakSub());
        return ContatoEmergenciaResponseDTO.de(contato);
    }

    @Transactional
    public void remover(UsuarioAutenticado usuario, UUID idosoId, UUID contatoId) {
        acessoIdosoService.exigirAdministrador(usuario, idosoId);
        repository.delete(buscar(idosoId, contatoId));
        log.info("Contato de emergência {} do idoso {} removido por {}", contatoId, idosoId, usuario.keycloakSub());
    }

    private ContatoEmergencia buscar(UUID idosoId, UUID contatoId) {
        return repository.findByIdAndIdosoId(contatoId, idosoId)
                .orElseThrow(() -> new ContatoEmergenciaNaoEncontradoException(contatoId));
    }

    private static String telefone(ContatoEmergenciaRequestDTO request) {
        return Telefones.paraE164(request.telefone())
                .orElseThrow(() -> new IllegalArgumentException("O telefone é inválido."));
    }

    private static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String s = valor.trim();
        return s.isEmpty() ? null : s;
    }
}
