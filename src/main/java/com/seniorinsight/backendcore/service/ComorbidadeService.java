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
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.security.UsuarioAutenticadoArgumentResolver;
import com.seniorinsight.backendcore.repository.IdosoRepository;

/**
 * Serviço de regras de negócio para a gestão de comorbidades do Idoso (HU15).
 * Manipula a coluna JSONB nativa da entidade Idoso garantindo validações de negócio,
 * controle de acesso pelo vínculo com o idoso (rede de proteção ou médico, via AcessoIdosoService) e consistência de dados.
 */
@Service
public class ComorbidadeService {

    private static final Logger log = LoggerFactory.getLogger(ComorbidadeService.class);

    private final IdosoRepository idosoRepository;
    private final AcessoIdosoService acessoIdosoService;

    public ComorbidadeService(IdosoRepository idosoRepository, AcessoIdosoService acessoIdosoService) {
        this.idosoRepository = idosoRepository;
        this.acessoIdosoService = acessoIdosoService;
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

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        // Familiar com vínculo ou médico do idoso (403 sem vínculo, 404 se o idoso não existe)
        Idoso idoso = acessoIdosoService.exigirVinculo(usuario, idosoId);

        if (request == null || request.comorbidades() == null || request.comorbidades().isEmpty()) {
            throw new IllegalArgumentException("A lista de comorbidades não pode estar vazia.");
        }

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

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        // Familiar com vínculo ou médico do idoso (403 sem vínculo, 404 se o idoso não existe)
        Idoso idoso = acessoIdosoService.exigirVinculo(usuario, idosoId);

        if (nomeComorbidade == null || nomeComorbidade.trim().isBlank()) {
            throw new IllegalArgumentException("O nome da comorbidade para remoção não pode ser nulo ou vazio.");
        }

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

        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        // Rede, médico do idoso ou o próprio idoso pelo aparelho pareado (403 sem vínculo, 404 se não existe)
        Idoso idoso = acessoIdosoService.exigirLeitura(usuario, idosoId);

        List<String> comorbidades = idoso.getComorbidades() != null
                ? Collections.unmodifiableList(idoso.getComorbidades())
                : Collections.emptyList();

        return new ComorbidadesResponseDTO(idoso.getId(), comorbidades, idoso.getAtualizadoEm());
    }

    private UsuarioAutenticado resolverUsuarioAutenticado(UsuarioAutenticado usuarioParam) {
        UsuarioAutenticado usuario = usuarioParam != null ? usuarioParam : UsuarioAutenticadoArgumentResolver.atual();
        if (usuario == null || usuario.keycloakSub() == null || usuario.keycloakSub().isBlank()) {
            throw new PerfilNaoAutorizadoException("Usuário não autenticado. Forneça um token JWT válido.");
        }
        return usuario;
    }
}
