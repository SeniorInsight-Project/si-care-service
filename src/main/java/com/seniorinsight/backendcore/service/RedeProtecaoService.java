package com.seniorinsight.backendcore.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.ConviteRede;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.Medico;
import com.seniorinsight.backendcore.domain.PapelAcesso;
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
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.exception.VinculoJaExistenteException;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.security.UsuarioAutenticadoArgumentResolver;
import com.seniorinsight.backendcore.repository.ConviteRedeRepository;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/**
 * Serviço de regras de negócio para Gestão e Convites da Rede de Proteção do Idoso (HU16).
 * Orquestra o fluxo de geração de convites temporais (48h), validação de token, vinculação
 * de novo familiar à rede do idoso e listagem dos membros com conversão para DTOs amigáveis.
 */
@Service
public class RedeProtecaoService {

    private static final Logger log = LoggerFactory.getLogger(RedeProtecaoService.class);

    private static final String NOME_CONVIDANTE_PADRAO = "Um familiar do idoso";

    private final ConviteRedeRepository conviteRedeRepository;
    private final VinculoRedeRepository vinculoRedeRepository;
    private final FamiliarRepository familiarRepository;
    private final MedicoRepository medicoRepository;
    private final AcessoIdosoService acessoIdosoService;
    private final SimuladorEmailService simuladorEmailService;

    public RedeProtecaoService(
            ConviteRedeRepository conviteRedeRepository,
            VinculoRedeRepository vinculoRedeRepository,
            FamiliarRepository familiarRepository,
            MedicoRepository medicoRepository,
            AcessoIdosoService acessoIdosoService,
            SimuladorEmailService simuladorEmailService) {
        this.conviteRedeRepository = conviteRedeRepository;
        this.vinculoRedeRepository = vinculoRedeRepository;
        this.familiarRepository = familiarRepository;
        this.medicoRepository = medicoRepository;
        this.acessoIdosoService = acessoIdosoService;
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
    public ConviteRedeResponseDTO gerarConvite(UUID idosoId, ConviteRedeRequestDTO request, UsuarioAutenticado usuarioAutenticado) {
        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Os dados do convite não podem ser nulos.");
        }

        UsuarioAutenticado usuario = resolverUsuarioAutenticado(usuarioAutenticado);

        // Só quem já está na rede do idoso (Familiar com vínculo) ou o médico que o acompanha emite convite
        Idoso idoso = acessoIdosoService.exigirVinculo(usuario, idosoId);

        // RN-012: o Administrador é único por idoso e nasce com o cadastro; convite não cria outro
        if (PapelAcesso.ADMINISTRADOR.equalsIgnoreCase(request.papelAcesso().trim())) {
            throw new IllegalArgumentException(
                    "O papel Administrador não pode ser concedido por convite: há um único Administrador por idoso.");
        }

        String emailDestinatario = request.email().trim().toLowerCase();

        // Verifica se o destinatário já possui perfil de familiar e já está vinculado ao idoso
        familiarRepository.findByEmailIgnoreCase(emailDestinatario).ifPresent(familiarDest -> {
            if (vinculoRedeRepository.existsByFamiliarAndIdoso(familiarDest, idoso)) {
                log.warn("Tentativa de convidar familiar que já possui vínculo com o idoso {}", idosoId);
                throw new VinculoJaExistenteException("O familiar com e-mail '" + emailDestinatario + "' já possui vínculo ativo com este idoso na Rede de Proteção.");
            }
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
                .criadoPorSub(usuario.keycloakSub())
                .build();

        ConviteRede conviteSalvo = conviteRedeRepository.save(convite);
        log.info("Convite {} gerado com sucesso para o idoso {}.", conviteSalvo.getId(), idosoId);

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
    public AceiteConviteResponseDTO aceitarConvite(String token, UsuarioAutenticado usuarioAutenticado) {
        if (token == null || token.trim().isBlank()) {
            throw new IllegalArgumentException("O token de convite é obrigatório.");
        }

        UsuarioAutenticado usuario = resolverUsuarioAutenticado(usuarioAutenticado);

        Familiar familiarLogado = familiarRepository.findByKeycloakSub(usuario.keycloakSub())
                .orElseThrow(() -> {
                    log.warn("Usuário {} tentou aceitar convite sem possuir perfil completo de Familiar.", usuario.keycloakSub());
                    return new PerfilNaoAutorizadoException(
                            "Perfil não encontrado para este usuário. Apenas Familiares podem aceitar convites para a Rede de Proteção.");
                });

        ConviteRede convite = buscarConvitePorToken(token)
                .orElseThrow(() -> {
                    log.warn("Tentativa de aceite com token de convite inexistente.");
                    return new ConviteInvalidoException("Convite não encontrado para o token informado.");
                });

        if (convite.getStatus() == StatusConvite.ACEITO) {
            throw new ConviteInvalidoException("Este convite já foi aceito anteriormente.");
        }

        if (convite.getStatus() == StatusConvite.CANCELADO) {
            throw new ConviteInvalidoException("Este convite foi cancelado e não é mais válido.");
        }

        if (convite.isExpirado()) {
            convite.expirar();
            conviteRedeRepository.save(convite);
            log.warn("Tentativa de aceite do convite {} expirado em {}.", convite.getId(), convite.getDataExpiracao());
            throw new ConviteExpiradoException("O convite expirou. O prazo de validade temporal de 48 horas foi ultrapassado.");
        }

        // Convites emitidos antes desta regra com papel Administrador também não viram vínculo
        if (PapelAcesso.ADMINISTRADOR.equalsIgnoreCase(convite.getPapelAcessoSugerido().trim())) {
            throw new ConviteInvalidoException(
                    "Este convite concede o papel Administrador, que não pode ser concedido por convite.");
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
     * Consulta o convite pelo token, sem alterá-lo, para a tela de aceite mostrar quem convidou e para qual idoso.
     * Exige só usuário autenticado: quem consulta ainda não faz parte da rede do idoso.
     *
     * @param token Token temporal recebido no e-mail do convite
     * @param usuarioAutenticado Usuário autenticado que está consultando
     * @return DTO com os dados públicos do convite pendente
     */
    @Transactional(readOnly = true)
    public ConvitePublicoDTO consultarConvite(String token, UsuarioAutenticado usuarioAutenticado) {
        if (token == null || token.trim().isBlank()) {
            throw new IllegalArgumentException("O token de convite é obrigatório.");
        }

        resolverUsuarioAutenticado(usuarioAutenticado);

        ConviteRede convite = buscarConvitePorToken(token)
                .orElseThrow(() -> {
                    log.warn("Consulta com token de convite inexistente.");
                    return new ConviteNaoEncontradoException("Convite não encontrado");
                });

        if (convite.getStatus() == StatusConvite.ACEITO) {
            throw new ConviteJaAceitoException("Convite já foi aceito");
        }

        if (convite.getStatus() == StatusConvite.CANCELADO) {
            throw new ConviteExpiradoException("Convite cancelado");
        }

        if (convite.isExpirado()) {
            throw new ConviteExpiradoException("Convite expirado");
        }

        return new ConvitePublicoDTO(
                nomeDoConvidante(convite.getCriadoPorSub()),
                primeiroNome(convite.getIdoso().getNomeCompleto()),
                convite.getPapelAcessoSugerido(),
                convite.getGrauParentescoSugerido(),
                convite.getDataExpiracao(),
                convite.getStatus()
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
    public List<MembroRedeResponseDTO> listarMembrosRede(UUID idosoId, UsuarioAutenticado usuarioAutenticado) {
        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        UsuarioAutenticado usuario = resolverUsuarioAutenticado(usuarioAutenticado);

        // Rede, médico do idoso ou o próprio idoso pelo aparelho pareado (403 sem vínculo, 404 se não existe)
        acessoIdosoService.exigirLeitura(usuario, idosoId);

        List<VinculoRede> vinculos = vinculoRedeRepository.findByIdosoIdWithFamiliar(idosoId);

        return vinculos.stream()
                .map(v -> new MembroRedeResponseDTO(
                        v.getId(),
                        v.getFamiliar().getId(),
                        v.getFamiliar().getKeycloakSub(),
                        v.getFamiliar().getNomeCompleto(),
                        v.getFamiliar().getEmail(),
                        v.getFamiliar().getTelefone(),
                        v.getGrauParentesco(),
                        v.getPapelAcesso(),
                        v.getRestricoesAcesso(),
                        v.getCriadoEm()
                ))
                .toList();
    }

    /**
     * Busca o convite pelo token, com o idoso carregado quando possível. Usada no aceite e na consulta.
     */
    private Optional<ConviteRede> buscarConvitePorToken(String token) {
        String tokenLimpo = token.trim();
        return conviteRedeRepository.findByTokenWithIdoso(tokenLimpo)
                .or(() -> conviteRedeRepository.findByToken(tokenLimpo));
    }

    private String nomeDoConvidante(String criadoPorSub) {
        if (criadoPorSub == null || criadoPorSub.isBlank()) {
            return NOME_CONVIDANTE_PADRAO;
        }
        return familiarRepository.findByKeycloakSub(criadoPorSub)
                .map(Familiar::getNomeCompleto)
                .or(() -> medicoRepository.findByKeycloakSub(criadoPorSub).map(Medico::getNomeCompleto))
                .orElse(NOME_CONVIDANTE_PADRAO);
    }

    private static String primeiroNome(String nomeCompleto) {
        return nomeCompleto.trim().split("\\s+")[0];
    }

    private UsuarioAutenticado resolverUsuarioAutenticado(UsuarioAutenticado usuarioParam) {
        UsuarioAutenticado usuario = usuarioParam != null ? usuarioParam : UsuarioAutenticadoArgumentResolver.atual();
        if (usuario == null || usuario.keycloakSub() == null || usuario.keycloakSub().isBlank()) {
            throw new PerfilNaoAutorizadoException("Usuário não autenticado. Forneça um token JWT válido.");
        }
        return usuario;
    }
}
