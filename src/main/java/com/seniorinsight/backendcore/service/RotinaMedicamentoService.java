package com.seniorinsight.backendcore.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.HistoricoMedicacao;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.Medicamento;
import com.seniorinsight.backendcore.domain.RotinaMedicamento;
import com.seniorinsight.backendcore.domain.StatusIngestao;
import com.seniorinsight.backendcore.dto.AtualizarRotinaRequestDTO;
import com.seniorinsight.backendcore.dto.ConfirmarIngestaoRequestDTO;
import com.seniorinsight.backendcore.dto.HistoricoMedicacaoResponseDTO;
import com.seniorinsight.backendcore.dto.RotinaMedicamentoRequestDTO;
import com.seniorinsight.backendcore.dto.RotinaMedicamentoResponseDTO;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.MedicamentoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.exception.RotinaMedicamentoNaoEncontradaException;
import com.seniorinsight.backendcore.model.StatusUsuario;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.HistoricoMedicacaoRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.MedicamentoRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.RotinaMedicamentoRepository;
import com.seniorinsight.backendcore.repository.UsuarioRepository;

/**
 * Serviço de regras de negócio para a Rotina de Medicamentos (HU26).
 * Gerencia a agenda diária de medicação, controle de fármacos prescritos,
 * confirmação de ingestão com validação estrita da janela clínica de tolerância de 30 minutos
 * e histórico detalhado para acompanhamento da adesão ao tratamento.
 */
@Service
public class RotinaMedicamentoService {

    private static final Logger log = LoggerFactory.getLogger(RotinaMedicamentoService.class);
    private static final ZoneId FUSO_HORARIO_PADRAO = ZoneId.of("America/Sao_Paulo");
    private static final long TOLERANCIA_MINUTOS = 30L;

    private final RotinaMedicamentoRepository rotinaMedicamentoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final HistoricoMedicacaoRepository historicoMedicacaoRepository;
    private final IdosoRepository idosoRepository;
    private final UsuarioRepository usuarioRepository;
    private final FamiliarRepository familiarRepository;
    private final MedicoRepository medicoRepository;

    public RotinaMedicamentoService(
            RotinaMedicamentoRepository rotinaMedicamentoRepository,
            MedicamentoRepository medicamentoRepository,
            HistoricoMedicacaoRepository historicoMedicacaoRepository,
            IdosoRepository idosoRepository,
            UsuarioRepository usuarioRepository,
            FamiliarRepository familiarRepository,
            MedicoRepository medicoRepository) {
        this.rotinaMedicamentoRepository = rotinaMedicamentoRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.historicoMedicacaoRepository = historicoMedicacaoRepository;
        this.idosoRepository = idosoRepository;
        this.usuarioRepository = usuarioRepository;
        this.familiarRepository = familiarRepository;
        this.medicoRepository = medicoRepository;
    }

    /**
     * Cria uma nova agenda/rotina de medicamento associando o fármaco ao idoso.
     *
     * @param idosoId Identificador único do idoso
     * @param request Dados da rotina e do medicamento
     * @param usuarioAutenticado Usuário responsável pela criação
     * @return DTO com os dados completos da rotina cadastrada
     */
    @Transactional
    public RotinaMedicamentoResponseDTO criarRotina(
            UUID idosoId,
            RotinaMedicamentoRequestDTO request,
            Usuario usuarioAutenticado) {

        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Os dados da rotina de medicamentos não podem ser nulos.");
        }
        if (request.horario() == null) {
            throw new IllegalArgumentException("O horário da rotina é obrigatório.");
        }
        if (request.frequencia() == null || request.frequencia().isBlank()) {
            throw new IllegalArgumentException("A frequência da rotina é obrigatória.");
        }

        Idoso idoso = idosoRepository.findById(idosoId)
                .orElseThrow(() -> {
                    log.warn("Tentativa de criar rotina para idoso inexistente: {}", idosoId);
                    return new IdosoNaoEncontradoException(idosoId);
                });

        Medicamento medicamento = resolverMedicamento(request);

        LocalDate dataInicio = request.dataInicio() != null ? request.dataInicio() : LocalDate.now(FUSO_HORARIO_PADRAO);
        String diasSemana = (request.diasSemana() != null && !request.diasSemana().isBlank())
                ? request.diasSemana().trim()
                : "TODOS";

        RotinaMedicamento rotina = RotinaMedicamento.builder()
                .idoso(idoso)
                .medicamento(medicamento)
                .horario(request.horario())
                .frequencia(request.frequencia().trim())
                .diasSemana(diasSemana)
                .dataInicio(dataInicio)
                .dataFim(request.dataFim())
                .ativo(true)
                .observacoes(request.observacoes())
                .build();

        RotinaMedicamento rotinaSalva = rotinaMedicamentoRepository.save(rotina);
        log.info("Rotina de medicamento {} criada com sucesso para o idoso {} pelo usuário {}",
                rotinaSalva.getId(), idosoId, usuario.getEmail());

        return mapearParaResponseDTO(rotinaSalva, LocalDate.now(FUSO_HORARIO_PADRAO));
    }

    /**
     * Lista a rotina diária de medicamentos de um idoso, ordenando cronologicamente por horário
     * e exibindo o status de ingestão em tempo real no dia consultado (PENDENTE, TOMADO, ATRASADO, IGNORADO).
     *
     * @param idosoId Identificador único do idoso
     * @param dataReferencia Data a ser consultada (padrão hoje)
     * @param apenasAtivos Filtra se deve retornar apenas rotinas ativas (padrão true)
     * @param usuarioAutenticado Usuário autenticado executando a consulta
     * @return Lista de rotinas diárias otimizada para React Native
     */
    @Transactional(readOnly = true)
    public List<RotinaMedicamentoResponseDTO> listarRotinasDiarias(
            UUID idosoId,
            LocalDate dataReferencia,
            Boolean apenasAtivos,
            Usuario usuarioAutenticado) {

        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        if (!idosoRepository.existsById(idosoId)) {
            log.warn("Tentativa de listar rotinas para idoso inexistente: {}", idosoId);
            throw new IdosoNaoEncontradoException(idosoId);
        }

        LocalDate dataAlvo = dataReferencia != null ? dataReferencia : LocalDate.now(FUSO_HORARIO_PADRAO);
        Boolean filtroAtivo = (apenasAtivos != null) ? apenasAtivos : true;

        List<RotinaMedicamento> rotinas = rotinaMedicamentoRepository.findByIdosoIdAndAtivoWithDetails(idosoId, filtroAtivo);

        // Filtra pela vigência do tratamento na data de referência
        List<RotinaMedicamento> rotinasVigentes = rotinas.stream()
                .filter(r -> r.getDataInicio() == null || !r.getDataInicio().isAfter(dataAlvo))
                .filter(r -> r.getDataFim() == null || !r.getDataFim().isBefore(dataAlvo))
                .toList();

        return rotinasVigentes.stream()
                .map(r -> mapearParaResponseDTO(r, dataAlvo))
                .toList();
    }

    /**
     * Confirma a ingestão de um medicamento programado na rotina, salvando no histórico
     * e validando rigorosamente a janela de tolerância clínica de 30 minutos em relação ao horário agendado.
     *
     * @param rotinaId Identificador único da rotina de medicamento
     * @param request Dados adicionais opcionais (timestamp exato, observação, status forçado)
     * @param usuarioAutenticado Usuário que está confirmando a ingestão
     * @return DTO com informações detalhadas do registro e feedback da tolerância
     */
    @Transactional
    public HistoricoMedicacaoResponseDTO confirmarIngestao(
            UUID rotinaId,
            ConfirmarIngestaoRequestDTO request,
            Usuario usuarioAutenticado) {

        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        if (rotinaId == null) {
            throw new IllegalArgumentException("O identificador da rotina não pode ser nulo.");
        }

        RotinaMedicamento rotina = rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)
                .orElseThrow(() -> {
                    log.warn("Tentativa de confirmar ingestão para rotina inexistente: {}", rotinaId);
                    return new RotinaMedicamentoNaoEncontradaException(rotinaId);
                });

        Instant dataHoraIngestao = (request != null && request.dataHoraIngestao() != null)
                ? request.dataHoraIngestao()
                : Instant.now();

        LocalDate dataReferencia = (request != null && request.dataReferencia() != null)
                ? request.dataReferencia()
                : dataHoraIngestao.atZone(FUSO_HORARIO_PADRAO).toLocalDate();

        // Calcula o horário exato agendado na data de referência
        ZonedDateTime zdtPrevisto = ZonedDateTime.of(dataReferencia, rotina.getHorario(), FUSO_HORARIO_PADRAO);
        Instant dataHoraPrevista = zdtPrevisto.toInstant();

        // Diferença em minutos entre o horário realizado e o horário agendado
        long diferencaMinutos = Duration.between(dataHoraPrevista, dataHoraIngestao).toMinutes();

        StatusIngestao statusFinal;
        boolean dentroJanela;
        String mensagemFeedback;

        // Se o usuário explicitamente marcou como IGNORADO
        if (request != null && request.status() == StatusIngestao.IGNORADO) {
            statusFinal = StatusIngestao.IGNORADO;
            dentroJanela = false;
            mensagemFeedback = "Medicação marcada como IGNORADA (não ingerida).";
        } else {
            // Regra da janela de tolerância de 30 minutos:
            // Tolerância clínica aceita ingestão até 30 minutos após o horário agendado.
            if (diferencaMinutos <= TOLERANCIA_MINUTOS) {
                statusFinal = StatusIngestao.TOMADO;
                dentroJanela = true;
                mensagemFeedback = "Ingestão confirmada com sucesso dentro da janela de tolerância de 30 minutos.";
            } else {
                statusFinal = StatusIngestao.ATRASADO;
                dentroJanela = false;
                mensagemFeedback = String.format(
                        "Ingestão confirmada com ATRASO (%d minutos após o horário previsto).", diferencaMinutos);
            }
        }

        String observacao = request != null ? request.observacao() : null;

        HistoricoMedicacao historico = HistoricoMedicacao.builder()
                .rotina(rotina)
                .status(statusFinal)
                .dataHoraIngestao(dataHoraIngestao)
                .dataHoraPrevista(dataHoraPrevista)
                .observacao(observacao)
                .registradoPor(usuario)
                .build();

        HistoricoMedicacao historicoSalvo = historicoMedicacaoRepository.save(historico);
        log.info("Ingestão registrada para a rotina {} com status {}. Janela de tolerância: {}",
                rotinaId, statusFinal, dentroJanela);

        return new HistoricoMedicacaoResponseDTO(
                historicoSalvo.getId(),
                rotina.getId(),
                rotina.getMedicamento().getNome(),
                rotina.getMedicamento().getDosagem(),
                historicoSalvo.getStatus(),
                historicoSalvo.getDataHoraIngestao(),
                historicoSalvo.getDataHoraPrevista(),
                dentroJanela,
                diferencaMinutos,
                historicoSalvo.getObservacao(),
                usuario.getEmail(),
                mensagemFeedback,
                historicoSalvo.getCriadoEm()
        );
    }

    /**
     * Obtém uma rotina de medicamento específica por ID.
     */
    @Transactional(readOnly = true)
    public RotinaMedicamentoResponseDTO obterRotinaPorId(UUID rotinaId, Usuario usuarioAutenticado) {
        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        RotinaMedicamento rotina = rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)
                .orElseThrow(() -> new RotinaMedicamentoNaoEncontradaException(rotinaId));

        return mapearParaResponseDTO(rotina, LocalDate.now(FUSO_HORARIO_PADRAO));
    }

    /**
     * Atualiza dados de uma rotina de medicamento existente.
     */
    @Transactional
    public RotinaMedicamentoResponseDTO atualizarRotina(
            UUID rotinaId,
            AtualizarRotinaRequestDTO request,
            Usuario usuarioAutenticado) {

        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        if (request == null) {
            throw new IllegalArgumentException("Os dados de atualização não podem ser nulos.");
        }

        RotinaMedicamento rotina = rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)
                .orElseThrow(() -> new RotinaMedicamentoNaoEncontradaException(rotinaId));

        if (request.horario() != null) {
            rotina.setHorario(request.horario());
        }
        if (request.frequencia() != null && !request.frequencia().isBlank()) {
            rotina.setFrequencia(request.frequencia().trim());
        }
        if (request.diasSemana() != null && !request.diasSemana().isBlank()) {
            rotina.setDiasSemana(request.diasSemana().trim());
        }
        if (request.dataInicio() != null) {
            rotina.setDataInicio(request.dataInicio());
        }
        if (request.dataFim() != null) {
            rotina.setDataFim(request.dataFim());
        }
        if (request.ativo() != null) {
            rotina.setAtivo(request.ativo());
        }
        if (request.observacoes() != null) {
            rotina.setObservacoes(request.observacoes());
        }

        // Atualiza campos do medicamento se informados
        if (request.dosagem() != null && !request.dosagem().isBlank()) {
            rotina.getMedicamento().setDosagem(request.dosagem().trim());
        }
        if (request.instrucoes() != null) {
            rotina.getMedicamento().setInstrucoes(request.instrucoes().trim());
        }

        RotinaMedicamento atualizada = rotinaMedicamentoRepository.save(rotina);
        log.info("Rotina {} atualizada com sucesso pelo usuário {}", rotinaId, usuario.getEmail());

        return mapearParaResponseDTO(atualizada, LocalDate.now(FUSO_HORARIO_PADRAO));
    }

    /**
     * Remove ou inativa uma rotina de medicamento.
     */
    @Transactional
    public void removerRotina(UUID rotinaId, Usuario usuarioAutenticado) {
        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        RotinaMedicamento rotina = rotinaMedicamentoRepository.findById(rotinaId)
                .orElseThrow(() -> new RotinaMedicamentoNaoEncontradaException(rotinaId));

        rotinaMedicamentoRepository.delete(rotina);
        log.info("Rotina {} removida com sucesso pelo usuário {}", rotinaId, usuario.getEmail());
    }

    /**
     * Lista o histórico de ingestões registradas para uma rotina específica.
     */
    @Transactional(readOnly = true)
    public List<HistoricoMedicacaoResponseDTO> listarHistoricoRotina(UUID rotinaId, Usuario usuarioAutenticado) {
        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        RotinaMedicamento rotina = rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)
                .orElseThrow(() -> new RotinaMedicamentoNaoEncontradaException(rotinaId));

        List<HistoricoMedicacao> historicos = historicoMedicacaoRepository.findByRotinaIdOrderByDataHoraIngestaoDesc(rotinaId);

        return historicos.stream()
                .map(h -> {
                    long diff = h.getDataHoraPrevista() != null
                            ? Duration.between(h.getDataHoraPrevista(), h.getDataHoraIngestao()).toMinutes()
                            : 0L;
                    boolean dentroJanela = h.getStatus() == StatusIngestao.TOMADO;
                    String nomeUsuario = h.getRegistradoPor() != null ? h.getRegistradoPor().getEmail() : "Sistema";

                    return new HistoricoMedicacaoResponseDTO(
                            h.getId(),
                            rotina.getId(),
                            rotina.getMedicamento().getNome(),
                            rotina.getMedicamento().getDosagem(),
                            h.getStatus(),
                            h.getDataHoraIngestao(),
                            h.getDataHoraPrevista(),
                            dentroJanela,
                            diff,
                            h.getObservacao(),
                            nomeUsuario,
                            "Status: " + h.getStatus(),
                            h.getCriadoEm()
                    );
                })
                .toList();
    }

    // --- Métodos Auxiliares ---

    private Medicamento resolverMedicamento(RotinaMedicamentoRequestDTO request) {
        if (request.medicamentoId() != null) {
            return medicamentoRepository.findById(request.medicamentoId())
                    .orElseThrow(() -> new MedicamentoNaoEncontradoException(request.medicamentoId()));
        }

        if (request.nomeMedicamento() == null || request.nomeMedicamento().isBlank()) {
            throw new IllegalArgumentException("O nome do medicamento é obrigatório quando não fornecido o medicamentoId.");
        }
        if (request.dosagem() == null || request.dosagem().isBlank()) {
            throw new IllegalArgumentException("A dosagem do medicamento é obrigatória.");
        }

        String nome = request.nomeMedicamento().trim();
        String dosagem = request.dosagem().trim();

        return medicamentoRepository.findByNomeIgnoreCaseAndDosagemIgnoreCase(nome, dosagem)
                .orElseGet(() -> {
                    Medicamento novo = Medicamento.builder()
                            .nome(nome)
                            .dosagem(dosagem)
                            .instrucoes(request.instrucoes() != null ? request.instrucoes().trim() : null)
                            .build();
                    return medicamentoRepository.save(novo);
                });
    }

    private RotinaMedicamentoResponseDTO mapearParaResponseDTO(RotinaMedicamento rotina, LocalDate dataConsulta) {
        Instant inicioDia = dataConsulta.atStartOfDay(FUSO_HORARIO_PADRAO).toInstant();
        Instant fimDia = dataConsulta.plusDays(1).atStartOfDay(FUSO_HORARIO_PADRAO).toInstant();

        List<HistoricoMedicacao> historicosHoje = historicoMedicacaoRepository
                .findByRotinaIdAndDataHoraIngestaoBetweenOrderByDataHoraIngestaoDesc(rotina.getId(), inicioDia, fimDia);

        StatusIngestao statusHoje = StatusIngestao.PENDENTE;
        Instant horarioIngestaoHoje = null;

        if (!historicosHoje.isEmpty()) {
            HistoricoMedicacao maisRecenteHoje = historicosHoje.get(0);
            statusHoje = maisRecenteHoje.getStatus();
            horarioIngestaoHoje = maisRecenteHoje.getDataHoraIngestao();
        }

        Optional<HistoricoMedicacao> ultimoGeral = historicoMedicacaoRepository
                .findTopByRotinaIdOrderByDataHoraIngestaoDesc(rotina.getId());
        Instant ultimaIngestao = ultimoGeral.map(HistoricoMedicacao::getDataHoraIngestao).orElse(null);

        return new RotinaMedicamentoResponseDTO(
                rotina.getId(),
                rotina.getIdoso().getId(),
                rotina.getIdoso().getNomeCompleto(),
                rotina.getMedicamento().getId(),
                rotina.getMedicamento().getNome(),
                rotina.getMedicamento().getDosagem(),
                rotina.getMedicamento().getInstrucoes(),
                rotina.getHorario(),
                rotina.getFrequencia(),
                rotina.getDiasSemana(),
                rotina.getDataInicio(),
                rotina.getDataFim(),
                rotina.getAtivo(),
                rotina.getObservacoes(),
                statusHoje,
                horarioIngestaoHoje,
                ultimaIngestao,
                rotina.getCriadoEm(),
                rotina.getAtualizadoEm()
        );
    }

    private void validarPerfilAutorizado(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new PerfilNaoAutorizadoException("Apenas perfis autorizados (Familiar ou Médico) podem gerenciar rotinas de medicamentos.");
        }

        boolean ehFamiliar = familiarRepository.existsByUsuarioId(usuario.getId());
        boolean ehMedico = medicoRepository.existsByUsuarioId(usuario.getId());

        if (!ehFamiliar && !ehMedico) {
            log.warn("Acesso negado: usuário {} tentou acessar rotinas de medicamentos sem perfil de Familiar ou Médico.", usuario.getEmail());
            throw new PerfilNaoAutorizadoException("Apenas perfis autorizados (Familiar ou Médico) podem gerenciar rotinas de medicamentos.");
        }
    }

    private Usuario resolverUsuarioAutenticado(Usuario usuarioParam) {
        if (usuarioParam != null && usuarioParam.getId() != null) {
            return usuarioRepository.findById(usuarioParam.getId())
                    .filter(u -> u.getStatus() == StatusUsuario.ATIVO)
                    .orElse(usuarioParam);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof Usuario u && u.getId() != null) {
                return usuarioRepository.findById(u.getId())
                        .filter(user -> user.getStatus() == StatusUsuario.ATIVO)
                        .orElse(u);
            }

            String email = authentication.getName();
            if (email != null && !email.isBlank()) {
                return usuarioRepository.findByEmail(email)
                        .filter(u -> u.getStatus() == StatusUsuario.ATIVO)
                        .orElseThrow(() -> new PerfilNaoAutorizadoException("Usuário autenticado não encontrado ou inativo no sistema."));
            }
        }

        throw new PerfilNaoAutorizadoException("Usuário não autenticado. Forneça um token JWT válido.");
    }
}
