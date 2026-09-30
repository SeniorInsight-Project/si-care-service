package com.seniorinsight.backendcore.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class RotinaMedicamentoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private RotinaMedicamentoRepository rotinaMedicamentoRepository;

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @Mock
    private HistoricoMedicacaoRepository historicoMedicacaoRepository;

    @Mock
    private IdosoRepository idosoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FamiliarRepository familiarRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @InjectMocks
    private RotinaMedicamentoService rotinaMedicamentoService;

    private Usuario usuarioFamiliar;
    private Usuario usuarioSemPerfil;
    private Idoso idoso;
    private Medicamento medicamento;
    private RotinaMedicamento rotina;
    private UUID idosoId;
    private UUID rotinaId;
    private UUID medicamentoId;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();
        rotinaId = UUID.randomUUID();
        medicamentoId = UUID.randomUUID();

        usuarioFamiliar = Usuario.builder()
                .id(UUID.randomUUID())
                .email("familiar@seniorinsight.com")
                .status(StatusUsuario.ATIVO)
                .build();

        usuarioSemPerfil = Usuario.builder()
                .id(UUID.randomUUID())
                .email("semperfil@seniorinsight.com")
                .status(StatusUsuario.ATIVO)
                .build();

        idoso = Idoso.builder()
                .id(idosoId)
                .nomeCompleto("Sebastião Ferreira")
                .dataNascimento(LocalDate.of(1945, 5, 20))
                .build();

        medicamento = Medicamento.builder()
                .id(medicamentoId)
                .nome("Losartana Potássica")
                .dosagem("50mg")
                .instrucoes("1 comprimido pela manhã em jejum")
                .build();

        rotina = RotinaMedicamento.builder()
                .id(rotinaId)
                .idoso(idoso)
                .medicamento(medicamento)
                .horario(LocalTime.of(8, 0))
                .frequencia("1x ao dia (Diário)")
                .diasSemana("TODOS")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .ativo(true)
                .observacoes("Controle de hipertensão")
                .build();
    }

    // =========================================================================
    // Testes de Criação de Rotina (POST)
    // =========================================================================

    @Test
    @DisplayName("Deve criar rotina cadastrando novo medicamento com sucesso")
    void deveCriarRotinaComNovoMedicamentoComSucesso() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));
        when(medicamentoRepository.findByNomeIgnoreCaseAndDosagemIgnoreCase("Losartana Potássica", "50mg"))
                .thenReturn(Optional.empty());
        when(medicamentoRepository.save(any(Medicamento.class))).thenReturn(medicamento);
        when(rotinaMedicamentoRepository.save(any(RotinaMedicamento.class))).thenAnswer(i -> {
            RotinaMedicamento r = i.getArgument(0);
            r.setId(rotinaId);
            return r;
        });

        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                null,
                "Losartana Potássica",
                "50mg",
                "1 comprimido pela manhã",
                LocalTime.of(8, 0),
                "1x ao dia (Diário)",
                "TODOS",
                LocalDate.of(2026, 10, 1),
                null,
                "Uso contínuo"
        );

        RotinaMedicamentoResponseDTO response = rotinaMedicamentoService.criarRotina(idosoId, request, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.idosoId()).isEqualTo(idosoId);
        assertThat(response.medicamentoNome()).isEqualTo("Losartana Potássica");
        assertThat(response.dosagem()).isEqualTo("50mg");
        assertThat(response.horario()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.statusHoje()).isEqualTo(StatusIngestao.PENDENTE);
        verify(rotinaMedicamentoRepository).save(any(RotinaMedicamento.class));
    }

    @Test
    @DisplayName("Deve criar rotina reutilizando medicamento existente por ID")
    void deveCriarRotinaComMedicamentoExistentePorIdComSucesso() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));
        when(medicamentoRepository.findById(medicamentoId)).thenReturn(Optional.of(medicamento));
        when(rotinaMedicamentoRepository.save(any(RotinaMedicamento.class))).thenAnswer(i -> {
            RotinaMedicamento r = i.getArgument(0);
            r.setId(rotinaId);
            return r;
        });

        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                medicamentoId,
                null,
                null,
                null,
                LocalTime.of(12, 0),
                "1x ao dia",
                "TODOS",
                null,
                null,
                null
        );

        RotinaMedicamentoResponseDTO response = rotinaMedicamentoService.criarRotina(idosoId, request, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.medicamentoId()).isEqualTo(medicamentoId);
        assertThat(response.medicamentoNome()).isEqualTo("Losartana Potássica");
    }

    @Test
    @DisplayName("Deve lançar IdosoNaoEncontradoException ao criar rotina para idoso inexistente")
    void deveLancarIdosoNaoEncontradoAoCriarRotina() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.empty());

        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                null, "Aspirina", "100mg", null, LocalTime.of(8, 0), "Diário", "TODOS", null, null, null
        );

        assertThatThrownBy(() -> rotinaMedicamentoService.criarRotina(idosoId, request, usuarioFamiliar))
                .isInstanceOf(IdosoNaoEncontradoException.class);

        verify(rotinaMedicamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar PerfilNaoAutorizadoException quando usuário não for Familiar nem Médico")
    void deveLancarPerfilNaoAutorizadoAoCriarRotina() {
        when(usuarioRepository.findById(usuarioSemPerfil.getId())).thenReturn(Optional.of(usuarioSemPerfil));
        when(familiarRepository.existsByUsuarioId(usuarioSemPerfil.getId())).thenReturn(false);
        when(medicoRepository.existsByUsuarioId(usuarioSemPerfil.getId())).thenReturn(false);

        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                null, "Aspirina", "100mg", null, LocalTime.of(8, 0), "Diário", "TODOS", null, null, null
        );

        assertThatThrownBy(() -> rotinaMedicamentoService.criarRotina(idosoId, request, usuarioSemPerfil))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("Deve lançar MedicamentoNaoEncontradoException quando ID de medicamento não existir")
    void deveLancarMedicamentoNaoEncontradoQuandoIdInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));
        when(medicamentoRepository.findById(idInexistente)).thenReturn(Optional.empty());

        RotinaMedicamentoRequestDTO request = new RotinaMedicamentoRequestDTO(
                idInexistente, null, null, null, LocalTime.of(8, 0), "Diário", null, null, null, null
        );

        assertThatThrownBy(() -> rotinaMedicamentoService.criarRotina(idosoId, request, usuarioFamiliar))
                .isInstanceOf(MedicamentoNaoEncontradoException.class);
    }

    // =========================================================================
    // Testes de Listagem Diária (GET)
    // =========================================================================

    @Test
    @DisplayName("Deve listar rotina diária com status PENDENTE quando ainda não houve ingestão hoje")
    void deveListarRotinasDiariasComStatusPendente() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.existsById(idosoId)).thenReturn(true);
        when(rotinaMedicamentoRepository.findByIdosoIdAndAtivoWithDetails(idosoId, true))
                .thenReturn(List.of(rotina));
        when(historicoMedicacaoRepository.findByRotinaIdAndDataHoraIngestaoBetweenOrderByDataHoraIngestaoDesc(
                any(), any(), any())).thenReturn(Collections.emptyList());

        List<RotinaMedicamentoResponseDTO> lista = rotinaMedicamentoService.listarRotinasDiarias(
                idosoId, LocalDate.now(FUSO), true, usuarioFamiliar);

        assertThat(lista).hasSize(1);
        RotinaMedicamentoResponseDTO item = lista.get(0);
        assertThat(item.id()).isEqualTo(rotinaId);
        assertThat(item.statusHoje()).isEqualTo(StatusIngestao.PENDENTE);
        assertThat(item.horario()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    @DisplayName("Deve listar rotina diária com status TOMADO quando ingestão foi registrada hoje")
    void deveListarRotinasDiariasComStatusTomado() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.existsById(idosoId)).thenReturn(true);
        when(rotinaMedicamentoRepository.findByIdosoIdAndAtivoWithDetails(idosoId, true))
                .thenReturn(List.of(rotina));

        Instant agora = Instant.now();
        HistoricoMedicacao historicoHoje = HistoricoMedicacao.builder()
                .id(UUID.randomUUID())
                .rotina(rotina)
                .status(StatusIngestao.TOMADO)
                .dataHoraIngestao(agora)
                .build();

        when(historicoMedicacaoRepository.findByRotinaIdAndDataHoraIngestaoBetweenOrderByDataHoraIngestaoDesc(
                any(), any(), any())).thenReturn(List.of(historicoHoje));
        when(historicoMedicacaoRepository.findTopByRotinaIdOrderByDataHoraIngestaoDesc(rotinaId))
                .thenReturn(Optional.of(historicoHoje));

        List<RotinaMedicamentoResponseDTO> lista = rotinaMedicamentoService.listarRotinasDiarias(
                idosoId, LocalDate.now(FUSO), true, usuarioFamiliar);

        assertThat(lista).hasSize(1);
        RotinaMedicamentoResponseDTO item = lista.get(0);
        assertThat(item.statusHoje()).isEqualTo(StatusIngestao.TOMADO);
        assertThat(item.horarioIngestaoHoje()).isEqualTo(agora);
    }

    // =========================================================================
    // Testes de Confirmação de Ingestão e Janela de Tolerância de 30 min (POST)
    // =========================================================================

    @Test
    @DisplayName("Deve confirmar ingestão com status TOMADO dentro da janela de tolerância de 30 min")
    void deveConfirmarIngestaoDentroDaJanelaDeToleranciaComoTomado() {
        LocalDate dataHoje = LocalDate.of(2026, 10, 1);
        // Horário agendado: 08:00
        // Ingestão às 08:20 (20 minutos após -> dentro da tolerância de 30 min)
        Instant ingestaoInstant = ZonedDateTime.of(dataHoje, LocalTime.of(8, 20), FUSO).toInstant();

        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)).thenReturn(Optional.of(rotina));
        when(historicoMedicacaoRepository.save(any(HistoricoMedicacao.class))).thenAnswer(i -> {
            HistoricoMedicacao h = i.getArgument(0);
            h.setId(UUID.randomUUID());
            return h;
        });

        ConfirmarIngestaoRequestDTO request = new ConfirmarIngestaoRequestDTO(
                ingestaoInstant, dataHoje, "Ingerido no horário", null
        );

        HistoricoMedicacaoResponseDTO response = rotinaMedicamentoService.confirmarIngestao(
                rotinaId, request, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusIngestao.TOMADO);
        assertThat(response.dentroJanelaTolerancia()).isTrue();
        assertThat(response.diferencaMinutos()).isEqualTo(20L);
        assertThat(response.mensagem()).contains("dentro da janela de tolerância de 30 minutos");
        verify(historicoMedicacaoRepository).save(any(HistoricoMedicacao.class));
    }

    @Test
    @DisplayName("Deve confirmar ingestão com status ATRASADO quando exceder a janela de tolerância de 30 min")
    void deveConfirmarIngestaoAposJanelaDeToleranciaComoAtrasado() {
        LocalDate dataHoje = LocalDate.of(2026, 10, 1);
        // Horário agendado: 08:00
        // Ingestão às 08:45 (45 minutos após -> atraso superior a 30 min)
        Instant ingestaoInstant = ZonedDateTime.of(dataHoje, LocalTime.of(8, 45), FUSO).toInstant();

        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)).thenReturn(Optional.of(rotina));
        when(historicoMedicacaoRepository.save(any(HistoricoMedicacao.class))).thenAnswer(i -> {
            HistoricoMedicacao h = i.getArgument(0);
            h.setId(UUID.randomUUID());
            return h;
        });

        ConfirmarIngestaoRequestDTO request = new ConfirmarIngestaoRequestDTO(
                ingestaoInstant, dataHoje, "Esqueceu pela manhã e tomou depois", null
        );

        HistoricoMedicacaoResponseDTO response = rotinaMedicamentoService.confirmarIngestao(
                rotinaId, request, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusIngestao.ATRASADO);
        assertThat(response.dentroJanelaTolerancia()).isFalse();
        assertThat(response.diferencaMinutos()).isEqualTo(45L);
        assertThat(response.mensagem()).contains("Ingestão confirmada com ATRASO");
    }

    @Test
    @DisplayName("Deve registrar status IGNORADO quando o idoso ou cuidador optar por não tomar a medicação")
    void deveRegistrarStatusIgnoradoComSucesso() {
        LocalDate dataHoje = LocalDate.of(2026, 10, 1);
        Instant ingestaoInstant = ZonedDateTime.of(dataHoje, LocalTime.of(8, 0), FUSO).toInstant();

        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)).thenReturn(Optional.of(rotina));
        when(historicoMedicacaoRepository.save(any(HistoricoMedicacao.class))).thenAnswer(i -> {
            HistoricoMedicacao h = i.getArgument(0);
            h.setId(UUID.randomUUID());
            return h;
        });

        ConfirmarIngestaoRequestDTO request = new ConfirmarIngestaoRequestDTO(
                ingestaoInstant, dataHoje, "Idoso recusou a medicação", StatusIngestao.IGNORADO
        );

        HistoricoMedicacaoResponseDTO response = rotinaMedicamentoService.confirmarIngestao(
                rotinaId, request, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusIngestao.IGNORADO);
        assertThat(response.dentroJanelaTolerancia()).isFalse();
        assertThat(response.mensagem()).contains("IGNORADA");
    }

    @Test
    @DisplayName("Deve lançar RotinaMedicamentoNaoEncontradaException ao confirmar em rotina inexistente")
    void deveLancarRotinaNaoEncontradaAoConfirmarIngestao() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rotinaMedicamentoService.confirmarIngestao(
                rotinaId, null, usuarioFamiliar))
                .isInstanceOf(RotinaMedicamentoNaoEncontradaException.class);
    }

    // =========================================================================
    // Testes de Demais Operações CRUD
    // =========================================================================

    @Test
    @DisplayName("Deve obter rotina por ID com sucesso")
    void deveObterRotinaPorIdComSucesso() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)).thenReturn(Optional.of(rotina));

        RotinaMedicamentoResponseDTO response = rotinaMedicamentoService.obterRotinaPorId(rotinaId, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(rotinaId);
        assertThat(response.medicamentoNome()).isEqualTo("Losartana Potássica");
    }

    @Test
    @DisplayName("Deve atualizar rotina de medicamento com sucesso")
    void deveAtualizarRotinaComSucesso() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(rotinaMedicamentoRepository.findByIdWithDetails(rotinaId)).thenReturn(Optional.of(rotina));
        when(rotinaMedicamentoRepository.save(any(RotinaMedicamento.class))).thenAnswer(i -> i.getArgument(0));

        AtualizarRotinaRequestDTO request = new AtualizarRotinaRequestDTO(
                LocalTime.of(9, 30),
                "2x ao dia",
                "TODOS",
                null,
                null,
                true,
                "Horário atualizado",
                "100mg",
                "Tomar após café"
        );

        RotinaMedicamentoResponseDTO response = rotinaMedicamentoService.atualizarRotina(rotinaId, request, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.horario()).isEqualTo(LocalTime.of(9, 30));
        assertThat(response.dosagem()).isEqualTo("100mg");
    }

    @Test
    @DisplayName("Deve remover rotina de medicamento com sucesso")
    void deveRemoverRotinaComSucesso() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(rotinaMedicamentoRepository.findById(rotinaId)).thenReturn(Optional.of(rotina));

        rotinaMedicamentoService.removerRotina(rotinaId, usuarioFamiliar);

        verify(rotinaMedicamentoRepository).delete(rotina);
    }
}
