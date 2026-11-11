package com.seniorinsight.backendcore.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.seniorinsight.backendcore.domain.ConviteRede;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.Medico;
import com.seniorinsight.backendcore.domain.StatusConvite;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.dto.AceiteConviteResponseDTO;
import com.seniorinsight.backendcore.dto.ConvitePublicoDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeRequestDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeResponseDTO;
import com.seniorinsight.backendcore.dto.MembroRedeResponseDTO;
import com.seniorinsight.backendcore.exception.ConviteExpiradoException;
import com.seniorinsight.backendcore.exception.ConviteInvalidoException;
import com.seniorinsight.backendcore.exception.ConviteJaAceitoException;
import com.seniorinsight.backendcore.exception.ConviteNaoEncontradoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.exception.VinculoJaExistenteException;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.repository.ConviteRedeRepository;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

@ExtendWith(MockitoExtension.class)
class RedeProtecaoServiceTest {

    @Mock
    private ConviteRedeRepository conviteRedeRepository;

    @Mock
    private VinculoRedeRepository vinculoRedeRepository;

    @Mock
    private AcessoIdosoService acessoIdosoService;

    @Mock
    private FamiliarRepository familiarRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private SimuladorEmailService simuladorEmailService;

    @InjectMocks
    private RedeProtecaoService redeProtecaoService;

    private UUID idosoId;
    private Idoso idoso;
    private UsuarioAutenticado usuarioFamiliar;
    private Familiar familiar;
    private UsuarioAutenticado usuarioConvidado;
    private Familiar familiarConvidado;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();

        idoso = Idoso.builder()
                .id(idosoId)
                .nomeCompleto("Dona Sebastiana")
                .dataNascimento(LocalDate.of(1940, 10, 12))
                .build();

        usuarioFamiliar = new UsuarioAutenticado(UUID.randomUUID().toString(), "filho@seniorinsight.com", null, List.of());

        familiar = new Familiar(UUID.randomUUID(), usuarioFamiliar.keycloakSub(), "Carlos Silva", "(11) 98888-7777", "123.456.789-00");
        familiar.setEmail(usuarioFamiliar.email());

        usuarioConvidado = new UsuarioAutenticado(UUID.randomUUID().toString(), "neta@seniorinsight.com", null, List.of());

        familiarConvidado = new Familiar(UUID.randomUUID(), usuarioConvidado.keycloakSub(), "Beatriz Silva", "(11) 97777-6666", "987.654.321-11");
        familiarConvidado.setEmail(usuarioConvidado.email());
    }

    // =========================================================================
    // Testes de Geração de Convites (POST /idosos/{idosoId}/convites)
    // =========================================================================

    @Test
    @DisplayName("Deve gerar convite com sucesso para familiar e simular e-mail")
    void deveGerarConviteComSucesso() {
        ConviteRedeRequestDTO request = new ConviteRedeRequestDTO("neta@seniorinsight.com", "FAMILIAR", "Neta", "Olá!");

        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenReturn(idoso);
        when(conviteRedeRepository.findByIdosoIdAndEmailAndStatus(idosoId, "neta@seniorinsight.com", StatusConvite.PENDENTE))
                .thenReturn(Optional.empty());

        when(conviteRedeRepository.save(any(ConviteRede.class))).thenAnswer(invocation -> {
            ConviteRede c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ConviteRedeResponseDTO response = redeProtecaoService.gerarConvite(idosoId, request, usuarioFamiliar);

        assertThat(response).isNotNull();
        assertThat(response.idosoId()).isEqualTo(idosoId);
        assertThat(response.emailDestinatario()).isEqualTo("neta@seniorinsight.com");
        assertThat(response.papelAcessoSugerido()).isEqualTo("FAMILIAR");
        assertThat(response.token()).isNotBlank();
        assertThat(response.status()).isEqualTo(StatusConvite.PENDENTE);
        assertThat(response.dataExpiracao()).isAfter(Instant.now());

        verify(simuladorEmailService).simularEnvioConvite(
                eq("neta@seniorinsight.com"),
                eq("Dona Sebastiana"),
                any(),
                eq("FAMILIAR"),
                any(),
                eq("Olá!")
        );
    }

    @Test
    @DisplayName("Deve lançar IdosoNaoEncontradoException ao tentar convidar para idoso inexistente")
    void deveLancarIdosoNaoEncontradoAoGerarConvite() {
        ConviteRedeRequestDTO request = new ConviteRedeRequestDTO("neta@seniorinsight.com", "FAMILIAR", "Neta", null);

        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenThrow(new IdosoNaoEncontradoException(idosoId));

        assertThatThrownBy(() -> redeProtecaoService.gerarConvite(idosoId, request, usuarioFamiliar))
                .isInstanceOf(IdosoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve lançar VinculoJaExistenteException quando convidado já possui vínculo ativo com o idoso")
    void deveLancarVinculoJaExistenteAoGerarConvite() {
        ConviteRedeRequestDTO request = new ConviteRedeRequestDTO("neta@seniorinsight.com", "FAMILIAR", "Neta", null);

        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenReturn(idoso);
        when(familiarRepository.findByEmailIgnoreCase("neta@seniorinsight.com")).thenReturn(Optional.of(familiarConvidado));
        when(vinculoRedeRepository.existsByFamiliarAndIdoso(familiarConvidado, idoso)).thenReturn(true);

        assertThatThrownBy(() -> redeProtecaoService.gerarConvite(idosoId, request, usuarioFamiliar))
                .isInstanceOf(VinculoJaExistenteException.class);
    }

    // =========================================================================
    // Testes de Aceite de Convite (POST /convites/aceitar?token={token})
    // =========================================================================

    @Test
    @DisplayName("Deve aceitar convite com sucesso e criar vínculo na tabela vinculos_rede")
    void deveAceitarConviteComSucesso() {
        String token = UUID.randomUUID().toString();
        ConviteRede convite = ConviteRede.builder()
                .id(UUID.randomUUID())
                .idoso(idoso)
                .email("neta@seniorinsight.com")
                .token(token)
                .papelAcessoSugerido("CUIDADOR_SECUNDARIO")
                .grauParentescoSugerido("Neta")
                .status(StatusConvite.PENDENTE)
                .dataExpiracao(Instant.now().plus(48, ChronoUnit.HOURS))
                .build();

        when(familiarRepository.findByKeycloakSub(usuarioConvidado.keycloakSub())).thenReturn(Optional.of(familiarConvidado));
        when(conviteRedeRepository.findByTokenWithIdoso(token)).thenReturn(Optional.of(convite));
        when(vinculoRedeRepository.existsByFamiliarAndIdoso(familiarConvidado, idoso)).thenReturn(false);

        when(vinculoRedeRepository.save(any(VinculoRede.class))).thenAnswer(invocation -> {
            VinculoRede v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        AceiteConviteResponseDTO response = redeProtecaoService.aceitarConvite(token, usuarioConvidado);

        assertThat(response).isNotNull();
        assertThat(response.idosoId()).isEqualTo(idosoId);
        assertThat(response.familiarId()).isEqualTo(familiarConvidado.getId());
        assertThat(response.papelAcesso()).isEqualTo("CUIDADOR_SECUNDARIO");
        assertThat(response.grauParentesco()).isEqualTo("Neta");
        assertThat(convite.getStatus()).isEqualTo(StatusConvite.ACEITO);

        verify(vinculoRedeRepository).save(any(VinculoRede.class));
        verify(conviteRedeRepository).save(convite);
    }

    @Test
    @DisplayName("Deve lançar ConviteInvalidoException quando token não existe")
    void deveLancarConviteInvalidoQuandoTokenNaoExiste() {
        String token = "token-inexistente";

        when(familiarRepository.findByKeycloakSub(usuarioConvidado.keycloakSub())).thenReturn(Optional.of(familiarConvidado));
        when(conviteRedeRepository.findByTokenWithIdoso(token)).thenReturn(Optional.empty());
        when(conviteRedeRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> redeProtecaoService.aceitarConvite(token, usuarioConvidado))
                .isInstanceOf(ConviteInvalidoException.class);
    }

    @Test
    @DisplayName("Deve lançar ConviteExpiradoException quando o convite tiver expirado (>48h)")
    void deveLancarConviteExpiradoQuandoPassar48h() {
        String token = UUID.randomUUID().toString();
        ConviteRede convite = ConviteRede.builder()
                .id(UUID.randomUUID())
                .idoso(idoso)
                .email("neta@seniorinsight.com")
                .token(token)
                .papelAcessoSugerido("FAMILIAR")
                .status(StatusConvite.PENDENTE)
                .dataExpiracao(Instant.now().minus(2, ChronoUnit.HOURS)) // Expirado!
                .build();

        when(familiarRepository.findByKeycloakSub(usuarioConvidado.keycloakSub())).thenReturn(Optional.of(familiarConvidado));
        when(conviteRedeRepository.findByTokenWithIdoso(token)).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> redeProtecaoService.aceitarConvite(token, usuarioConvidado))
                .isInstanceOf(ConviteExpiradoException.class);

        assertThat(convite.getStatus()).isEqualTo(StatusConvite.EXPIRADO);
        verify(conviteRedeRepository).save(convite);
    }

    // =========================================================================
    // Testes de Listagem da Rede (GET /idosos/{idosoId}/rede)
    // =========================================================================

    @Test
    @DisplayName("Deve listar todos os membros da rede convertendo para DTO amigável")
    void deveListarMembrosRedeComSucesso() {
        VinculoRede vinculo = VinculoRede.builder()
                .id(UUID.randomUUID())
                .familiar(familiar)
                .idoso(idoso)
                .grauParentesco("Filho")
                .papelAcesso("CUIDADOR_PRINCIPAL")
                .restricoesAcesso(new HashMap<>())
                .build();

        when(vinculoRedeRepository.findByIdosoIdWithFamiliar(idosoId)).thenReturn(List.of(vinculo));

        List<MembroRedeResponseDTO> membros = redeProtecaoService.listarMembrosRede(idosoId, usuarioFamiliar);

        assertThat(membros).hasSize(1);
        MembroRedeResponseDTO dto = membros.get(0);
        assertThat(dto.usuarioId()).isEqualTo(usuarioFamiliar.keycloakSub());
        assertThat(dto.nomeCompleto()).isEqualTo("Carlos Silva");
        assertThat(dto.email()).isEqualTo("filho@seniorinsight.com");
        assertThat(dto.grauParentesco()).isEqualTo("Filho");
        assertThat(dto.papelAcesso()).isEqualTo("CUIDADOR_PRINCIPAL");
    }

    // =========================================================================
    // Autorização por idoso (sem vínculo com o idoso)
    // =========================================================================

    @Test
    @DisplayName("Sem vínculo com o idoso, gerar convite é negado: nada é gravado e nenhum e-mail é simulado")
    void semVinculoNaoGeraConvite() {
        ConviteRedeRequestDTO request = new ConviteRedeRequestDTO("neta@seniorinsight.com", "FAMILIAR", "Neta", null);
        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId))
                .thenThrow(new PerfilNaoAutorizadoException("Você não faz parte da rede de proteção deste idoso."));

        assertThatThrownBy(() -> redeProtecaoService.gerarConvite(idosoId, request, usuarioFamiliar))
                .isInstanceOf(PerfilNaoAutorizadoException.class);

        verify(conviteRedeRepository, never()).save(any());
        verifyNoInteractions(simuladorEmailService);
    }

    @Test
    @DisplayName("Sem vínculo com o idoso, listar a rede é negado e a rede não é consultada")
    void semVinculoNaoListaRede() {
        when(acessoIdosoService.exigirLeitura(usuarioFamiliar, idosoId))
                .thenThrow(new PerfilNaoAutorizadoException("Você não faz parte da rede de proteção deste idoso."));

        assertThatThrownBy(() -> redeProtecaoService.listarMembrosRede(idosoId, usuarioFamiliar))
                .isInstanceOf(PerfilNaoAutorizadoException.class);

        verify(vinculoRedeRepository, never()).findByIdosoIdWithFamiliar(any());
    }

    @Test
    @DisplayName("Convite com papel Administrador é recusado (RN-012), em qualquer caixa, e nada é gravado")
    void conviteNaoConcedeAdministrador() {
        when(acessoIdosoService.exigirVinculo(usuarioFamiliar, idosoId)).thenReturn(idoso);

        for (String papel : List.of("ADMINISTRADOR", "administrador", " Administrador ")) {
            ConviteRedeRequestDTO request = new ConviteRedeRequestDTO("neta@seniorinsight.com", papel, "Neta", null);
            assertThatThrownBy(() -> redeProtecaoService.gerarConvite(idosoId, request, usuarioFamiliar))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Administrador");
        }

        verify(conviteRedeRepository, never()).save(any());
        verifyNoInteractions(simuladorEmailService);
    }

    @Test
    @DisplayName("Convite antigo com papel Administrador não vira vínculo ao ser aceito")
    void aceiteDeConviteAntigoDeAdministradorEhRecusado() {
        ConviteRede convite = ConviteRede.builder().idoso(idoso).email("neta@seniorinsight.com")
                .token("tok").papelAcessoSugerido("ADMINISTRADOR").status(StatusConvite.PENDENTE)
                .dataExpiracao(Instant.now().plusSeconds(3600)).build();
        when(familiarRepository.findByKeycloakSub(usuarioConvidado.keycloakSub())).thenReturn(Optional.of(familiarConvidado));
        when(conviteRedeRepository.findByTokenWithIdoso("tok")).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> redeProtecaoService.aceitarConvite("tok", usuarioConvidado))
                .isInstanceOf(ConviteInvalidoException.class);

        verify(vinculoRedeRepository, never()).save(any());
    }

    // =========================================================================
    // Consulta do convite pelo token (GET /convites?token=)
    // =========================================================================

    private ConviteRede conviteDe(StatusConvite status, Instant expiraEm) {
        return ConviteRede.builder().idoso(idoso).email("neta@seniorinsight.com").token("tok")
                .papelAcessoSugerido("CUIDADOR").grauParentescoSugerido("Neta").status(status)
                .dataExpiracao(expiraEm).criadoPorSub(usuarioFamiliar.keycloakSub()).build();
    }

    @Test
    @DisplayName("Consulta devolve só os campos públicos, com o nome do convidante e o primeiro nome do idoso")
    void consultaDevolveConvitePublico() {
        Instant expiraEm = Instant.now().plusSeconds(3600);
        when(conviteRedeRepository.findByTokenWithIdoso("tok"))
                .thenReturn(Optional.of(conviteDe(StatusConvite.PENDENTE, expiraEm)));
        when(familiarRepository.findByKeycloakSub(usuarioFamiliar.keycloakSub())).thenReturn(Optional.of(familiar));

        ConvitePublicoDTO dto = redeProtecaoService.consultarConvite(" tok ", usuarioConvidado);

        assertThat(dto.nomeConvidante()).isEqualTo("Carlos Silva");
        assertThat(dto.primeiroNomeIdoso()).isEqualTo("Dona");
        assertThat(dto.papelAcesso()).isEqualTo("CUIDADOR");
        assertThat(dto.grauParentesco()).isEqualTo("Neta");
        assertThat(dto.expiraEm()).isEqualTo(expiraEm);
        assertThat(dto.status()).isEqualTo(StatusConvite.PENDENTE);
        verify(conviteRedeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Consulta usa o nome do médico quando quem convidou é médico")
    void consultaUsaNomeDoMedico() {
        Medico medico = new Medico();
        medico.setNomeCompleto("Dra. Helena Souza");
        when(conviteRedeRepository.findByTokenWithIdoso("tok"))
                .thenReturn(Optional.of(conviteDe(StatusConvite.PENDENTE, Instant.now().plusSeconds(3600))));
        when(familiarRepository.findByKeycloakSub(usuarioFamiliar.keycloakSub())).thenReturn(Optional.empty());
        when(medicoRepository.findByKeycloakSub(usuarioFamiliar.keycloakSub())).thenReturn(Optional.of(medico));

        assertThat(redeProtecaoService.consultarConvite("tok", usuarioConvidado).nomeConvidante())
                .isEqualTo("Dra. Helena Souza");
    }

    @Test
    @DisplayName("Consulta usa o texto padrão quando o convidante não é encontrado")
    void consultaUsaNomePadraoSemConvidante() {
        ConviteRede convite = conviteDe(StatusConvite.PENDENTE, Instant.now().plusSeconds(3600));
        convite.setCriadoPorSub(null);
        when(conviteRedeRepository.findByTokenWithIdoso("tok")).thenReturn(Optional.of(convite));

        assertThat(redeProtecaoService.consultarConvite("tok", usuarioConvidado).nomeConvidante())
                .isEqualTo("Um familiar do idoso");
    }

    @Test
    @DisplayName("Consulta com token inexistente lança ConviteNaoEncontradoException")
    void consultaInexistente() {
        when(conviteRedeRepository.findByTokenWithIdoso("tok")).thenReturn(Optional.empty());
        when(conviteRedeRepository.findByToken("tok")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> redeProtecaoService.consultarConvite("tok", usuarioConvidado))
                .isInstanceOf(ConviteNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Consulta de convite aceito lança ConviteJaAceitoException")
    void consultaJaAceito() {
        when(conviteRedeRepository.findByTokenWithIdoso("tok"))
                .thenReturn(Optional.of(conviteDe(StatusConvite.ACEITO, Instant.now().plusSeconds(3600))));

        assertThatThrownBy(() -> redeProtecaoService.consultarConvite("tok", usuarioConvidado))
                .isInstanceOf(ConviteJaAceitoException.class);
    }

    @Test
    @DisplayName("Consulta de convite expirado ou cancelado lança ConviteExpiradoException e não altera o convite")
    void consultaExpiradoOuCancelado() {
        when(conviteRedeRepository.findByTokenWithIdoso("expirado"))
                .thenReturn(Optional.of(conviteDe(StatusConvite.PENDENTE, Instant.now().minusSeconds(60))));
        when(conviteRedeRepository.findByTokenWithIdoso("cancelado"))
                .thenReturn(Optional.of(conviteDe(StatusConvite.CANCELADO, Instant.now().plusSeconds(3600))));

        assertThatThrownBy(() -> redeProtecaoService.consultarConvite("expirado", usuarioConvidado))
                .isInstanceOf(ConviteExpiradoException.class).hasMessage("Convite expirado");
        assertThatThrownBy(() -> redeProtecaoService.consultarConvite("cancelado", usuarioConvidado))
                .isInstanceOf(ConviteExpiradoException.class).hasMessage("Convite cancelado");

        verify(conviteRedeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Consulta não exige perfil de Familiar nem vínculo com o idoso")
    void consultaNaoExigeVinculo() {
        when(conviteRedeRepository.findByTokenWithIdoso("tok"))
                .thenReturn(Optional.of(conviteDe(StatusConvite.PENDENTE, Instant.now().plusSeconds(3600))));

        redeProtecaoService.consultarConvite("tok", usuarioConvidado);

        verifyNoInteractions(acessoIdosoService);
        verify(familiarRepository, never()).findByKeycloakSub(usuarioConvidado.keycloakSub());
    }
}
