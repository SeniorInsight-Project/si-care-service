package com.seniorinsight.backendcore.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.AlarmeEmergencia;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.StatusAlarme;
import com.seniorinsight.backendcore.dto.AlarmeEmergenciaResponseDTO;
import com.seniorinsight.backendcore.dto.AtualizarStatusAlarmeRequestDTO;
import com.seniorinsight.backendcore.dto.DispararSosRequestDTO;
import com.seniorinsight.backendcore.event.SosAcionadoEvent;
import com.seniorinsight.backendcore.exception.AlarmeNaoEncontradoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.repository.AlarmeEmergenciaRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;

/**
 * Serviço de regras de negócio para o Botão de Alarme / SOS de Emergência (HU35).
 * Desenvolvido com foco no cumprimento do SLA crítico (< 100ms) para disparo de socorro,
 * persistindo o alarme imediatamente e delegando a notificação da rede de proteção
 * a eventos assíncronos do Spring Framework.
 */
@Service
public class AlarmeEmergenciaService {

    private static final Logger log = LoggerFactory.getLogger(AlarmeEmergenciaService.class);

    private final AlarmeEmergenciaRepository alarmeEmergenciaRepository;
    private final IdosoRepository idosoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AlarmeEmergenciaService(
            AlarmeEmergenciaRepository alarmeEmergenciaRepository,
            IdosoRepository idosoRepository,
            ApplicationEventPublisher eventPublisher) {
        this.alarmeEmergenciaRepository = alarmeEmergenciaRepository;
        this.idosoRepository = idosoRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registra o disparo imediato do alarme de emergência e lança evento assíncrono para a rede de proteção.
     * SLA Crítico: Execução ultrarrápida (< 100ms) respondida com HTTP 202 Accepted.
     *
     * @param idosoId Identificador do idoso que acionou o SOS
     * @param request Dados de telemetria e coordenadas GPS
     * @param usuarioAutenticado Usuário responsável pela requisição (opcional)
     * @return DTO com os dados do alarme disparado
     */
    @Transactional
    public AlarmeEmergenciaResponseDTO dispararAlarme(UUID idosoId, DispararSosRequestDTO request, Usuario usuarioAutenticado) {
        long inicioMs = System.currentTimeMillis();

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        Idoso idoso = idosoRepository.findById(idosoId)
                .orElseThrow(() -> {
                    log.warn("[SISTEMA SOS] Idoso não encontrado ao tentar acionar SOS: {}", idosoId);
                    return new IdosoNaoEncontradoException(idosoId);
                });

        AlarmeEmergencia alarme = AlarmeEmergencia.builder()
                .idoso(idoso)
                .latitude(request != null ? request.latitude() : null)
                .longitude(request != null ? request.longitude() : null)
                .nivelBateria(request != null ? request.nivelBateria() : null)
                .observacao(request != null ? request.observacao() : null)
                .status(StatusAlarme.DISPARADO)
                .build();

        AlarmeEmergencia alarmeSalvo = alarmeEmergenciaRepository.save(alarme);

        // Lança evento de domínio desacoplado para notificação da rede de proteção
        eventPublisher.publishEvent(new SosAcionadoEvent(
                alarmeSalvo.getId(),
                idoso.getId(),
                idoso.getNomeCompleto(),
                alarmeSalvo.getLatitude(),
                alarmeSalvo.getLongitude(),
                alarmeSalvo.getNivelBateria(),
                alarmeSalvo.getObservacao(),
                alarmeSalvo.getCriadoEm()
        ));

        long duracaoMs = System.currentTimeMillis() - inicioMs;
        log.info("[SISTEMA SOS] Alarme ID {} disparado para idoso {} com sucesso em {}ms (SLA < 100ms).",
                alarmeSalvo.getId(), idoso.getId(), duracaoMs);

        return AlarmeEmergenciaResponseDTO.fromEntity(
                alarmeSalvo,
                "Alarme SOS de emergência disparado com sucesso. Notificações à rede de proteção sendo transmitidas em background."
        );
    }

    /**
     * Atualiza o status do alarme (ex: EM_ATENDIMENTO, RESOLVIDO, CANCELADO).
     *
     * @param alarmeId Identificador único do alarme
     * @param request Dados de atualização com o novo status e notas
     * @param usuarioAutenticado Usuário responsável pela atualização
     * @return DTO com os dados do alarme atualizados
     */
    @Transactional
    public AlarmeEmergenciaResponseDTO atualizarStatus(UUID alarmeId, AtualizarStatusAlarmeRequestDTO request, Usuario usuarioAutenticado) {
        if (alarmeId == null) {
            throw new IllegalArgumentException("O identificador do alarme não pode ser nulo.");
        }
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("O novo status do alarme é obrigatório.");
        }

        AlarmeEmergencia alarme = alarmeEmergenciaRepository.findById(alarmeId)
                .orElseThrow(() -> {
                    log.warn("[SISTEMA SOS] Tentativa de atualizar alarme inexistente: {}", alarmeId);
                    return new AlarmeNaoEncontradoException(alarmeId);
                });

        alarme.atualizarStatus(request.status(), request.observacao());
        AlarmeEmergencia atualizado = alarmeEmergenciaRepository.save(alarme);

        log.info("[SISTEMA SOS] Status do alarme {} alterado para {} com sucesso.",
                alarmeId, request.status());

        return AlarmeEmergenciaResponseDTO.fromEntity(
                atualizado,
                "Status do alarme atualizado com sucesso para " + request.status() + "."
        );
    }

    /**
     * Consulta os detalhes de um alarme de emergência pelo seu identificador único.
     *
     * @param alarmeId Identificador único do alarme
     * @return DTO com os dados do alarme
     */
    @Transactional(readOnly = true)
    public AlarmeEmergenciaResponseDTO buscarPorId(UUID alarmeId) {
        if (alarmeId == null) {
            throw new IllegalArgumentException("O identificador do alarme não pode ser nulo.");
        }

        AlarmeEmergencia alarme = alarmeEmergenciaRepository.findById(alarmeId)
                .orElseThrow(() -> new AlarmeNaoEncontradoException(alarmeId));

        return AlarmeEmergenciaResponseDTO.fromEntity(alarme, "Alarme localizado com sucesso.");
    }

    /**
     * Lista o histórico de alarmes de emergência de um idoso em ordem cronológica decrescente.
     *
     * @param idosoId Identificador do idoso
     * @return Lista de DTOs com o histórico de alarmes
     */
    @Transactional(readOnly = true)
    public List<AlarmeEmergenciaResponseDTO> listarPorIdoso(UUID idosoId) {
        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        if (!idosoRepository.existsById(idosoId)) {
            throw new IdosoNaoEncontradoException(idosoId);
        }

        return alarmeEmergenciaRepository.findByIdosoIdOrderByCriadoEmDesc(idosoId)
                .stream()
                .map(alarme -> AlarmeEmergenciaResponseDTO.fromEntity(alarme, null))
                .toList();
    }
}
