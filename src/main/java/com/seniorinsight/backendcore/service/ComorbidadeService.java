package com.seniorinsight.backendcore.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.dto.ComorbidadeRequestDTO;
import com.seniorinsight.backendcore.dto.ComorbidadesResponseDTO;
import com.seniorinsight.backendcore.exception.ComorbidadeJaCadastradaException;
import com.seniorinsight.backendcore.exception.ComorbidadeNaoEncontradaException;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.security.UsuarioAutenticadoArgumentResolver;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;

/**
 * Serviço de regras de negócio para a gestão de comorbidades do Idoso (HU15).
 * Manipula a coluna JSONB nativa da entidade Idoso garantindo validações de negócio,
 * controle de acesso baseado nos perfis autorizados (Familiar ou Médico) e consistência de dados.
 */
@Service
public class ComorbidadeService {

    private static final Logger log = LoggerFactory.getLogger(ComorbidadeService.class);

    private final IdosoRepository idosoRepository;
    private final FamiliarRepository familiarRepository;
    private final MedicoRepository medicoRepository;

    public ComorbidadeService(
            IdosoRepository idosoRepository,
            FamiliarRepository familiarRepository,
            MedicoRepository medicoRepository) {
        this.idosoRepository = idosoRepository;
        this.familiarRepository = familiarRepository;
        this.medicoRepository = medicoRepository;
    }

    /**
     * Adiciona uma ou mais comorbidades à lista JSONB existente do perfil do idoso.
     *
     * @param idosoId Identificador único do idoso
     * @param request DTO com a lista de comorbidades a serem inseridas
     * @param usuarioAutenticado Usuário que está executando a requisição
     * @return ComorbidadesResponseDTO contendo o idosoId e a lista atualizada de comorbidades
     */
    @Transactional
    public ComorbidadesResponseDTO adicionarComorbidades(UUID idosoId, ComorbidadeRequestDTO request, UsuarioAutenticado usuarioAutenticado) {
        UsuarioAutenticado usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        if (request == null || request.comorbidades() == null || request.comorbidades().isEmpty()) {
            throw new IllegalArgumentException("A lista de comorbidades não pode estar vazia.");
        }

        Idoso idoso = idosoRepository.findById(idosoId)
                .orElseThrow(() -> {
                    log.warn("Tentativa de adicionar comorbidades para idoso inexistente: {}", idosoId);
                    return new IdosoNaoEncontradoException(idosoId);
                });

        List<String> listaAtual = idoso.getComorbidades();
        if (listaAtual == null) {
            listaAtual = new ArrayList<>();
        } else {
            listaAtual = new ArrayList<>(listaAtual);
        }

        List<String> novasComorbidades = new ArrayList<>();

        for (String item : request.comorbidades()) {
            if (item == null || item.trim().isBlank()) {
                continue;
            }

            String normalizada = item.trim();

            boolean jaExisteNaBase = listaAtual.stream()
                    .anyMatch(c -> c.trim().equalsIgnoreCase(normalizada));

            boolean jaExisteNoLote = novasComorbidades.stream()
                    .anyMatch(c -> c.trim().equalsIgnoreCase(normalizada));

            if (!jaExisteNaBase && !jaExisteNoLote) {
                novasComorbidades.add(normalizada);
            }
        }

        if (novasComorbidades.isEmpty()) {
            log.warn("Nenhuma comorbidade nova adicionada ao idoso {}. Todas já estavam cadastradas.", idosoId);
            throw new ComorbidadeJaCadastradaException("Todas as comorbidades informadas já estão cadastradas para este idoso.");
        }

        listaAtual.addAll(novasComorbidades);
        idoso.setComorbidades(listaAtual);

        Idoso idosoSalvo = idosoRepository.save(idoso);
        log.info("Comorbidades {} adicionadas com sucesso ao idoso {} pelo usuário {}",
                novasComorbidades, idosoId, usuario.email());

        return new ComorbidadesResponseDTO(idosoSalvo.getId(), idosoSalvo.getComorbidades(), idosoSalvo.getAtualizadoEm());
    }

    /**
     * Remove uma comorbidade específica da lista JSONB do idoso.
     *
     * @param idosoId Identificador único do idoso
     * @param nomeComorbidade Nome da comorbidade a ser removida
     * @param usuarioAutenticado Usuário que está executando a requisição
     * @return ComorbidadesResponseDTO contendo o idosoId e a lista atualizada de comorbidades
     */
    @Transactional
    public ComorbidadesResponseDTO removerComorbidade(UUID idosoId, String nomeComorbidade, UsuarioAutenticado usuarioAutenticado) {
        UsuarioAutenticado usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        if (nomeComorbidade == null || nomeComorbidade.trim().isBlank()) {
            throw new IllegalArgumentException("O nome da comorbidade para remoção não pode ser nulo ou vazio.");
        }

        Idoso idoso = idosoRepository.findById(idosoId)
                .orElseThrow(() -> {
                    log.warn("Tentativa de remover comorbidade para idoso inexistente: {}", idosoId);
                    return new IdosoNaoEncontradoException(idosoId);
                });

        List<String> listaAtual = idoso.getComorbidades();
        if (listaAtual == null || listaAtual.isEmpty()) {
            throw new ComorbidadeNaoEncontradaException(nomeComorbidade);
        }

        listaAtual = new ArrayList<>(listaAtual);
        String nomeAlvo = nomeComorbidade.trim();

        boolean removido = listaAtual.removeIf(c -> c.trim().equalsIgnoreCase(nomeAlvo));

        if (!removido) {
            log.warn("Comorbidade '{}' não localizada para o idoso {}", nomeComorbidade, idosoId);
            throw new ComorbidadeNaoEncontradaException(nomeComorbidade);
        }

        idoso.setComorbidades(listaAtual);
        Idoso idosoSalvo = idosoRepository.save(idoso);

        log.info("Comorbidade '{}' removida com sucesso do idoso {} pelo usuário {}",
                nomeComorbidade, idosoId, usuario.email());

        return new ComorbidadesResponseDTO(idosoSalvo.getId(), idosoSalvo.getComorbidades(), idosoSalvo.getAtualizadoEm());
    }

    /**
     * Consulta a lista atual de comorbidades cadastradas para o idoso.
     *
     * @param idosoId Identificador único do idoso
     * @param usuarioAutenticado Usuário que está executando a requisição
     * @return ComorbidadesResponseDTO contendo o idosoId e a lista de comorbidades
     */
    @Transactional(readOnly = true)
    public ComorbidadesResponseDTO listarComorbidades(UUID idosoId, UsuarioAutenticado usuarioAutenticado) {
        UsuarioAutenticado usuario = resolverUsuarioAutenticado(usuarioAutenticado);
        validarPerfilAutorizado(usuario);

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        Idoso idoso = idosoRepository.findById(idosoId)
                .orElseThrow(() -> new IdosoNaoEncontradoException(idosoId));

        List<String> comorbidades = idoso.getComorbidades() != null
                ? Collections.unmodifiableList(idoso.getComorbidades())
                : Collections.emptyList();

        return new ComorbidadesResponseDTO(idoso.getId(), comorbidades, idoso.getAtualizadoEm());
    }

    /**
     * Valida se o usuário autenticado possui perfil de Familiar ou Médico.
     * Caso não possua, bloqueia o acesso lançando PerfilNaoAutorizadoException (HTTP 403).
     */
    private void validarPerfilAutorizado(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.keycloakSub() == null) {
            throw new PerfilNaoAutorizadoException("Apenas perfis autorizados (Familiar ou Médico) podem alterar as comorbidades do idoso.");
        }

        boolean ehFamiliar = familiarRepository.existsByKeycloakSub(usuario.keycloakSub());
        boolean ehMedico = medicoRepository.existsByKeycloakSub(usuario.keycloakSub());

        if (!ehFamiliar && !ehMedico) {
            log.warn("Acesso negado: usuário {} (ID: {}) tentou manipular comorbidades sem perfil de Familiar ou Médico.",
                    usuario.email(), usuario.keycloakSub());
            throw new PerfilNaoAutorizadoException("Perfil não encontrado para este usuário.");
        }
    }

    private UsuarioAutenticado resolverUsuarioAutenticado(UsuarioAutenticado usuarioParam) {
        UsuarioAutenticado usuario = usuarioParam != null ? usuarioParam : UsuarioAutenticadoArgumentResolver.atual();
        if (usuario == null || usuario.keycloakSub() == null || usuario.keycloakSub().isBlank()) {
            throw new PerfilNaoAutorizadoException("Usuário não autenticado. Forneça um token JWT válido.");
        }
        return usuario;
    }
}
