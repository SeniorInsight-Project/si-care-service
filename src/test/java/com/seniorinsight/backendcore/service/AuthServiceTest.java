package com.seniorinsight.backendcore.service;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Medico;
import com.seniorinsight.backendcore.dto.AuthResponseDTO;
import com.seniorinsight.backendcore.dto.CompletarPerfilRequestDTO;
import com.seniorinsight.backendcore.dto.LoginRequestDTO;
import com.seniorinsight.backendcore.dto.RegistroRequestDTO;
import com.seniorinsight.backendcore.exception.CpfJaCadastradoException;
import com.seniorinsight.backendcore.exception.CrmJaCadastradoException;
import com.seniorinsight.backendcore.exception.CredenciaisInvalidasException;
import com.seniorinsight.backendcore.exception.EmailJaCadastradoException;
import com.seniorinsight.backendcore.exception.PerfilJaExistenteException;
import com.seniorinsight.backendcore.exception.RateLimitExceededException;
import com.seniorinsight.backendcore.model.ProvedorSso;
import com.seniorinsight.backendcore.model.StatusUsuario;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.UsuarioRepository;
import com.seniorinsight.backendcore.security.JwtService;
import com.seniorinsight.backendcore.security.RateLimiterService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FamiliarRepository familiarRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RateLimiterService rateLimiterService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository,
                familiarRepository,
                medicoRepository,
                passwordEncoder,
                jwtService,
                rateLimiterService
        );
    }

    // =========================================================================
    // Testes de autenticarTradicional (HU-002)
    // =========================================================================

    @Test
    @DisplayName("Deve autenticar com sucesso usuário com perfil completo")
    void deveAutenticarTradicionalComSucessoPerfilCompleto() {
        LoginRequestDTO request = new LoginRequestDTO("usuario@seniorinsight.com", "SenhaForte@123");
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@seniorinsight.com")
                .senhaHash("$2a$10$hashSimulado")
                .status(StatusUsuario.ATIVO)
                .build();

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(usuarioRepository.findByEmail("usuario@seniorinsight.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte@123", "$2a$10$hashSimulado")).thenReturn(true);
        when(familiarRepository.existsByUsuario(usuario)).thenReturn(true);
        when(jwtService.generateToken(usuario)).thenReturn("jwt.token.valido");

        AuthResponseDTO response = authService.autenticarTradicional(request);

        assertNotNull(response);
        assertEquals("jwt.token.valido", response.accessToken());
        assertTrue(response.perfilCompleto());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    @DisplayName("Deve autenticar com sucesso usuário com perfil incompleto")
    void deveAutenticarTradicionalComSucessoPerfilIncompleto() {
        LoginRequestDTO request = new LoginRequestDTO("usuario@seniorinsight.com", "SenhaForte@123");
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@seniorinsight.com")
                .senhaHash("$2a$10$hashSimulado")
                .status(StatusUsuario.ATIVO)
                .build();

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(usuarioRepository.findByEmail("usuario@seniorinsight.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte@123", "$2a$10$hashSimulado")).thenReturn(true);
        when(familiarRepository.existsByUsuario(usuario)).thenReturn(false);
        when(medicoRepository.existsByUsuario(usuario)).thenReturn(false);
        when(jwtService.generateToken(usuario)).thenReturn("jwt.token.valido");

        AuthResponseDTO response = authService.autenticarTradicional(request);

        assertNotNull(response);
        assertEquals("jwt.token.valido", response.accessToken());
        assertFalse(response.perfilCompleto());
    }

    @Test
    @DisplayName("Deve lançar RateLimitExceededException quando limite de tentativas for atingido")
    void deveLancarExcecaoQuandoRateLimitExcedido() {
        LoginRequestDTO request = new LoginRequestDTO("usuario@seniorinsight.com", "SenhaForte@123");
        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(false);

        assertThrows(RateLimitExceededException.class, () -> authService.autenticarTradicional(request));
        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Deve lançar CredenciaisInvalidasException quando e-mail não existir")
    void deveLancarExcecaoQuandoEmailNaoExistir() {
        LoginRequestDTO request = new LoginRequestDTO("inexistente@seniorinsight.com", "SenhaForte@123");
        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(usuarioRepository.findByEmail("inexistente@seniorinsight.com")).thenReturn(Optional.empty());

        assertThrows(CredenciaisInvalidasException.class, () -> authService.autenticarTradicional(request));
    }

    @Test
    @DisplayName("Deve lançar CredenciaisInvalidasException quando senha estiver incorreta")
    void deveLancarExcecaoQuandoSenhaIncorreta() {
        LoginRequestDTO request = new LoginRequestDTO("usuario@seniorinsight.com", "SenhaErrada");
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@seniorinsight.com")
                .senhaHash("$2a$10$hashSimulado")
                .status(StatusUsuario.ATIVO)
                .build();

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(usuarioRepository.findByEmail("usuario@seniorinsight.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaErrada", "$2a$10$hashSimulado")).thenReturn(false);

        assertThrows(CredenciaisInvalidasException.class, () -> authService.autenticarTradicional(request));
    }

    @Test
    @DisplayName("Deve lançar CredenciaisInvalidasException quando usuário estiver inativo ou bloqueado")
    void deveLancarExcecaoQuandoUsuarioBloqueado() {
        LoginRequestDTO request = new LoginRequestDTO("usuario@seniorinsight.com", "SenhaForte@123");
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@seniorinsight.com")
                .senhaHash("$2a$10$hashSimulado")
                .status(StatusUsuario.BLOQUEADO)
                .build();

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(usuarioRepository.findByEmail("usuario@seniorinsight.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte@123", "$2a$10$hashSimulado")).thenReturn(true);

        assertThrows(CredenciaisInvalidasException.class, () -> authService.autenticarTradicional(request));
    }

    // =========================================================================
    // Testes de registrarNovoUsuario (HU-002)
    // =========================================================================

    @Test
    @DisplayName("Deve registrar novo usuário Familiar com hash BCrypt e perfil completo")
    void deveRegistrarNovoUsuarioFamiliarComSucesso() {
        RegistroRequestDTO request = new RegistroRequestDTO(
                "novo.familiar@seniorinsight.com",
                "SenhaForte@123",
                "Carlos Silva",
                "123.456.789-00",
                "(11) 98765-4321",
                "FAMILIAR",
                null,
                null
        );

        when(usuarioRepository.existsByEmail("novo.familiar@seniorinsight.com")).thenReturn(false);
        when(familiarRepository.existsByCpf("123.456.789-00")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("$2a$10$hashBCryptGerado");

        Usuario usuarioSalvo = Usuario.builder()
                .id(UUID.randomUUID())
                .email("novo.familiar@seniorinsight.com")
                .senhaHash("$2a$10$hashBCryptGerado")
                .provedorSso(ProvedorSso.LOCAL)
                .build();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);
        when(jwtService.generateToken(usuarioSalvo)).thenReturn("jwt.novo.familiar");

        AuthResponseDTO response = authService.registrarNovoUsuario(request);

        assertNotNull(response);
        assertEquals("jwt.novo.familiar", response.accessToken());
        assertTrue(response.perfilCompleto());
        verify(familiarRepository).save(any(Familiar.class));
        verify(medicoRepository, never()).save(any(Medico.class));
    }

    @Test
    @DisplayName("Deve registrar novo usuário Médico com hash BCrypt e perfil completo")
    void deveRegistrarNovoUsuarioMedicoComSucesso() {
        RegistroRequestDTO request = new RegistroRequestDTO(
                "novo.medico@seniorinsight.com",
                "SenhaForte@123",
                "Dra. Ana Paula",
                null,
                "(11) 3333-4444",
                "MEDICO",
                "123456/SP",
                "Geriatria"
        );

        when(usuarioRepository.existsByEmail("novo.medico@seniorinsight.com")).thenReturn(false);
        when(medicoRepository.existsByCrm("123456/SP")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("$2a$10$hashBCryptGerado");

        Usuario usuarioSalvo = Usuario.builder()
                .id(UUID.randomUUID())
                .email("novo.medico@seniorinsight.com")
                .senhaHash("$2a$10$hashBCryptGerado")
                .provedorSso(ProvedorSso.LOCAL)
                .build();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);
        when(jwtService.generateToken(usuarioSalvo)).thenReturn("jwt.novo.medico");

        AuthResponseDTO response = authService.registrarNovoUsuario(request);

        assertNotNull(response);
        assertEquals("jwt.novo.medico", response.accessToken());
        assertTrue(response.perfilCompleto());
        verify(medicoRepository).save(any(Medico.class));
        verify(familiarRepository, never()).save(any(Familiar.class));
    }

    @Test
    @DisplayName("Deve lançar EmailJaCadastradoException quando email já existir no registro")
    void deveLancarExcecaoQuandoEmailJaCadastradoNoRegistro() {
        RegistroRequestDTO request = new RegistroRequestDTO(
                "existente@seniorinsight.com", "SenhaForte@123", "Nome", "123", "tel", "FAMILIAR", null, null);
        when(usuarioRepository.existsByEmail("existente@seniorinsight.com")).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class, () -> authService.registrarNovoUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar CpfJaCadastradoException quando CPF já existir no registro")
    void deveLancarExcecaoQuandoCpfJaCadastradoNoRegistro() {
        RegistroRequestDTO request = new RegistroRequestDTO(
                "novo@seniorinsight.com", "SenhaForte@123", "Nome", "123.456.789-00", "tel", "FAMILIAR", null, null);
        when(usuarioRepository.existsByEmail("novo@seniorinsight.com")).thenReturn(false);
        when(familiarRepository.existsByCpf("123.456.789-00")).thenReturn(true);

        assertThrows(CpfJaCadastradoException.class, () -> authService.registrarNovoUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar CrmJaCadastradoException quando CRM já existir no registro")
    void deveLancarExcecaoQuandoCrmJaCadastradoNoRegistro() {
        RegistroRequestDTO request = new RegistroRequestDTO(
                "novo@seniorinsight.com", "SenhaForte@123", "Nome", null, "tel", "MEDICO", "12345/SP", "Cardio");
        when(usuarioRepository.existsByEmail("novo@seniorinsight.com")).thenReturn(false);
        when(medicoRepository.existsByCrm("12345/SP")).thenReturn(true);

        assertThrows(CrmJaCadastradoException.class, () -> authService.registrarNovoUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    // =========================================================================
    // Testes de completarPerfilSso (HU-002)
    // =========================================================================

    @Test
    @DisplayName("Deve completar perfil SSO como Familiar com sucesso")
    void deveCompletarPerfilSsoFamiliarComSucesso() {
        Usuario usuarioSso = Usuario.builder()
                .id(UUID.randomUUID())
                .email("sso.user@gmail.com")
                .provedorSso(ProvedorSso.GOOGLE)
                .build();

        CompletarPerfilRequestDTO request = new CompletarPerfilRequestDTO(
                "Maria Silva", "111.222.333-44", "(11) 99999-8888", "FAMILIAR", null, null);

        when(usuarioRepository.findById(usuarioSso.getId())).thenReturn(Optional.of(usuarioSso));
        when(familiarRepository.existsByUsuario(usuarioSso)).thenReturn(false);
        when(medicoRepository.existsByUsuario(usuarioSso)).thenReturn(false);
        when(familiarRepository.existsByCpf("111.222.333-44")).thenReturn(false);
        when(jwtService.generateToken(usuarioSso)).thenReturn("jwt.perfil.completo");

        AuthResponseDTO response = authService.completarPerfilSso(usuarioSso, request);

        assertNotNull(response);
        assertEquals("jwt.perfil.completo", response.accessToken());
        assertTrue(response.perfilCompleto());
        verify(familiarRepository).save(any(Familiar.class));
    }

    @Test
    @DisplayName("Deve lançar PerfilJaExistenteException quando usuário SSO já completou perfil")
    void deveLancarExcecaoQuandoPerfilJaExistente() {
        Usuario usuarioSso = Usuario.builder()
                .id(UUID.randomUUID())
                .email("sso.user@gmail.com")
                .provedorSso(ProvedorSso.GOOGLE)
                .build();

        CompletarPerfilRequestDTO request = new CompletarPerfilRequestDTO(
                "Maria Silva", "111.222.333-44", "(11) 99999-8888", "FAMILIAR", null, null);

        when(usuarioRepository.findById(usuarioSso.getId())).thenReturn(Optional.of(usuarioSso));
        when(familiarRepository.existsByUsuario(usuarioSso)).thenReturn(true);

        assertThrows(PerfilJaExistenteException.class, () -> authService.completarPerfilSso(usuarioSso, request));
        verify(familiarRepository, never()).save(any());
    }
}
