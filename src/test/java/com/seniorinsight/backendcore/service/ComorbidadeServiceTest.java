package com.seniorinsight.backendcore.service;

import java.time.LocalDate;
import java.util.ArrayList;
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

import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.dto.ComorbidadeRequestDTO;
import com.seniorinsight.backendcore.dto.ComorbidadesResponseDTO;
import com.seniorinsight.backendcore.exception.ComorbidadeJaCadastradaException;
import com.seniorinsight.backendcore.exception.ComorbidadeNaoEncontradaException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.model.StatusUsuario;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ComorbidadeServiceTest {

    @Mock
    private IdosoRepository idosoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FamiliarRepository familiarRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @InjectMocks
    private ComorbidadeService comorbidadeService;

    private Usuario usuarioFamiliar;
    private Usuario usuarioMedico;
    private Usuario usuarioSemPerfil;
    private Idoso idoso;
    private UUID idosoId;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();

        usuarioFamiliar = Usuario.builder()
                .id(UUID.randomUUID())
                .email("familiar@seniorinsight.com")
                .status(StatusUsuario.ATIVO)
                .build();

        usuarioMedico = Usuario.builder()
                .id(UUID.randomUUID())
                .email("medico@seniorinsight.com")
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
                .comorbidades(new ArrayList<>(List.of("Hipertensão Arterial")))
                .build();
    }

    // =========================================================================
    // Testes de Adicionar Comorbidades (POST)
    // =========================================================================

    @Test
    @DisplayName("Deve adicionar comorbidades com sucesso quando usuário for Familiar")
    void deveAdicionarComorbidadesPorFamiliarComSucesso() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));
        when(idosoRepository.save(any(Idoso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Diabetes Mellitus Tipo 2", "Artrose"));

        ComorbidadesResponseDTO response = comorbidadeService.adicionarComorbidades(idosoId, request, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.idosoId()).isEqualTo(idosoId);
        assertThat(response.comorbidades()).containsExactlyInAnyOrder("Hipertensão Arterial", "Diabetes Mellitus Tipo 2", "Artrose");
        verify(idosoRepository).save(idoso);
    }

    @Test
    @DisplayName("Deve adicionar comorbidades com sucesso quando usuário for Médico")
    void deveAdicionarComorbidadesPorMedicoComSucesso() {
        when(usuarioRepository.findById(usuarioMedico.getId())).thenReturn(Optional.of(usuarioMedico));
        when(familiarRepository.existsByUsuarioId(usuarioMedico.getId())).thenReturn(false);
        when(medicoRepository.existsByUsuarioId(usuarioMedico.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));
        when(idosoRepository.save(any(Idoso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Insuficiência Cardíaca"));

        ComorbidadesResponseDTO response = comorbidadeService.adicionarComorbidades(idosoId, request, usuarioMedico);

        assertThat(response.comorbidades()).contains("Insuficiência Cardíaca");
        verify(idosoRepository).save(idoso);
    }

    @Test
    @DisplayName("Deve lançar PerfilNaoAutorizadoException quando usuário não for Familiar nem Médico")
    void deveLancarPerfilNaoAutorizadoQuandoUsuarioNaoTiverPerfil() {
        when(usuarioRepository.findById(usuarioSemPerfil.getId())).thenReturn(Optional.of(usuarioSemPerfil));
        when(familiarRepository.existsByUsuarioId(usuarioSemPerfil.getId())).thenReturn(false);
        when(medicoRepository.existsByUsuarioId(usuarioSemPerfil.getId())).thenReturn(false);

        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Diabetes"));

        assertThatThrownBy(() -> comorbidadeService.adicionarComorbidades(idosoId, request, usuarioSemPerfil))
                .isInstanceOf(PerfilNaoAutorizadoException.class)
                .hasMessageContaining("Apenas perfis autorizados (Familiar ou Médico)");

        verify(idosoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar IdosoNaoEncontradoException quando o idoso não existir")
    void deveLancarIdosoNaoEncontradoAoAdicionarComorbidade() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.empty());

        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Diabetes"));

        assertThatThrownBy(() -> comorbidadeService.adicionarComorbidades(idosoId, request, usuarioFamiliar))
                .isInstanceOf(IdosoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve lançar ComorbidadeJaCadastradaException quando todas as comorbidades informadas já existirem")
    void deveLancarComorbidadeJaCadastradaQuandoJaExistir() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));

        // Idoso já possui "Hipertensão Arterial"
        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("hipertensão arterial"));

        assertThatThrownBy(() -> comorbidadeService.adicionarComorbidades(idosoId, request, usuarioFamiliar))
                .isInstanceOf(ComorbidadeJaCadastradaException.class)
                .hasMessageContaining("Todas as comorbidades informadas já estão cadastradas");

        verify(idosoRepository, never()).save(any());
    }

    // =========================================================================
    // Testes de Remover Comorbidade (DELETE)
    // =========================================================================

    @Test
    @DisplayName("Deve remover comorbidade com sucesso quando comorbidade existir na lista")
    void deveRemoverComorbidadeComSucesso() {
        when(usuarioRepository.findById(usuarioMedico.getId())).thenReturn(Optional.of(usuarioMedico));
        when(familiarRepository.existsByUsuarioId(usuarioMedico.getId())).thenReturn(false);
        when(medicoRepository.existsByUsuarioId(usuarioMedico.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));
        when(idosoRepository.save(any(Idoso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComorbidadesResponseDTO response = comorbidadeService.removerComorbidade(idosoId, "hipertensão arterial", usuarioMedico);

        assertThat(response).isNotNull();
        assertThat(response.comorbidades()).isEmpty();
        verify(idosoRepository).save(idoso);
    }

    @Test
    @DisplayName("Deve lançar ComorbidadeNaoEncontradaException quando comorbidade a ser removida não constar no idoso")
    void deveLancarComorbidadeNaoEncontradaQuandoNaoExistirNaLista() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));

        assertThatThrownBy(() -> comorbidadeService.removerComorbidade(idosoId, "Osteoporose", usuarioFamiliar))
                .isInstanceOf(ComorbidadeNaoEncontradaException.class)
                .hasMessageContaining("Osteoporose");

        verify(idosoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar PerfilNaoAutorizadoException ao tentar remover sem perfil de Familiar ou Médico")
    void deveLancarPerfilNaoAutorizadoAoRemoverComorbidade() {
        when(usuarioRepository.findById(usuarioSemPerfil.getId())).thenReturn(Optional.of(usuarioSemPerfil));
        when(familiarRepository.existsByUsuarioId(usuarioSemPerfil.getId())).thenReturn(false);
        when(medicoRepository.existsByUsuarioId(usuarioSemPerfil.getId())).thenReturn(false);

        assertThatThrownBy(() -> comorbidadeService.removerComorbidade(idosoId, "Hipertensão Arterial", usuarioSemPerfil))
                .isInstanceOf(PerfilNaoAutorizadoException.class);

        verify(idosoRepository, never()).save(any());
    }

    // =========================================================================
    // Testes de Listar Comorbidades (GET)
    // =========================================================================

    @Test
    @DisplayName("Deve listar comorbidades do idoso com sucesso")
    void deveListarComorbidadesComSucesso() {
        when(usuarioRepository.findById(usuarioFamiliar.getId())).thenReturn(Optional.of(usuarioFamiliar));
        when(familiarRepository.existsByUsuarioId(usuarioFamiliar.getId())).thenReturn(true);
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));

        ComorbidadesResponseDTO response = comorbidadeService.listarComorbidades(idosoId, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.comorbidades()).containsExactly("Hipertensão Arterial");
    }
}
