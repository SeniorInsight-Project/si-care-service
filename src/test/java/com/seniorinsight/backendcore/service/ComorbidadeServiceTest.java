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
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.repository.IdosoRepository;

@ExtendWith(MockitoExtension.class)
class ComorbidadeServiceTest {

    @Mock
    private IdosoRepository idosoRepository;

    @Mock
    private AcessoIdosoService acessoIdosoService;

    @InjectMocks
    private ComorbidadeService comorbidadeService;

    private UsuarioAutenticado usuarioFamiliar;
    private UsuarioAutenticado usuarioMedico;
    private UsuarioAutenticado usuarioSemPerfil;
    private Idoso idoso;
    private UUID idosoId;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();

        usuarioFamiliar = new UsuarioAutenticado(UUID.randomUUID().toString(), "familiar@seniorinsight.com", null, List.of());

        usuarioMedico = new UsuarioAutenticado(UUID.randomUUID().toString(), "medico@seniorinsight.com", null, List.of());

        usuarioSemPerfil = new UsuarioAutenticado(UUID.randomUUID().toString(), "semperfil@seniorinsight.com", null, List.of());

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
        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenReturn(idoso);
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
        when(acessoIdosoService.exigirVinculo(usuarioMedico, idosoId)).thenReturn(idoso);
        when(idosoRepository.save(any(Idoso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Insuficiência Cardíaca"));

        ComorbidadesResponseDTO response = comorbidadeService.adicionarComorbidades(idosoId, request, usuarioMedico);

        assertThat(response.comorbidades()).contains("Insuficiência Cardíaca");
        verify(idosoRepository).save(idoso);
    }

    @Test
    @DisplayName("Deve lançar PerfilNaoAutorizadoException quando usuário não for Familiar nem Médico")
    void deveLancarPerfilNaoAutorizadoQuandoUsuarioNaoTiverPerfil() {
        when(acessoIdosoService.exigirVinculo(usuarioSemPerfil, idosoId))
                .thenThrow(new PerfilNaoAutorizadoException("Você não faz parte da rede de proteção deste idoso."));

        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Diabetes"));

        assertThatThrownBy(() -> comorbidadeService.adicionarComorbidades(idosoId, request, usuarioSemPerfil))
                .isInstanceOf(PerfilNaoAutorizadoException.class)
                .hasMessageContaining("rede de proteção");

        verify(idosoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar IdosoNaoEncontradoException quando o idoso não existir")
    void deveLancarIdosoNaoEncontradoAoAdicionarComorbidade() {
        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenThrow(new IdosoNaoEncontradoException(idosoId));

        ComorbidadeRequestDTO request = new ComorbidadeRequestDTO(List.of("Diabetes"));

        assertThatThrownBy(() -> comorbidadeService.adicionarComorbidades(idosoId, request, usuarioFamiliar))
                .isInstanceOf(IdosoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve lançar ComorbidadeJaCadastradaException quando todas as comorbidades informadas já existirem")
    void deveLancarComorbidadeJaCadastradaQuandoJaExistir() {
        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenReturn(idoso);

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
        when(acessoIdosoService.exigirVinculo(usuarioMedico, idosoId)).thenReturn(idoso);
        when(idosoRepository.save(any(Idoso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComorbidadesResponseDTO response = comorbidadeService.removerComorbidade(idosoId, "hipertensão arterial", usuarioMedico);

        assertThat(response).isNotNull();
        assertThat(response.comorbidades()).isEmpty();
        verify(idosoRepository).save(idoso);
    }

    @Test
    @DisplayName("Deve lançar ComorbidadeNaoEncontradaException quando comorbidade a ser removida não constar no idoso")
    void deveLancarComorbidadeNaoEncontradaQuandoNaoExistirNaLista() {
        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenReturn(idoso);

        assertThatThrownBy(() -> comorbidadeService.removerComorbidade(idosoId, "Osteoporose", usuarioFamiliar))
                .isInstanceOf(ComorbidadeNaoEncontradaException.class)
                .hasMessageContaining("Osteoporose");

        verify(idosoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar PerfilNaoAutorizadoException ao tentar remover sem perfil de Familiar ou Médico")
    void deveLancarPerfilNaoAutorizadoAoRemoverComorbidade() {
        when(acessoIdosoService.exigirVinculo(usuarioSemPerfil, idosoId))
                .thenThrow(new PerfilNaoAutorizadoException("Você não faz parte da rede de proteção deste idoso."));

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
        when(acessoIdosoService.exigirLeitura(usuarioFamiliar, idosoId)).thenReturn(idoso);

        ComorbidadesResponseDTO response = comorbidadeService.listarComorbidades(idosoId, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.comorbidades()).containsExactly("Hipertensão Arterial");
    }

    @Test
    @DisplayName("Sem vínculo com o idoso, listar é negado (403) e nada é lido")
    void deveNegarListagemSemVinculo() {
        when(acessoIdosoService.exigirLeitura(usuarioSemPerfil, idosoId))
                .thenThrow(new PerfilNaoAutorizadoException("Você não faz parte da rede de proteção deste idoso."));

        assertThatThrownBy(() -> comorbidadeService.listarComorbidades(idosoId, usuarioSemPerfil))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("A escrita exige vínculo de rede ou médico, não o aparelho do idoso (exigirVinculo, não exigirLeitura)")
    void escritaUsaExigirVinculo() {
        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenReturn(idoso);
        when(idosoRepository.save(any(Idoso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        comorbidadeService.adicionarComorbidades(idosoId, new ComorbidadeRequestDTO(List.of("Artrose")), usuarioFamiliar);
        comorbidadeService.removerComorbidade(idosoId, "Artrose", usuarioFamiliar);

        verify(acessoIdosoService, org.mockito.Mockito.times(2)).exigirVinculo(usuarioFamiliar, idosoId);
        verify(acessoIdosoService, never()).exigirLeitura(any(), any());
    }
}
