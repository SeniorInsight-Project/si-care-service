package com.seniorinsight.backendcore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.seniorinsight.backendcore.security.Mascara;
import com.seniorinsight.backendcore.security.RateLimiterService;

/**
 * Serviço de regras de negócio de autenticação tradicional, registro de usuários
 * e preenchimento de perfil (HU-002 / DRS).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final FamiliarRepository familiarRepository;
    private final MedicoRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiterService rateLimiterService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            FamiliarRepository familiarRepository,
            MedicoRepository medicoRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RateLimiterService rateLimiterService) {
        this.usuarioRepository = usuarioRepository;
        this.familiarRepository = familiarRepository;
        this.medicoRepository = medicoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Autenticação tradicional por e-mail e senha com validação de hash BCrypt.
     * Retorna o token JWT e o indicador se o perfil está completo.
     *
     * @param request Dados de login (email e senha)
     * @return AuthResponseDTO contendo o access token JWT e perfilCompleto
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO autenticarTradicional(LoginRequestDTO request) {
        String email = request.email().trim().toLowerCase();

        // Rate limiting defensivo contra ataques de força bruta
        if (!rateLimiterService.tryAcquire("login:" + email, 5, 60)) {
            log.warn("Limite de tentativas de login excedido para o e-mail: {}", Mascara.email(email));
            throw new RateLimitExceededException("Muitas tentativas de login consecutivas. Por favor, aguarde um minuto.");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Tentativa de login para e-mail não cadastrado: {}", Mascara.email(email));
                    return new CredenciaisInvalidasException("E-mail ou senha inválidos.");
                });

        if (usuario.getSenhaHash() == null || !passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            log.warn("Falha de autenticação (senha incorreta) para o usuário: {}", Mascara.email(email));
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos.");
        }

        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            log.warn("Tentativa de login de usuário inativo ou bloqueado: {}", Mascara.email(email));
            throw new CredenciaisInvalidasException("Conta inativa ou bloqueada. Entre em contato com o suporte.");
        }

        boolean perfilCompleto = familiarRepository.existsByUsuario(usuario) || medicoRepository.existsByUsuario(usuario);
        String jwt = jwtService.generateToken(usuario);

        log.info("Usuário autenticado com sucesso via login tradicional: {}", Mascara.email(email));
        return new AuthResponseDTO(jwt, perfilCompleto);
    }

    /**
     * Registro de novo usuário com senha hasheada via BCrypt e criação do perfil inicial (Familiar ou Médico).
     *
     * @param request Dados completos de registro
     * @return AuthResponseDTO contendo o access token JWT e perfilCompleto = true
     */
    @Transactional
    public AuthResponseDTO registrarNovoUsuario(RegistroRequestDTO request) {
        String email = request.email().trim().toLowerCase();
        String tipoPerfil = request.tipoPerfil().trim().toUpperCase();
        String cpf = request.cpf() != null ? request.cpf().trim() : null;

        if (!"FAMILIAR".equals(tipoPerfil) && !"MEDICO".equals(tipoPerfil)) {
            throw new IllegalArgumentException("Tipo de perfil inválido: " + request.tipoPerfil() + ". Valores aceitos: FAMILIAR ou MEDICO.");
        }

        if (usuarioRepository.existsByEmail(email)) {
            log.warn("Tentativa de cadastro com e-mail já existente: {}", Mascara.email(email));
            throw new EmailJaCadastradoException("O e-mail '" + email + "' já está cadastrado.");
        }

        if ("FAMILIAR".equals(tipoPerfil)) {
            if (cpf == null || cpf.isBlank()) {
                throw new IllegalArgumentException("O CPF é obrigatório para cadastro de Familiar.");
            }
            if (familiarRepository.existsByCpf(cpf)) {
                log.warn("Tentativa de cadastro de Familiar com CPF duplicado.");
                throw new CpfJaCadastradoException("O CPF informado já está cadastrado.");
            }
        } else {
            // MEDICO
            if (request.crm() == null || request.crm().isBlank()) {
                throw new IllegalArgumentException("O CRM é obrigatório para cadastro de Médico.");
            }
            if (request.especialidade() == null || request.especialidade().isBlank()) {
                throw new IllegalArgumentException("A especialidade é obrigatória para cadastro de Médico.");
            }
            String crm = request.crm().trim();
            if (medicoRepository.existsByCrm(crm)) {
                log.warn("Tentativa de cadastro de Médico com CRM duplicado.");
                throw new CrmJaCadastradoException("O CRM informado já está cadastrado.");
            }
        }

        // 1. Criação do Usuário
        Usuario novoUsuario = Usuario.builder()
                .email(email)
                .senhaHash(passwordEncoder.encode(request.senha()))
                .provedorSso(ProvedorSso.LOCAL)
                .status(StatusUsuario.ATIVO)
                .mfaHabilitado(false)
                .build();

        novoUsuario = usuarioRepository.save(novoUsuario);

        // 2. Criação do perfil correspondente
        if ("FAMILIAR".equals(tipoPerfil)) {
            Familiar familiar = Familiar.builder()
                    .usuario(novoUsuario)
                    .nomeCompleto(request.nomeCompleto().trim())
                    .cpf(cpf)
                    .telefone(request.telefone() != null ? request.telefone().trim() : null)
                    .build();
            familiarRepository.save(familiar);
            log.info("Perfil de Familiar cadastrado com sucesso para o usuário: {}", Mascara.email(email));
        } else {
            Medico medico = Medico.builder()
                    .usuario(novoUsuario)
                    .nomeCompleto(request.nomeCompleto().trim())
                    .crm(request.crm().trim())
                    .especialidade(request.especialidade().trim())
                    .telefoneConsultorio(request.telefone() != null ? request.telefone().trim() : null)
                    .build();
            medicoRepository.save(medico);
            log.info("Perfil de Médico cadastrado com sucesso para o usuário: {}", Mascara.email(email));
        }

        String jwt = jwtService.generateToken(novoUsuario);
        return new AuthResponseDTO(jwt, true);
    }

    /**
     * Completa o perfil de um usuário autenticado via SSO (Google).
     * Lê o usuário logado via SecurityContext e cria o perfil de Familiar ou Médico amarrado a este usuário.
     *
     * @param usuarioAutenticado Usuário injetado ou lido do SecurityContext
     * @param request Dados do perfil a preencher
     * @return AuthResponseDTO contendo o access token JWT atualizado e perfilCompleto = true
     */
    @Transactional
    public AuthResponseDTO completarPerfilSso(Usuario usuarioAutenticado, CompletarPerfilRequestDTO request) {
        Usuario usuario = resolveUsuarioAutenticado(usuarioAutenticado);

        if (familiarRepository.existsByUsuario(usuario) || medicoRepository.existsByUsuario(usuario)) {
            log.warn("Usuário {} tentou completar perfil que já existe", usuario.getId());
            throw new PerfilJaExistenteException("O perfil deste usuário já foi preenchido anteriormente.");
        }

        String tipoPerfil = request.tipoPerfil().trim().toUpperCase();
        String cpf = request.cpf() != null ? request.cpf().trim() : null;

        if (!"FAMILIAR".equals(tipoPerfil) && !"MEDICO".equals(tipoPerfil)) {
            throw new IllegalArgumentException("Tipo de perfil inválido: " + request.tipoPerfil() + ". Valores aceitos: FAMILIAR ou MEDICO.");
        }

        if ("FAMILIAR".equals(tipoPerfil)) {
            if (cpf == null || cpf.isBlank()) {
                throw new IllegalArgumentException("O CPF é obrigatório para perfil de Familiar.");
            }
            if (familiarRepository.existsByCpf(cpf)) {
                log.warn("Tentativa de completar perfil Familiar com CPF duplicado.");
                throw new CpfJaCadastradoException("O CPF informado já está cadastrado.");
            }

            Familiar familiar = Familiar.builder()
                    .usuario(usuario)
                    .nomeCompleto(request.nomeCompleto().trim())
                    .cpf(cpf)
                    .telefone(request.telefone() != null ? request.telefone().trim() : null)
                    .build();
            familiarRepository.save(familiar);
            log.info("Perfil de Familiar completado via SSO para o usuário: {}", usuario.getId());
        } else {
            // MEDICO
            if (request.crm() == null || request.crm().isBlank()) {
                throw new IllegalArgumentException("O CRM é obrigatório para perfil de Médico.");
            }
            if (request.especialidade() == null || request.especialidade().isBlank()) {
                throw new IllegalArgumentException("A especialidade é obrigatória para perfil de Médico.");
            }
            String crm = request.crm().trim();
            if (medicoRepository.existsByCrm(crm)) {
                log.warn("Tentativa de completar perfil Médico com CRM duplicado.");
                throw new CrmJaCadastradoException("O CRM informado já está cadastrado.");
            }

            Medico medico = Medico.builder()
                    .usuario(usuario)
                    .nomeCompleto(request.nomeCompleto().trim())
                    .crm(crm)
                    .especialidade(request.especialidade().trim())
                    .telefoneConsultorio(request.telefone() != null ? request.telefone().trim() : null)
                    .build();
            medicoRepository.save(medico);
            log.info("Perfil de Médico completado via SSO para o usuário: {}", usuario.getId());
        }

        String jwt = jwtService.generateToken(usuario);
        return new AuthResponseDTO(jwt, true);
    }

    private Usuario resolveUsuarioAutenticado(Usuario usuarioParam) {
        if (usuarioParam != null && usuarioParam.getId() != null) {
            return usuarioRepository.findById(usuarioParam.getId())
                    .orElse(usuarioParam);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof Usuario u) {
                return usuarioRepository.findById(u.getId()).orElse(u);
            }
            String email = authentication.getName();
            if (email != null && !email.isBlank()) {
                return usuarioRepository.findByEmail(email)
                        .orElseThrow(() -> new CredenciaisInvalidasException("Usuário autenticado não encontrado no banco de dados."));
            }
        }

        throw new CredenciaisInvalidasException("Usuário não autenticado. Forneça um token JWT válido.");
    }
}
