package com.seniorinsight.backendcore.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.seniorinsight.backendcore.domain.AlarmeEmergencia;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.StatusAlarme;
import com.seniorinsight.backendcore.dto.AlarmeEmergenciaResponseDTO;
import com.seniorinsight.backendcore.dto.AtualizarStatusAlarmeRequestDTO;
import com.seniorinsight.backendcore.dto.DispararSosRequestDTO;
import com.seniorinsight.backendcore.event.SosAcionadoEvent;
import com.seniorinsight.backendcore.exception.AlarmeNaoEncontradoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.repository.AlarmeEmergenciaRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AlarmeEmergenciaServiceTest {

    @Mock
    private AlarmeEmergenciaRepository alarmeEmergenciaRepository;

    @Mock
    private IdosoRepository idosoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AlarmeEmergenciaService alarmeEmergenciaService;

    private UUID idosoId;
    private UUID alarmeId;
    private Idoso mockIdoso;
    private AlarmeEmergencia mockAlarme;
    private UsuarioAutenticado mockUsuario;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();
        alarmeId = UUID.randomUUID();

        mockIdoso = Idoso.builder()
                .id(idosoId)
                .nomeCompleto("Sebastião da Silva")
                .dataNascimento(LocalDate.of(1948, 5, 20))
                .tipoSanguineo("O+")
                .alergias("Penicilina")
                .observacoesSocorros("Portador de marca-passo cardíaco.")
                .build();

        mockAlarme = AlarmeEmergencia.builder()
                .id(alarmeId)
                .idoso(mockIdoso)
                .latitude(-23.550520)
                .longitude(-46.633308)
                .nivelBateria(85)
                .status(StatusAlarme.DISPARADO)
                .observacao("Disparo de emergência móvel")
                .build();

        mockUsuario = new UsuarioAutenticado(UUID.randomUUID().toString(), "idoso@seniorinsight.com", null, List.of());
    }

    // =========================================================================
    // dispararAlarme (HU35 - SLA < 100ms)
    // =========================================================================

    @Test
    @DisplayName("Deve disparar alarme SOS com sucesso, persistir com status DISPARADO e publicar SosAcionadoEvent")
    void deveDispararAlarmeComSucessoEPublicarEvento() {
        DispararSosRequestDTO request = new DispararSosRequestDTO(-23.550520, -46.633308, 85, "Disparo de emergência móvel");

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(alarmeEmergenciaRepository.save(any(AlarmeEmergencia.class))).thenAnswer(invocation -> {
            AlarmeEmergencia a = invocation.getArgument(0);
            a.setId(alarmeId);
            return a;
        });

        long start = System.currentTimeMillis();
        AlarmeEmergenciaResponseDTO response = alarmeEmergenciaService.dispararAlarme(idosoId, request, mockUsuario);
        long duration = System.currentTimeMillis() - start;

        // Validação do cumprimento de SLA
        assertThat(duration).isLessThan(100);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(alarmeId);
        assertThat(response.idosoId()).isEqualTo(idosoId);
        assertThat(response.nomeIdoso()).isEqualTo("Sebastião da Silva");
        assertThat(response.status()).isEqualTo(StatusAlarme.DISPARADO);
        assertThat(response.latitude()).isEqualTo(-23.550520);
        assertThat(response.longitude()).isEqualTo(-46.633308);
        assertThat(response.nivelBateria()).isEqualTo(85);

        // Verifica que o evento assíncrono SosAcionadoEvent foi publicado
        ArgumentCaptor<SosAcionadoEvent> eventCaptor = ArgumentCaptor.forClass(SosAcionadoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        SosAcionadoEvent eventoPublicado = eventCaptor.getValue();
        assertThat(eventoPublicado.alarmeId()).isEqualTo(alarmeId);
        assertThat(eventoPublicado.idosoId()).isEqualTo(idosoId);
        assertThat(eventoPublicado.nomeIdoso()).isEqualTo("Sebastião da Silva");
        assertThat(eventoPublicado.latitude()).isEqualTo(-23.550520);
        assertThat(eventoPublicado.longitude()).isEqualTo(-46.633308);
        assertThat(eventoPublicado.nivelBateria()).isEqualTo(85);
    }

    @Test
    @DisplayName("Deve lançar IdosoNaoEncontradoException ao tentar disparar SOS para idoso inexistente")
    void deveLancarExcecaoQuandoIdosoNaoEncontradoAoDisparar() {
        DispararSosRequestDTO request = new DispararSosRequestDTO(-23.550520, -46.633308, 50, null);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmeEmergenciaService.dispararAlarme(idosoId, request, mockUsuario))
                .isInstanceOf(IdosoNaoEncontradoException.class)
                .hasMessageContaining(idosoId.toString());

        verify(alarmeEmergenciaRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando idosoId for nulo")
    void deveLancarExcecaoQuandoIdosoIdNulo() {
        DispararSosRequestDTO request = new DispararSosRequestDTO(-23.550520, -46.633308, 50, null);

        assertThatThrownBy(() -> alarmeEmergenciaService.dispararAlarme(null, request, mockUsuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser nulo");
    }

    // =========================================================================
    // atualizarStatus (HU35)
    // =========================================================================

    @Test
    @DisplayName("Deve atualizar status do alarme de DISPARADO para EM_ATENDIMENTO com sucesso")
    void deveAtualizarStatusParaEmAtendimento() {
        AtualizarStatusAlarmeRequestDTO request = new AtualizarStatusAlarmeRequestDTO(
                StatusAlarme.EM_ATENDIMENTO, "Equipe médica a caminho"
        );

        when(alarmeEmergenciaRepository.findById(alarmeId)).thenReturn(Optional.of(mockAlarme));
        when(alarmeEmergenciaRepository.save(any(AlarmeEmergencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlarmeEmergenciaResponseDTO response = alarmeEmergenciaService.atualizarStatus(alarmeId, request, mockUsuario);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusAlarme.EM_ATENDIMENTO);
        assertThat(response.observacao()).isEqualTo("Equipe médica a caminho");
        verify(alarmeEmergenciaRepository).save(mockAlarme);
    }

    @Test
    @DisplayName("Deve atualizar status do alarme para RESOLVIDO com sucesso")
    void deveAtualizarStatusParaResolvido() {
        AtualizarStatusAlarmeRequestDTO request = new AtualizarStatusAlarmeRequestDTO(
                StatusAlarme.RESOLVIDO, "Idoso atendido e estabilizado"
        );

        when(alarmeEmergenciaRepository.findById(alarmeId)).thenReturn(Optional.of(mockAlarme));
        when(alarmeEmergenciaRepository.save(any(AlarmeEmergencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlarmeEmergenciaResponseDTO response = alarmeEmergenciaService.atualizarStatus(alarmeId, request, mockUsuario);

        assertThat(response.status()).isEqualTo(StatusAlarme.RESOLVIDO);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao tentar alterar alarme que já está RESOLVIDO")
    void deveLancarExcecaoAoAlterarAlarmeJaResolvido() {
        mockAlarme.setStatus(StatusAlarme.RESOLVIDO);
        AtualizarStatusAlarmeRequestDTO request = new AtualizarStatusAlarmeRequestDTO(
                StatusAlarme.EM_ATENDIMENTO, "Tentativa de reabrir"
        );

        when(alarmeEmergenciaRepository.findById(alarmeId)).thenReturn(Optional.of(mockAlarme));

        assertThatThrownBy(() -> alarmeEmergenciaService.atualizarStatus(alarmeId, request, mockUsuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já se encontra finalizado");

        verify(alarmeEmergenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar AlarmeNaoEncontradoException ao tentar atualizar alarme inexistente")
    void deveLancarExcecaoQuandoAlarmeNaoEncontradoAoAtualizar() {
        AtualizarStatusAlarmeRequestDTO request = new AtualizarStatusAlarmeRequestDTO(StatusAlarme.CANCELADO, "Alarme falso");
        when(alarmeEmergenciaRepository.findById(alarmeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmeEmergenciaService.atualizarStatus(alarmeId, request, mockUsuario))
                .isInstanceOf(AlarmeNaoEncontradoException.class)
                .hasMessageContaining(alarmeId.toString());
    }

    // =========================================================================
    // buscarPorId & listarPorIdoso
    // =========================================================================

    @Test
    @DisplayName("Deve buscar alarme por ID com sucesso")
    void deveBuscarAlarmePorIdComSucesso() {
        when(alarmeEmergenciaRepository.findById(alarmeId)).thenReturn(Optional.of(mockAlarme));

        AlarmeEmergenciaResponseDTO response = alarmeEmergenciaService.buscarPorId(alarmeId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(alarmeId);
        assertThat(response.status()).isEqualTo(StatusAlarme.DISPARADO);
    }

    @Test
    @DisplayName("Deve listar histórico de alarmes do idoso")
    void deveListarAlarmesDoIdoso() {
        when(idosoRepository.existsById(idosoId)).thenReturn(true);
        when(alarmeEmergenciaRepository.findByIdosoIdOrderByCriadoEmDesc(idosoId)).thenReturn(List.of(mockAlarme));

        List<AlarmeEmergenciaResponseDTO> lista = alarmeEmergenciaService.listarPorIdoso(idosoId);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).id()).isEqualTo(alarmeId);
    }
}
