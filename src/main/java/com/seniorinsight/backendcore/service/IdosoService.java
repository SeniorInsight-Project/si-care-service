package com.seniorinsight.backendcore.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.AuditoriaIdoso;
import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.dto.IdosoRequestDTO;
import com.seniorinsight.backendcore.dto.IdosoResponseDTO;
import com.seniorinsight.backendcore.exception.CpfJaCadastradoException;
import com.seniorinsight.backendcore.exception.FamiliarNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.repository.AuditoriaIdosoRepository;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;

/**
 * Cadastro e edição do idoso (HU-008 e HU-009).
 *
 * <p>Quem cadastra vira o Familiar Administrador do idoso, na mesma transação (RN-012). A edição é só do
 * Administrador e grava o antes e o depois de cada campo alterado.
 */
@Service
public class IdosoService {

    private static final Logger log = LoggerFactory.getLogger(IdosoService.class);

    /** Idade mínima esperada de um idoso; abaixo dela a resposta traz um aviso, sem bloquear. */
    static final int IDADE_MINIMA_ESPERADA = 49;

    static final String AVISO_IDADE = "O idoso tem menos de " + IDADE_MINIMA_ESPERADA
            + " anos. Confira a data de nascimento informada.";

    private final IdosoRepository idosoRepository;
    private final FamiliarRepository familiarRepository;
    private final VinculoRedeRepository vinculoRedeRepository;
    private final AuditoriaIdosoRepository auditoriaRepository;
    private final AcessoIdosoService acessoIdosoService;
    private final Clock clock;

    public IdosoService(IdosoRepository idosoRepository,
                        FamiliarRepository familiarRepository,
                        VinculoRedeRepository vinculoRedeRepository,
                        AuditoriaIdosoRepository auditoriaRepository,
                        AcessoIdosoService acessoIdosoService,
                        Clock clock) {
        this.idosoRepository = idosoRepository;
        this.familiarRepository = familiarRepository;
        this.vinculoRedeRepository = vinculoRedeRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.acessoIdosoService = acessoIdosoService;
        this.clock = clock;
    }

    /** Cria o idoso e o vínculo de Administrador do autor, na mesma transação. */
    @Transactional
    public IdosoResponseDTO cadastrar(UsuarioAutenticado usuario, IdosoRequestDTO request) {
        Familiar autor = familiarRepository.findByKeycloakSub(usuario.keycloakSub())
                .orElseThrow(() -> new PerfilNaoAutorizadoException(
                        "Apenas um Familiar pode cadastrar um idoso."));

        String cpf = normalizar(request.cpf());
        if (cpf != null && idosoRepository.existsByCpf(cpf)) {
            throw new CpfJaCadastradoException();
        }

        Idoso idoso = new Idoso();
        aplicar(idoso, request);
        try {
            idoso = idosoRepository.saveAndFlush(idoso);
        } catch (DataIntegrityViolationException e) {
            throw new CpfJaCadastradoException();
        }

        vinculoRedeRepository.save(VinculoRede.builder()
                .familiar(autor)
                .idoso(idoso)
                .grauParentesco(PapelAcesso.PARENTESCO_NAO_INFORMADO)
                .papelAcesso(PapelAcesso.ADMINISTRADOR)
                .build());

        log.info("Idoso {} cadastrado; Administrador: familiar {}", idoso.getId(), autor.getId());
        return resposta(idoso);
    }

    /** Idosos da rede do usuário (como familiar) e os que ele acompanha (como médico). */
    @Transactional(readOnly = true)
    public List<IdosoResponseDTO> listar(UsuarioAutenticado usuario) {
        Map<UUID, Idoso> idosos = new LinkedHashMap<>();
        idosoRepository.findByFamiliarKeycloakSub(usuario.keycloakSub()).forEach(i -> idosos.put(i.getId(), i));
        idosoRepository.findByMedicoKeycloakSub(usuario.keycloakSub()).forEach(i -> idosos.put(i.getId(), i));
        return idosos.values().stream()
                .sorted((a, b) -> a.getNomeCompleto().compareToIgnoreCase(b.getNomeCompleto()))
                .map(this::resposta)
                .toList();
    }

    /**
     * Idosos de um familiar específico. O próprio familiar vê todos os seus; outro Familiar vê só os idosos que ele
     * administra. Quem não é nem um nem outro recebe 403. {@code familiarId} é o id do Familiar no care-service ou
     * o {@code sub} do Keycloak dele.
     */
    @Transactional(readOnly = true)
    public List<IdosoResponseDTO> listarDoFamiliar(UsuarioAutenticado usuario, UUID familiarId) {
        Familiar alvo = familiarRepository.findById(familiarId)
                .or(() -> familiarRepository.findByKeycloakSub(familiarId.toString()))
                .orElseThrow(() -> new FamiliarNaoEncontradoException(familiarId));

        List<Idoso> idosos;
        if (alvo.getKeycloakSub().equals(usuario.keycloakSub())) {
            idosos = idosoRepository.findByFamiliarKeycloakSub(alvo.getKeycloakSub());
        } else {
            idosos = idosoRepository.findDoFamiliarAdministradosPor(alvo.getKeycloakSub(), usuario.keycloakSub());
            if (idosos.isEmpty()) {
                throw new PerfilNaoAutorizadoException(
                        "Só o próprio familiar ou um Familiar Administrador do idoso pode consultar esta lista.");
            }
        }
        return idosos.stream()
                .sorted((a, b) -> a.getNomeCompleto().compareToIgnoreCase(b.getNomeCompleto()))
                .map(this::resposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public IdosoResponseDTO buscar(UsuarioAutenticado usuario, UUID idosoId) {
        return resposta(acessoIdosoService.exigirVinculo(usuario, idosoId));
    }

    /** Atualiza o cadastro (só o Administrador) e grava a auditoria dos campos que mudaram. */
    @Transactional
    public IdosoResponseDTO atualizar(UsuarioAutenticado usuario, UUID idosoId, IdosoRequestDTO request) {
        Idoso idoso = acessoIdosoService.exigirAdministrador(usuario, idosoId);

        String cpf = normalizar(request.cpf());
        if (cpf != null && idosoRepository.existsByCpfAndIdNot(cpf, idosoId)) {
            throw new CpfJaCadastradoException();
        }

        Map<String, Object> anteriores = new LinkedHashMap<>();
        Map<String, Object> novos = new LinkedHashMap<>();
        Map<String, Object> antes = campos(idoso);
        aplicar(idoso, request);
        Map<String, Object> depois = campos(idoso);
        depois.forEach((campo, valor) -> {
            if (!Objects.equals(antes.get(campo), valor)) {
                anteriores.put(campo, antes.get(campo));
                novos.put(campo, valor);
            }
        });

        if (!novos.isEmpty()) {
            try {
                idosoRepository.saveAndFlush(idoso);
            } catch (DataIntegrityViolationException e) {
                throw new CpfJaCadastradoException();
            }
            auditoriaRepository.save(new AuditoriaIdoso(
                    idosoId, usuario.keycloakSub(), Instant.now(clock), anteriores, novos));
            log.info("Idoso {} atualizado por {}: campos {}", idosoId, usuario.keycloakSub(), novos.keySet());
        }
        return resposta(idoso);
    }

    private IdosoResponseDTO resposta(Idoso idoso) {
        return IdosoResponseDTO.de(idoso, avisos(idoso.getDataNascimento()));
    }

    private List<String> avisos(LocalDate dataNascimento) {
        List<String> avisos = new ArrayList<>();
        if (Period.between(dataNascimento, LocalDate.now(clock)).getYears() < IDADE_MINIMA_ESPERADA) {
            avisos.add(AVISO_IDADE);
        }
        return avisos;
    }

    private void aplicar(Idoso idoso, IdosoRequestDTO r) {
        idoso.setNomeCompleto(r.nomeCompleto().trim());
        idoso.setDataNascimento(r.dataNascimento());
        idoso.setGenero(normalizar(r.genero()));
        idoso.setCpf(normalizar(r.cpf()));
        idoso.setFotoPerfilUrl(normalizar(r.fotoPerfilUrl()));
        idoso.setTipoSanguineo(normalizar(r.tipoSanguineo()));
        idoso.setAlergias(normalizar(r.alergias()));
        idoso.setLogradouro(normalizar(r.logradouro()));
        idoso.setNumero(normalizar(r.numero()));
        idoso.setBairro(normalizar(r.bairro()));
        idoso.setCidade(normalizar(r.cidade()));
        idoso.setEstado(normalizar(r.estado()));
        idoso.setCep(normalizar(r.cep()));
        idoso.setPlanoSaude(normalizar(r.planoSaude()));
        idoso.setObservacoesSocorros(normalizar(r.observacoesSocorros()));
    }

    /** Campos editáveis do cadastro, para comparar antes e depois. Data vira texto ISO para sair como JSON. */
    private Map<String, Object> campos(Idoso i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nomeCompleto", i.getNomeCompleto());
        m.put("dataNascimento", i.getDataNascimento() != null ? i.getDataNascimento().toString() : null);
        m.put("genero", i.getGenero());
        m.put("cpf", i.getCpf());
        m.put("fotoPerfilUrl", i.getFotoPerfilUrl());
        m.put("tipoSanguineo", i.getTipoSanguineo());
        m.put("alergias", i.getAlergias());
        m.put("logradouro", i.getLogradouro());
        m.put("numero", i.getNumero());
        m.put("bairro", i.getBairro());
        m.put("cidade", i.getCidade());
        m.put("estado", i.getEstado());
        m.put("cep", i.getCep());
        m.put("planoSaude", i.getPlanoSaude());
        m.put("observacoesSocorros", i.getObservacoesSocorros());
        return m;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String s = valor.trim();
        return s.isEmpty() ? null : s;
    }
}
