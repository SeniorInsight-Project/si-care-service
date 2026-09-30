package com.seniorinsight.backendcore.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.ConviteRede;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.StatusConvite;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.dto.AceiteConviteResponseDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeRequestDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeResponseDTO;
import com.seniorinsight.backendcore.dto.MembroRedeResponseDTO;
import com.seniorinsight.backendcore.exception.ConviteExpiradoException;
import com.seniorinsight.backendcore.exception.ConviteInvalidoException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.exception.VinculoJaExistenteException;
import com.seniorinsight.backendcore.model.StatusUsuario;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.repository.ConviteRedeRepository;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.UsuarioRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/**
 * Serviço de regras de negócio para Gestão e Convites da Rede de Proteção do Idoso (HU16).
 * Orquestra o fluxo de geração de convites temporais (48h), validação de token, vinculação
 * de novo familiar à rede do idoso e listagem dos membros com conversão para DTOs amigáveis.
 */
@Service
public class RedeProtecaoService {

    private static final Logger log = LoggerFactory.getLogger(RedeProtecaoService.class);

    private final ConviteRedeRepository conviteRedeRepository;
    private final VinculoRedeRepository vinculoRedeRepository;
    private final IdosoRepository idosoRepository;
    private final FamiliarRepository familiarRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimuladorEmailService simuladorEmailService;

    public RedeProtecaoService(
            ConviteRedeRepository conviteRedeRepository,
            VinculoRedeRepository vinculoRedeRepository,
            IdosoRepository idosoRepository,
            FamiliarRepository familiarRepository,
            MedicoRepository medicoRepository,
            UsuarioRepository usuarioRepository,
            SimuladorEmailService simuladorEmailService) {
        this.conviteRedeRepository = conviteRedeRepository;
        this.vinculoRedeRepository = vinculoRedeRepository;
        this.idosoRepository = idosoRepository;
        this.familiarRepository = familiarRepository;
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.simuladorEmailService = simuladorEmailService;
    }

    /**
     * Gera um convite temporal (validade de 48h) via token UUID e simula o disparo de e-mail.
     *
     * @param idosoId Identificador único do idoso
     * @param request Dados de entrada contendo e-mail e papel sugerido
     * @param usuarioAutenticado Usuário responsável pela emissão do convite
     * @return DTO com informações do convite gerado e status da simulação
     */
    @Transactional
    public ConviteRedeResponseDTO gerarConvite(UUID idosoId, ConviteRedeRequestDTO request, Usuario usuarioAutenticado) {
        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Os dados do convite não podem ser nulos.");
        }

        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);

        Idoso idoso = idosoRepository.findById(idosoId)
                .orElseThrow(() -> {
                    log.warn("Tentativa de gerar convite para idoso inexistente: {}", idosoId);
                    return new IdosoNaoEncontradoException(idosoId);
                });

        validarPerfilAutorizadoEmissao(usuario, idoso);

        String emailDestinatario = request.email().trim().toLowerCase();

        // Verifica se o destinatário já possui perfil de familiar e já está vinculado ao idoso
        usuarioRepository.findByEmail(emailDestinatario).ifPresent(destinatario -> {
            familiarRepository.findByUsuarioId(destinatario.getId()).ifPresent(familiarDest -> {
                if (vinculoRedeRepository.existsByFamiliarAndIdoso(familiarDest, idoso)) {
                    log.warn("Tentativa de convidar familiar que já possui vínculo com o idoso {}", idosoId);
                    throw new VinculoJaExistenteException("O familiar com e-mail '" + emailDestinatario + "' já possui vínculo ativo com este idoso na Rede de Proteção.");
                }
            });
        });

        // Caso exista um convite pendente anterior para o mesmo idoso e e-mail, cancela-o para emitir o novo
        conviteRedeRepository.findByIdosoIdAndEmailAndStatus(idosoId, emailDestinatario, StatusConvite.PENDENTE)
                .ifPresent(conviteAnterior -> {
                    conviteAnterior.cancelar();
                    conviteRedeRepository.save(conviteAnterior);
                    log.info("Convite pendente anterior {} para o e-mail {} foi cancelado para nova emissão.",
                            conviteAnterior.getId(), emailDestinatario);
                });

        String token = UUID.randomUUID().toString();
        Instant dataExpiracao = Instant.now().plus(48, ChronoUnit.HOURS);
        String grauSugerido = (request.grauParentesco() != null && !request.grauParentesco().isBlank())
                ? request.grauParentesco().trim()
                : "Familiar";

        ConviteRede convite = ConviteRede.builder()
                .idoso(idoso)
                .email(emailDestinatario)
                .token(token)
                .papelAcessoSugerido(request.papelAcesso().trim())
                .grauParentescoSugerido(grauSugerido)
                .status(StatusConvite.PENDENTE)
                .dataExpiracao(dataExpiracao)
                .criadoPor(usuario)
                .build();

        ConviteRede conviteSalvo = conviteRedeRepository.save(convite);
        log.info("Convite {} gerado com sucesso para {} vinculado ao idoso {}. Token: {}",
                conviteSalvo.getId(), emailDestinatario, idosoId, token);

        // Dispara a simulação de envio de e-mail com os detalhes do convite
        simuladorEmailService.simularEnvioConvite(
                conviteSalvo.getEmail(),
                idoso.getNomeCompleto(),
                conviteSalvo.getToken(),
                conviteSalvo.getPapelAcessoSugerido(),
                conviteSalvo.getDataExpiracao(),
                request.mensagemPersonalizada()
        );

        return new ConviteRedeResponseDTO(
                conviteSalvo.getId(),
                idoso.getId(),
                idoso.getNomeCompleto(),
                conviteSalvo.getEmail(),
                conviteSalvo.getToken(),
                conviteSalvo.getPapelAcessoSugerido(),
                conviteSalvo.getGrauParentescoSugerido(),
                conviteSalvo.getStatus(),
                conviteSalvo.getDataExpiracao(),
                conviteSalvo.getCriadoEm(),
                "Convite gerado com sucesso. Simulação de e-mail disparada para " + conviteSalvo.getEmail()
        );
    }

    /**
     * Valida o token temporal e associa o familiar autenticado ao idoso na tabela vinculos_rede.
     *
     * @param token Token temporal recebido
     * @param usuarioAutenticado Usuário familiar logado que está aceitando o convite
     * @return DTO com informações do vínculo criado e status do processo
     */
    @Transactional
    public AceiteConviteResponseDTO aceitarConvite(String token, Usuario usuarioAutenticado) {
        if (token == null || token.trim().isBlank()) {
            throw new IllegalArgumentException("O token de convite é obrigatório.");
        }

        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);

        Familiar familiarLogado = familiarRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> {
                    log.warn("Usuário {} tentou aceitar convite sem possuir perfil completo de Familiar.", usuario.getEmail());
                    return new PerfilNaoAutorizadoException(
                            "Apenas usuários com perfil de Familiar completo podem aceitar convites para a Rede de Proteção. Complete seu perfil antes de prosseguir.");
                });

        ConviteRede convite = conviteRedeRepository.findByTokenWithIdoso(token.trim())
                .orElseGet(() -> conviteRedeRepository.findByToken(token.trim())
                        .orElseThrow(() -> {
                            log.warn("Tentativa de aceite com token inexistente: {}", token);
                            return new ConviteInvalidoException("Convite não encontrado para o token informado.");
                        }));

        if (convite.getStatus() == StatusConvite.ACEITO) {
            throw new ConviteInvalidoException("Este convite já foi aceito anteriormente.");
        }

        if (convite.getStatus() == StatusConvite.CANCELADO) {
            throw new ConviteInvalidoException("Este convite foi cancelado e não é mais válido.");
        }

        if (convite.isExpirado()) {
            convite.expirar();
            conviteRedeRepository.save(convite);
            log.warn("Tentativa de aceite de convite expirado. Token: {}, Expiração: {}", token, convite.getDataExpiracao());
            throw new ConviteExpiradoException("O convite expirou. O prazo de validade temporal de 48 horas foi ultrapassado.");
        }

        Idoso idoso = convite.getIdoso();

        // Verifica se o familiar já possui vínculo ativo com o idoso
        if (vinculoRedeRepository.existsByFamiliarAndIdoso(familiarLogado, idoso)) {
            convite.aceitar();
            conviteRedeRepository.save(convite);
            log.warn("Familiar {} já vinculado ao idoso {}", familiarLogado.getId(), idoso.getId());
            throw new VinculoJaExistenteException("O familiar logado já possui vínculo ativo na Rede de Proteção deste idoso.");
        }

        String grauParentesco = convite.getGrauParentescoSugerido() != null && !convite.getGrauParentescoSugerido().isBlank()
                ? convite.getGrauParentescoSugerido()
                : "Familiar";

        VinculoRede novoVinculo = VinculoRede.builder()
                .familiar(familiarLogado)
                .idoso(idoso)
                .grauParentesco(grauParentesco)
                .papelAcesso(convite.getPapelAcessoSugerido())
                .restricoesAcesso(new HashMap<>())
                .build();

        VinculoRede vinculoSalvo = vinculoRedeRepository.save(novoVinculo);

        convite.aceitar();
        conviteRedeRepository.save(convite);

        log.info("Familiar {} vinculado com sucesso ao idoso {} através do convite {}. Vínculo ID: {}",
                familiarLogado.getId(), idoso.getId(), convite.getId(), vinculoSalvo.getId());

        return new AceiteConviteResponseDTO(
                vinculoSalvo.getId(),
                idoso.getId(),
                idoso.getNomeCompleto(),
                familiarLogado.getId(),
                familiarLogado.getNomeCompleto(),
                vinculoSalvo.getGrauParentesco(),
                vinculoSalvo.getPapelAcesso(),
                vinculoSalvo.getCriadoEm(),
                "Convite aceito com sucesso! Vínculo criado na Rede de Proteção de " + idoso.getNomeCompleto()
        );
    }

    /**
     * Lista todos os membros vinculados à Rede de Proteção do Idoso convertendo para DTO amigável.
     *
     * @param idosoId Identificador único do idoso
     * @param usuarioAutenticado Usuário autenticado consultando a rede
     * @return Lista de membros com detalhes cadastrais, papéis e permissões
     */
    @Transactional(readOnly = true)
    public List<MembroRedeResponseDTO> listarMembrosRede(UUID idosoId, Usuario usuarioAutenticado) {
        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        Usuario usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizadoConsulta(usuario);

        if (!idosoRepository.existsById(idosoId)) {
            log.warn("Tentativa de consultar rede de idoso inexistente: {}", idosoId);
            throw new IdosoNaoEncontradoException(idosoId);
        }

        List<VinculoRede> vinculos = vinculoRedeRepository.findByIdosoIdWithFamiliar(idosoId);

        return vinculos.stream()
                .map(v -> new MembroRedeResponseDTO(
                        v.getId(),
                        v.getFamiliar().getId(),
                        v.getFamiliar().getUsuario().getId(),
                        v.getFamiliar().getNomeCompleto(),
                        v.getFamiliar().getUsuario().getEmail(),
                        v.getFamiliar().getTelefone(),
                        v.getGrauParentesco(),
                        v.getPapelAcesso(),
                        v.getRestricoesAcesso(),
                        v.getCriadoEm()
                ))
                .toList();
    }

    /**
     * Valida permissão para emissão de convite.
     * Deve ser um perfil cadastrado de Familiar ou Médico. Se for Familiar e já houver rede formada,
     * valida se o familiar remetente possui vínculo ativo com o idoso.
     */
    private void validarPerfilAutorizadoEmissao(Usuario usuario, Idoso idoso) {
        if (usuario == null || usuario.getId() == null) {
            throw new PerfilNaoAutorizadoException("Usuário não autenticado para emitir convites.");
        }

        boolean ehFamiliar = familiarRepository.existsByUsuarioId(usuario.getId());
        boolean ehMedico = medicoRepository.existsByUsuarioId(usuario.getId());

        if (!ehFamiliar && !ehMedico) {
            log.warn("Acesso negado: usuário {} tentou emitir convite sem perfil de Familiar ou Médico.", usuario.getEmail());
            throw new PerfilNaoAutorizadoException("Apenas perfis autorizados (Familiar ou Médico) podem gerenciar convites da Rede de Proteção.");
        }

        if (ehFamiliar) {
            Familiar familiar = familiarRepository.findByUsuarioId(usuario.getId()).orElse(null);
            if (familiar != null) {
                boolean redeExiste = !vinculoRedeRepository.findByIdosoId(idoso.getId()).isEmpty();
                if (redeExiste && !vinculoRedeRepository.existsByFamiliarAndIdoso(familiar, idoso)) {
                    log.warn("Familiar {} não possui vínculo com o idoso {} para gerenciar seus convites.", familiar.getId(), idoso.getId());
                    throw new PerfilNaoAutorizadoException("Você não possui vínculo com este idoso para gerenciar convites de sua Rede de Proteção.");
                }
            }
        }
    }

    /**
     * Valida permissão para consulta dos membros da rede.
     */
    private void validarPerfilAutorizadoConsulta(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new PerfilNaoAutorizadoException("Usuário não autenticado para consultar a Rede de Proteção.");
        }

        boolean ehFamiliar = familiarRepository.existsByUsuarioId(usuario.getId());
        boolean ehMedico = medicoRepository.existsByUsuarioId(usuario.getId());

        if (!ehFamiliar && !ehMedico) {
            log.warn("Acesso negado: usuário {} tentou consultar a rede sem perfil de Familiar ou Médico.", usuario.getEmail());
            throw new PerfilNaoAutorizadoException("Apenas perfis autorizados (Familiar ou Médico) podem visualizar a Rede de Proteção.");
        }
    }

    /**
     * Resolve o Usuário persistido no banco a partir do token ou do SecurityContextHolder.
     */
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
